package com.example.daemon_expedite.frontend

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Image
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.backend.NotFoundException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** What the app currently knows about the signed-in user. */
sealed interface SessionState {
    /** Signing in and loading the user. */
    data object Loading : SessionState

    /** Start-up failed (for example no internet). Call [SessionViewModel.retry]. */
    data object Failed : SessionState

    /**
     * The user is loaded.
     *
     * @property profileImageUrl Download URL for the user's photo, ready to give to an image
     * loader. [User.profileImage] only holds a Firebase Storage *path*, which can't be shown.
     * @property isAdmin True if the user has a document in the "admins" collection with the same
     * id. Checked once at start-up; adds the Admin option to the profile role toggle.
     * @property revision [User] is a plain mutable class, so Compose cannot see when its fields
     * change. This number goes up on every change made through [SessionViewModel], which makes
     * the state compare as different and redraws every screen reading it.
     */
    data class Ready(
        val user: User,
        val profileImageUrl: String? = null,
        val isAdmin: Boolean = false,
        val revision: Int = 0,
    ) : SessionState
}

/**
 * Holds the signed-in [User] for the whole app.
 *
 * Created once per activity (see [AppNavigation]) and shared by every screen, so screens never
 * fetch the user themselves. It survives screen rotation, and starts loading as soon as it is
 * created, which also covers Android restoring the app straight onto a later screen.
 *
 * Every change to the user goes through a function here. Each one changes the [User] object,
 * saves it with [Database.updateUser], then bumps [SessionState.Ready.revision] so screens redraw.
 * Results are reported through an `onResult` callback so the screen can navigate or show an error.
 */
