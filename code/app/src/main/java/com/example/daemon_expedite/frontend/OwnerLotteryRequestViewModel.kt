package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.InviteRejectedException
import com.example.daemon_expedite.backend.Lottery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of approving or declining one campsite on the request page. */
sealed interface InviteAnswerResult {
    /** Other campsites of this owner still wait for an answer; the page has reloaded. */
    data object MorePending : InviteAnswerResult

    /** The last invite was approved. [lottery] now lists this owner as a co-owner. */
    data class Approved(val lottery: Lottery) : InviteAnswerResult

    /** The last invite was declined. */
    data object Declined : InviteAnswerResult

    /** Not saved. [message] can be shown to the owner. */
    data class Failed(val message: String) : InviteAnswerResult
}

/**
 * Data for the page where an owner answers another owner's invite to add their campsites to
 * a lottery (the "Lottery Request · Approval Pending" frame).
 *
 * The lottery id comes from the route (see [Routes.OWNER_LOTTERY_REQUEST]). Call [load] with
 * the signed-in owner's id.
 */
class OwnerLotteryRequestViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val lotteryId: Id? = savedState.get<String>(Routes.LOTTERY_ID_ARG).toIdOrNull()
    private var ownerId: Id? = null

    private val _details = MutableStateFlow<UiState<LotteryDetails>>(UiState.Loading)
    val details: StateFlow<UiState<LotteryDetails>> = _details.asStateFlow()

    /** The campsite being approved or declined, or null. Both buttons ignore taps meanwhile. */
    private val _answering = MutableStateFlow<Id?>(null)
    val answering: StateFlow<Id?> = _answering.asStateFlow()

    fun load(ownerId: Id? = this.ownerId) {
        this.ownerId = ownerId
        val id = lotteryId ?: run {
            _details.value = UiState.Error("Couldn't find this request")
            return
        }
        viewModelScope.launch {
            if (_details.value !is UiState.Content) _details.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load this request") { fetchOwnerLotteryDetails(id, ownerId) }
            if (fresh is UiState.Content || _details.value !is UiState.Content) _details.value = fresh
        }
    }

    /** Approves (or, with [approve] false, declines) the invite for [campsiteId]. */
    fun answer(campsiteId: Id, approve: Boolean, onResult: (InviteAnswerResult) -> Unit) {
        val id = lotteryId ?: return
        val owner = ownerId ?: return onResult(InviteAnswerResult.Failed("Your owner profile is still loading"))
        if (_answering.value != null) return
        _answering.value = campsiteId
        viewModelScope.launch {
            val result = try {
                if (approve) {
                    Database.acceptCampsiteInvite(id, campsiteId, owner)
                } else {
                    Database.declineCampsiteInvite(id, campsiteId, owner)
                }
                val lottery = Database.fetchLottery(id)
                when {
                    lottery.invites.any { it.campsiteOwner == owner && it.campsite in lottery.pendingCampsites } -> {
                        load()
                        InviteAnswerResult.MorePending
                    }
                    approve -> InviteAnswerResult.Approved(lottery)
                    else -> InviteAnswerResult.Declined
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Firestore may wrap exceptions thrown inside a transaction.
                val rejected = generateSequence<Throwable>(e) { it.cause }.filterIsInstance<InviteRejectedException>().firstOrNull()
                if (rejected == null) Log.e("OwnerLotteryRequest", "Answering invite for $campsiteId failed", e)
                InviteAnswerResult.Failed(rejected?.message ?: "Couldn't save your answer. Check your connection and try again.")
            }
            _answering.value = null
            onResult(result)
        }
    }
}
