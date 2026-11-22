package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * Data for the admin's Profiles tab ([AdminProfilesScreen]): every user, plus deleting one.
 *
 * Call [load] every time the tab is shown, so new and deleted users are up to date.
 * Deleting goes through [Database.deleteUser], which also removes the user's owner side
 * (campsites, lotteries) and profile photo.
 */
class ProfileDeleteScreenViewModel : ViewModel() {

    private val _profiles = MutableStateFlow<UiState<List<AdminProfile>>>(UiState.Loading)
    val profiles: StateFlow<UiState<List<AdminProfile>>> = _profiles.asStateFlow()

    private val _deletingId = MutableStateFlow<String?>(null)

    /** The profile being deleted right now, or null. Only one delete runs at a time. */
    val deletingId: StateFlow<String?> = _deletingId.asStateFlow()

    /**
     * Loads (or reloads) every user. What is on screen stays there while it reloads.
     *
     * @param excludeId A user to leave out of the list: the signed-in admin, so they can't
     *   delete the account they are using.
     */
    fun load(excludeId: Id? = null) {
        viewModelScope.launch {
            if (_profiles.value !is UiState.Content) _profiles.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load profiles") {
                fetch().filter { it.id != excludeId?.value?.toString() }.sortedBy { it.name.lowercase() }
            }
            if (fresh is UiState.Content || _profiles.value !is UiState.Content) _profiles.value = fresh
        }
    }

    /**
     * Deletes the user with [id]. On success the profile is removed from the list straight
     * away, without a reload. Ignored while another delete is running.
     *
     * @param id The user's id, as on [AdminProfile.id].
     * @param onResult Called with true if the user is gone.
     */
    fun deleteProfile(id: String, onResult: (Boolean) -> Unit) {
        val userId = id.toIdOrNull()
        if (userId == null || _deletingId.value != null) return onResult(false)
        _deletingId.value = id
        viewModelScope.launch {
            val ok = runCatchingAll("Deleting user $id failed") { Database.deleteUser(userId) }
            if (ok) {
                (_profiles.value as? UiState.Content)?.let { current ->
                    _profiles.value = UiState.Content(current.data.filter { it.id != id })
                }
            }
            _deletingId.value = null
            onResult(ok)
        }
    }

    private suspend fun fetch(): List<AdminProfile> = coroutineScope {
        Database.fetchAllUsers().mapNotNull { user ->
            // Skip a malformed record instead of failing the whole list.
            val id = user["id"].toIdOrNull() ?: return@mapNotNull null
            async {
                val name = user["name"]?.takeIf { it.isNotBlank() } ?: "Unnamed user"
                val phone = user["phoneNumber"].orEmpty()
                val email = user["email"].orEmpty()
                val imageUrl = user["profileImage"]?.takeIf { it.isNotBlank() }?.let { imageId ->
                    quietly("Couldn't load profile picture for user $id") { profileImageUrl(imageId) }
                }
                AdminProfile(id.value.toString(), name, phone, email, imageUrl)
            }
        }.awaitAll()
    }

    /** Turns a user's profile image record id into a download URL an image loader can show. */
    private suspend fun profileImageUrl(imageId: String): String {
        val image = Database.fetchImage(Id(Uuid.parse(imageId)))
        return Database.getImage(image.path)
    }
}