class SessionViewModel : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** The loaded user, or null while loading or after a failure. */
    val user: User? get() = (_state.value as? SessionState.Ready)?.user

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Tries signing in and loading the user again after [SessionState.Failed]. */
    fun retry() {
        if (_state.value is SessionState.Failed) load()
    }

    // region Profile changes

    /** Saves new profile details. [onResult] gets true if the save reached the database. */
    fun updateProfile(
        name: String,
        email: String,
        phoneNumber: String,
        onResult: (Boolean) -> Unit = {},
    ) = saveUser(onResult) { it.updateProfile(name, email, phoneNumber) }

    /** Turns notifications on or off and saves the setting. */
    fun setNotificationsEnabled(enabled: Boolean, onResult: (Boolean) -> Unit = {}) =
        saveUser(onResult) { it.enableOrDisableNotifications(enabled) }

    /**
     * Removes the profile photo, then deletes the old image's record and Storage file.
     *
     * The user is saved without the photo *before* the image is deleted, so the user document
     * never points to an image that no longer exists. If that save fails, the photo is put back
     * and nothing is deleted.
     */
    fun removeProfilePhoto(onResult: (Boolean) -> Unit = {}) {
        val user = user ?: return onResult(false)
        val oldImage = user.profileImage ?: return onResult(true) // nothing to remove
        viewModelScope.launch {
            val ok = runCatchingAll("Removing profile photo failed") {
                user.profileImage = null
                Database.updateUser(user)
            }
            if (ok) {
                deleteImageQuietly(oldImage)
            } else {
                user.profileImage = oldImage // keep the app in step with the database
            }
            refreshPhotoUrlAndRedraw()
            onResult(ok)
        }
    }

    /**
     * Uploads [uri] (a photo picked from the gallery) and makes it the profile photo.
     *
     * [Database.uploadImage] stores the file and its `images/<id>` record; the user document
     * then only stores that image id. The previous photo is left in storage.
     */
    fun uploadProfilePhoto(uri: Uri, onResult: (Boolean) -> Unit = {}) {
        val user = user ?: return onResult(false)
        val oldImage = user.profileImage
        viewModelScope.launch {
            val ok = runCatchingAll("Uploading profile photo failed") {
                val image = Database.uploadProfileImage(user.id, uri)
                user.updateProfileImage(image) // also saves the user with Database.updateUser
            }
            if (ok) {
                // The user now points to the new photo, so the old one can go.
                oldImage?.let { deleteImageQuietly(it) }
                refreshPhotoUrlAndRedraw()
            }
            onResult(ok)
        }
    }

    /**
     * Deletes an image that nothing points to any more. A failure only leaves an unused file
     * behind, so it is logged rather than reported to the user.
     */
    private suspend fun deleteImageQuietly(image: Image) {
        runCatchingAll("Couldn't delete old image ${image.id.value} (${image.path})") {
            Database.deleteImage(image.id)
        }
    }

    /**
     * Deletes the whole account and starts again with a fresh, empty user.
     *
     * The user is the account and the owner record is a profile attached to it, so
     * [Database.deleteUser] also deletes the owner side (campsites, lotteries, images) if any.
     *
     * The anonymous Firebase sign-in stays the same, so the next load finds no profile and
     * creates a new one, just like a first launch.
     */
    fun deleteProfile(onResult: (Boolean) -> Unit = {}) {
        val user = user ?: return onResult(false)
        viewModelScope.launch {
            val ok = runCatchingAll("Deleting profile failed") {
                Database.deleteUser(user)
            }
            if (ok) {
                user.deleteProfile() // clears the in-memory lists
                loadJob?.cancel()
                load()
            }
            onResult(ok)
        }
    }

    // endregion

    /**
     * Call after changing the [User] object somewhere else (and saving it yourself), so screens
     * redraw with the new values.
     */
    fun userChanged() {
        _state.update { if (it is SessionState.Ready) it.copy(revision = it.revision + 1) else it }
    }

    /**
     * Applies [change] to the user, saves the whole user with [Database.updateUser], and redraws.
     *
     * The change is applied before saving so the screen updates straight away. If the save
     * fails the local change is kept, and [onResult] gets false so the screen can say so.
     *
     * Don't use this for photo changes: screens show the photo from
     * [SessionState.Ready.profileImageUrl], which must be looked up again (see
     * [uploadProfilePhoto] and [removeProfilePhoto]).
     */
    private fun saveUser(onResult: (Boolean) -> Unit, change: suspend (User) -> Unit) {
        val user = user ?: return onResult(false)
        viewModelScope.launch {
            val ok = runCatchingAll("Saving user failed") {
                change(user)
                Database.updateUser(user)
            }
            userChanged()
            onResult(ok)
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        _state.value = SessionState.Loading
        loadJob = viewModelScope.launch {
            _state.value = try {
                Database.init()
                val user = loadOrCreateUser()
                SessionState.Ready(
                    user = user,
                    profileImageUrl = photoUrlFor(user),
                    isAdmin = checkIsAdmin(user),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Start-up failed", e)
                SessionState.Failed
            }
        }
    }

    /**
     * Returns true if [user] has an entry in the "admins" collection.
     *
     * Admins are added by hand in the Firebase console, using the same id as the user's document,
     * so there is nothing to create here; we only check. If the check fails (no connection, or the
     * database function isn't written yet) the user is treated as a normal user.
     *
     */
    private suspend fun checkIsAdmin(user: User): Boolean =
        try {
            Database.isAdmin(user.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "Couldn't check admin status, treating as non-admin", e)
            false
        }

    /** Looks up the download URL for the current photo and redraws. */
    private suspend fun refreshPhotoUrlAndRedraw() {
        val user = user ?: return
        val url = photoUrlFor(user)
        _state.update {
            if (it is SessionState.Ready) it.copy(profileImageUrl = url, revision = it.revision + 1) else it
        }
    }

    /** Turns the user's Storage path into a download URL, or null if there is no photo. */
    private suspend fun photoUrlFor(user: User): String? {
        val path = user.profileImage?.path
        if (path == null) {
            Log.i(TAG, "User ${user.id.value} has no profile image record")
            return null
        }
        return try {
            Database.getImage(path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't get profile photo URL", e)
            null
        }
    }

    /**
     * Returns the user linked to this device's Firebase sign-in, creating one on first launch.
     *
     * Only [NotFoundException] means "first launch". Every other error (no connection, a field
     * that can't be read) is passed on, so a problem loading an existing user never creates a
     * second, empty account in its place.
     */
    private suspend fun loadOrCreateUser(): User =
        try {
            Database.fetchOwnUser()
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotFoundException) {
            Log.i(TAG, "No existing user found, creating a new one", e)
            User(notificationsEnabled = true).also { Database.addUser(it) }
        }

    private companion object {
        const val TAG = "SessionViewModel"
    }
}

/**
 * Runs [block] and returns true if it finished, false if it threw.
 *
 * Catches [Throwable] so database functions that are still `TODO()` (which throw
 * `NotImplementedError`) are reported as a failure instead of crashing the app.
 */
internal suspend fun runCatchingAll(logMessage: String, block: suspend () -> Unit): Boolean =
    try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Log.e("SessionViewModel", logMessage, e)
        false
    }
