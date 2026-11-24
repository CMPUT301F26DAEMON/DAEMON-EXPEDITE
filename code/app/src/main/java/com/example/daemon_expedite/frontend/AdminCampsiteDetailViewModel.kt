package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.LotteryNotCancellableException
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.ui_components.LotteryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Data for the admin's view of any campsite, plus deleting the campsite or its owner.
 *
 * The campsite id comes from the navigation route (see [Routes.ADMIN_CAMPSITE_DETAIL]).
 * Call [load] every time the screen is shown.
 *
 * Unlike the owner's delete, an admin's delete isn't blocked by a running lottery: removing a
 * campsite is a moderation action, so the lottery is cancelled first (see [deleteCampsite]).
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

    /**
     * Cancels the campsite's running lottery (if any), then deletes the campsite and its photos.
     * [onResult] gets true if the campsite is gone.
     *
     * The lottery is cancelled first so a failure leaves everything as it was; deleting first
     * could leave a live lottery pointing at a campsite that no longer exists.
     */
    fun deleteCampsite(onResult: (Boolean) -> Unit) {
        val campsite = campsite
        if (campsite == null || _isDeleting.value) return onResult(false)
        _isDeleting.value = true
        viewModelScope.launch {
            val ok = cancelCurrentLottery(campsite) && deleteCampsiteAndPhotos(campsite)
            _isDeleting.value = false
            onResult(ok)
        }
    }

    /**
     * Cancels [campsite]'s lottery that isn't past, if it has one (see [Database.cancelLottery]).
     * Returns false if the lookup or the cancel failed, so the campsite isn't deleted.
     * A lottery whose stays have already started can't be cancelled; that isn't a failure.
     */
    private suspend fun cancelCurrentLottery(campsite: Campsite): Boolean =
        try {
            campsite.currentLottery()?.let { Database.cancelLottery(it.id) }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Firestore wraps exceptions thrown inside a transaction, so look at the cause too.
            if (e is LotteryNotCancellableException || e.cause is LotteryNotCancellableException) {
                true
            } else {
                Log.e("AdminCampsiteDetail", "Cancelling the lottery of campsite ${campsite.id} failed", e)
                false
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
