package com.example.daemon_expedite.frontend

import com.example.daemon_expedite.backend.BookingRules
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.ui_components.LotteryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of [OwnerCampsiteDetailViewModel.delete]. */
enum class DeleteResult {
    /** The campsite is gone. */
    DELETED,

    /** Not deleted, because the campsite is in a lottery that isn't finished or cancelled. */
    LOTTERY_RUNNING,

    /** Not deleted, usually no connection. */
    FAILED,
}

/**
 * Data for the owner's view of one of their campsites, plus deleting it.
 *
 * The campsite id comes from the navigation route (see [Routes.OWNER_CAMPSITE_DETAIL]).
 * Call [load] with the owner every time the screen is shown, so changes saved on the Edit
 * campsite screen appear when the owner comes back.
 */
class OwnerCampsiteDetailViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val campsiteId: Id? = savedState.campsiteIdArg()

    private val _details = MutableStateFlow<UiState<CampsiteDetails>>(UiState.Loading)
    val details: StateFlow<UiState<CampsiteDetails>> = _details.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private var owner: Owner? = null
    private var campsite: Campsite? = null

    /**
     * Loads (or reloads) the campsite. While reloading, what is already on screen stays there,
     * and stays put if the reload fails.
     */
    fun load(owner: Owner) {
        this.owner = owner
        val id = campsiteId ?: run {
            _details.value = UiState.Error(CAMPSITE_NOT_FOUND)
            return
        }
        viewModelScope.launch {
            if (_details.value !is UiState.Content) _details.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load this campsite") {
                val loaded = Database.fetchCampsite(id)
                campsite = loaded
                // Only drives the badge and the booked dates, so a failed lookup shouldn't fail the page.
                val lotteries = quietly("Couldn't look up lotteries for ${loaded.id}") {
                    Database.fetchLotteriesForCampsite(loaded.id)
                }.orEmpty()
                val lottery = lotteries.firstOrNull { !it.phase().isPast }
                // OPEN only drives the "Lottery · Active" badge; owners don't see the lottery card.
                loaded.toDetails(lottery, if (lottery != null) LotteryStatus.OPEN else LotteryStatus.NONE)
                    .copy(bookedNights = BookingRules.bookedNightRanges(lotteries, loaded.id))
            }
            if (fresh is UiState.Content || _details.value !is UiState.Content) _details.value = fresh
        }
    }

    /** Loads again after an error, for example from a "try again" link. */
    fun retry() {
        owner?.let { load(it) }
    }

    /**
     * Deletes the campsite, unless it is in a lottery that isn't finished or cancelled.
     *
     * See [deleteCampsiteAndPhotos] for what is deleted.
     */
    fun delete(onResult: (DeleteResult) -> Unit) {
        val owner = owner
        val campsite = campsite
        if (owner == null || campsite == null || _isDeleting.value) return onResult(DeleteResult.FAILED)
        _isDeleting.value = true
        viewModelScope.launch {
            val running = try {
                campsite.currentLottery() != null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null // couldn't check: don't delete
            }
            val result = if (running == null) {
                DeleteResult.FAILED
            } else if (running) {
                DeleteResult.LOTTERY_RUNNING
            } else if (deleteCampsiteAndPhotos(campsite)) {
                DeleteResult.DELETED
            } else {
                DeleteResult.FAILED
            }
            _isDeleting.value = false
            onResult(result)
        }
    }
}

/**
 * Deletes [campsite] and its photos. Returns true if the campsite is gone. Shared by the owner's
 * and the admin's campsite pages.
 *
 * [Database.deleteCampsite] removes the campsite and takes it off the owner in one write.
 * Its photos are deleted afterwards; if that fails it only leaves unused files behind, so
 * the delete still counts as done.
 */
internal suspend fun deleteCampsiteAndPhotos(campsite: Campsite): Boolean {
    val ok = runCatchingAll("Deleting campsite ${campsite.id} failed") {
        Database.deleteCampsite(campsite)
    }
    if (ok) {
        campsite.images.forEach { image ->
            runCatchingAll("Couldn't delete image ${image.id} of deleted campsite") {
                Database.deleteImage(image.id)
            }
        }
    }
    return ok
}
