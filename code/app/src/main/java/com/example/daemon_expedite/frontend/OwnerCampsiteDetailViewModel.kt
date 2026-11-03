package com.example.daemon_expedite.frontend

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

/** Outcome of [OwnerCampsiteDetailViewModel.delete]. */
enum class DeleteResult {
    /** The campsite is gone. */
    DELETED,

    /** Not deleted, because one of the owner's lotteries for this campsite is still running. */
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
                val lottery = loaded.activeLotteryAmong(owner.ownerLotteries)
                // OPEN only drives the "Lottery · Active" badge; owners don't see the lottery card.
                loaded.toDetails(lottery, if (lottery != null) LotteryStatus.OPEN else LotteryStatus.NONE)
            }
            if (fresh is UiState.Content || _details.value !is UiState.Content) _details.value = fresh
        }
    }

    /** Loads again after an error, for example from a "try again" link. */
    fun retry() {
        owner?.let { load(it) }
    }

    /**
     * Deletes the campsite, unless one of the owner's lotteries for it is still running.
     *
     * [Database.deleteCampsite] removes the campsite and takes it off the owner in one write.
     * Its photos are deleted afterwards; if that fails it only leaves unused files behind, so
     * the delete still counts as done.
     */
    fun delete(onResult: (DeleteResult) -> Unit) {
        val owner = owner
        val campsite = campsite
        if (owner == null || campsite == null || _isDeleting.value) return onResult(DeleteResult.FAILED)
        _isDeleting.value = true
        viewModelScope.launch {
            val result = if (campsite.activeLotteryAmong(owner.ownerLotteries) != null) {
                DeleteResult.LOTTERY_RUNNING
            } else {
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
                if (ok) DeleteResult.DELETED else DeleteResult.FAILED
            }
            _isDeleting.value = false
            onResult(result)
        }
    }
}
