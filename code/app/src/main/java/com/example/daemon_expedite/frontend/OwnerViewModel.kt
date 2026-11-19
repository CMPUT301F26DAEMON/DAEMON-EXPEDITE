package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.NotFoundException
import com.google.firebase.FirebaseException
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Data for the campsite owner screens: the owner record and their registered campsites.
 *
 * Owners are stored separately from users (the "owners" collection). The first time someone
 * switches to the owner view, an owner record is created from their user details, using the
 * same id so it is easy to match the two up later.
 */
class OwnerViewModel : ViewModel() {

    private val _owner = MutableStateFlow<UiState<Owner>>(UiState.Loading)
    val owner: StateFlow<UiState<Owner>> = _owner.asStateFlow()

    private val _campsites = MutableStateFlow<UiState<List<CampsiteSummary>>>(UiState.Loading)
    val campsites: StateFlow<UiState<List<CampsiteSummary>>> = _campsites.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var loadedFor: User? = null

    /**
     * Loads the owner for [user] (creating one if needed) and then their campsites.
     * Safe to call on every visit: it only loads once per user.
     */
    fun load(user: User, force: Boolean = false) {
        if (!force && loadedFor === user) return
        loadedFor = user
        viewModelScope.launch {
            _owner.value = UiState.Loading
            _owner.value = loadAsUiState("Couldn't load your owner profile") { loadOrCreateOwner(user) }
            loadCampsites()
        }
    }

    /**
     * Deletes only the owner side of the account: the owner record, their campsites, and
     * everything attached to those campsites. The entrant profile is kept.
     *
     * Afterwards this ViewModel forgets the owner, so the next switch to the owner view finds no
     * owner record and creates a new, empty one (see [loadOrCreateOwner]).
     *
     * [Database.deleteOwner] deletes everything in one batched write, so a lost connection can't
     * leave half of it deleted.
     */
    fun deleteOwnerProfile(onResult: (Boolean) -> Unit = {}) {
        val owner = (_owner.value as? UiState.Content)?.data ?: return onResult(false)
        viewModelScope.launch {
            val ok = runCatchingAll("Deleting owner profile failed") {
                Database.deleteOwner(owner)
            }
            if (ok) {
                loadedFor = null
                _owner.value = UiState.Loading
                _campsites.value = UiState.Loading
                _query.value = ""
            }
            onResult(ok)
        }
    }

    /** Reloads the campsite list, for example after a new campsite is registered. */
    fun refreshCampsites() {
        viewModelScope.launch { loadCampsites() }
    }

    /** Filters the campsite list shown on the owner home page. */
    fun onQueryChange(text: String) {
        _query.value = text
    }

    private suspend fun loadCampsites() {
        val owner = (_owner.value as? UiState.Content)?.data
        if (owner == null) {
            _campsites.value = UiState.Error("Couldn't load your campsites")
            return
        }
        _campsites.value = loadAsUiState("Couldn't load your campsites") {
            owner.ownerCampsites.mapNotNull { id ->
                try {
                    Database.fetchCampsite(id).toSummary()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: FirebaseException) {
                    throw e // connection problems fail the whole list, so the user can retry
                } catch (e: Exception) {
                    // One missing or broken campsite shouldn't hide the others.
                    Log.w(TAG, "Skipping campsite ${id.value}", e)
                    null
                }
            }
        }
    }

    /**
     * Returns this device's owner record, or creates one from [user]'s details.
     *
     * Only a "not found" result creates a new owner. Any other error (no connection, a field
     * that can't be read) is passed on. Creating an owner in that case would overwrite the
     * existing record and lose the owner's campsites, because the new one has the same id.
     */
    private suspend fun loadOrCreateOwner(user: User): Owner =
        try {
            Database.getOwner()
        } catch (e: NotFoundException) {
            Log.i(TAG, "No owner record yet, creating one")
            Owner(
                id = user.id,
                name = user.name,
                email = user.email,
                phoneNumber = user.phoneNumber,
                notificationsEnabled = user.notificationsEnabled,
                profileImage = user.profileImage,
            ).also { Database.addOwner(it) }
        }

    private companion object {
        const val TAG = "OwnerViewModel"
    }
}
