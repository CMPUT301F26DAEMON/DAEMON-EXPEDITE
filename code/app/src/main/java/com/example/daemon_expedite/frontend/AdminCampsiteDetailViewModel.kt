package com.example.daemon_expedite.frontend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.ui_components.LotteryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data for the admin's view of any campsite, plus deleting the campsite or its owner.
 *
 * The campsite id comes from the navigation route (see [Routes.ADMIN_CAMPSITE_DETAIL]).
 * Call [load] every time the screen is shown.
 *
 * Unlike the owner's delete, an admin's delete isn't blocked by a running lottery: removing a
 * campsite is a moderation action. Lottery and booking screens already skip campsites that no
 * longer exist.
 */
class AdminCampsiteDetailViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val campsiteId: Id? = savedState.campsiteIdArg()

    private val _details = MutableStateFlow<UiState<CampsiteDetails>>(UiState.Loading)
    val details: StateFlow<UiState<CampsiteDetails>> = _details.asStateFlow()

    /** True while the campsite or its owner is being deleted. */
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private var campsite: Campsite? = null

    /** Who owns the campsite on screen, or null until it has loaded. Owner ids are user ids. */
    val ownerId: Id? get() = campsite?.ownerId

    /**
     * Loads (or reloads) the campsite. While reloading, what is already on screen stays there,
     * and stays put if the reload fails.
     */
    fun load() {
        val id = campsiteId ?: run {
            _details.value = UiState.Error(CAMPSITE_NOT_FOUND)
            return
        }
        viewModelScope.launch {
            if (_details.value !is UiState.Content) _details.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load this campsite") {
                val loaded = Database.fetchCampsite(id)
                campsite = loaded
                // Only drives the badge and the available dates, so a failed lookup shouldn't fail the page.
                val lottery = quietly("Couldn't look up lotteries for ${loaded.id}") { loaded.currentLottery() }
                val status = when {
                    lottery == null -> LotteryStatus.NONE
                    lottery.phase() == LotteryPhase.UPCOMING -> LotteryStatus.UPCOMING
                    else -> LotteryStatus.OPEN // shown as "Lottery · Active"
                }
                loaded.toDetails(lottery, status)
            }
            if (fresh is UiState.Content || _details.value !is UiState.Content) _details.value = fresh
        }
    }

    /** Deletes the campsite and its photos. [onResult] gets true if the campsite is gone. */
    fun deleteCampsite(onResult: (Boolean) -> Unit) {
        val campsite = campsite
        if (campsite == null || _isDeleting.value) return onResult(false)
        _isDeleting.value = true
        viewModelScope.launch {
            val ok = deleteCampsiteAndPhotos(campsite)
            _isDeleting.value = false
            onResult(ok)
        }
    }

    /**
     * Deletes the campsite's owner. [Database.deleteOwner] also removes everything on their owner
     * side (this campsite included) in one write; their entrant account is kept.
     * [onResult] gets true if the owner is gone.
     */
    fun deleteOwner(onResult: (Boolean) -> Unit) {
        val ownerId = ownerId
        if (ownerId == null || _isDeleting.value) return onResult(false)
        _isDeleting.value = true
        viewModelScope.launch {
            val ok = runCatchingAll("Deleting owner $ownerId failed") { Database.deleteOwner(ownerId) }
            _isDeleting.value = false
            onResult(ok)
        }
    }
}
