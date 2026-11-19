package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.ui_components.PillVariant
import com.google.firebase.FirebaseException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The entrant's lotteries split for the In Progress and Results tabs. On Results, each card's
 * pill says how the entrant did.
 */
data class EntrantLotteries(
    val inProgress: List<LotterySummary>,
    val results: List<LotterySummary>,
)

/**
 * Data for the entrant's My Lotteries screen.
 *
 * The lotteries come from the ids on the signed-in [User] ([User.lotteryIds]). Call [load] every
 * time the screen is shown, so a lottery joined from a campsite page appears when the entrant
 * comes back. What is on screen stays there while it reloads.
 */
class EntrantLotteriesViewModel : ViewModel() {

    private val _lotteries = MutableStateFlow<UiState<EntrantLotteries>>(UiState.Loading)
    val lotteries: StateFlow<UiState<EntrantLotteries>> = _lotteries.asStateFlow()

    private var user: User? = null

    fun load(user: User) {
        this.user = user
        viewModelScope.launch {
            if (_lotteries.value !is UiState.Content) _lotteries.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load your lotteries") { fetch(user) }
            if (fresh is UiState.Content || _lotteries.value !is UiState.Content) _lotteries.value = fresh
        }
    }

    fun retry() {
        user?.let(::load)
    }

    /** Takes [lotteryId] off the In Progress tab straight away, after the entrant left it. */
    fun removeFromInProgress(lotteryId: Id) {
        val current = (_lotteries.value as? UiState.Content)?.data ?: return
        _lotteries.value = UiState.Content(current.copy(inProgress = current.inProgress.filterNot { it.id == lotteryId }))
    }

    private suspend fun fetch(user: User): EntrantLotteries = coroutineScope {
        val lotteries = user.lotteryIds.toList().map { id -> async { fetchLotteryOrNull(id) } }.awaitAll().filterNotNull()
        // Look each campsite up once, even if it is in several lotteries.
        val names = lotteries.flatMap { it.campsites }.distinct().map { id ->
            async { id to quietly("Couldn't load campsite $id") { Database.fetchCampsite(id).name } }
        }.awaitAll().mapNotNull { (id, name) -> name?.let { id to it } }.toMap()

        val now = Clock.System.now()
        val zone = ZoneId.systemDefault()

        EntrantLotteries(
            // Closing soonest first.
            inProgress = lotteries
                .filter { it.isInProgressFor(user.id, now, zone) }
                .sortedBy { it.entryEnd }
                .map { it.toSummary(names, now, zone) },
            // Most recent stays first.
            results = lotteries
                .sortedByDescending { it.firstStayNight }
                .mapNotNull { lottery ->
                    val (status, variant) = lottery.resultPill(user.id, now, zone) ?: return@mapNotNull null
                    lottery.toSummary(names, now, zone).copy(status = status, statusVariant = variant)
                },
        )
    }

    /**
     * The lottery, or null if it was deleted or can't be read, so one bad id doesn't hide the
     * rest. Connection problems still fail the whole list, so the entrant can retry.
     */
    private suspend fun fetchLotteryOrNull(id: Id): Lottery? =
        try {
            Database.fetchLottery(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Skipping lottery ${id.value}", e)
            null
        }

    private companion object {
        const val TAG = "EntrantLotteries"
    }
}

/**
 * True if [viewer] is on the waiting list and winners haven't been drawn yet (entries open, or
 * closed and waiting for the draw). These go on the In Progress tab.
 */
internal fun Lottery.isInProgressFor(
    viewer: Id,
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean = viewer in waitingList.users && phase(now, zone).let { it == LotteryPhase.OPEN || it == LotteryPhase.DRAWING }

/**
 * The Results-tab pill for [viewer], or null if this lottery doesn't belong on that tab
 * (not drawn yet, cancelled, or the viewer didn't enter).
 *
 * A winner who neither booked nor declined before the first night shows as "Not Accepted",
 * since they can no longer act on it.
 */
internal fun Lottery.resultPill(
    viewer: Id,
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Pair<String, PillVariant>? {
    if (!drawn || cancelled) return null
    if (viewer !in winners) {
        return if (viewer in waitingList.users) "Lost" to PillVariant.Danger else null
    }
    return when {
        bookings.any { it.user == viewer } -> "Won · Booked" to PillVariant.Outlined
        viewer in declined -> "Won · Not Accepted" to PillVariant.Muted
        phase(now, zone) == LotteryPhase.BOOKING -> "Won · Action Pending" to PillVariant.Highlight
        else -> "Won · Not Accepted" to PillVariant.Muted
    }
}
