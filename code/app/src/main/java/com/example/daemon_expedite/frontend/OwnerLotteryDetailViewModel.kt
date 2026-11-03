package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.CancelOutcome
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryNotCancellableException
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.ui_components.PillVariant
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
import kotlin.time.toJavaInstant

/**
 * Everything the owner's lottery details page shows, already formatted.
 *
 * @property campsites The lottery's campsites, in order. Deleted ones are named "Deleted campsite".
 * @property entryText When entries open and close.
 * @property phaseNote One line on where the lottery is now, for example "Entrants can join now".
 * @property stayPeriods The bookable nights, for the calendar.
 * @property stayTexts One line per stay period, for example "16 Oct → 18 Oct · 2 nights".
 * @property drawText Entrants and winners, for example "12/20 joined · 4 winners to draw".
 * @property canEdit True before the draw (see [LotteryPhase.UPCOMING] and [LotteryPhase.OPEN]).
 * @property canCancel See [LotteryPhase.canCancel].
 * @property deletesOnCancel True if cancelling deletes it, because it hasn't opened yet.
 */
data class LotteryDetails(
    val id: Id,
    val title: String,
    val status: String,
    val statusVariant: PillVariant,
    val campsites: List<CampsiteOption>,
    val entryText: String,
    val phaseNote: String,
    val stayPeriods: List<DatePeriod>,
    val stayTexts: List<String>,
    val drawText: String,
    val canEdit: Boolean,
    val canCancel: Boolean,
    val deletesOnCancel: Boolean,
)

/** Outcome of [OwnerLotteryDetailViewModel.cancel]. */
enum class CancelResult {
    /** Deleted, because it hadn't opened yet. */
    DELETED,

    /** Marked cancelled. */
    CANCELLED,

    /** Stays have started (or it's over) since the page loaded, so it can't be cancelled. */
    TOO_LATE,

    /** Not cancelled, usually no connection. */
    FAILED,
}

/**
 * Data for the owner's lottery details page, plus cancelling it.
 *
 * The lottery id comes from the navigation route (see [Routes.OWNER_LOTTERY_DETAIL]).
 * Call [load] every time the page is shown, so changes saved on the edit screen show up.
 */
class OwnerLotteryDetailViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val lotteryId: Id? = savedState.get<String>(Routes.LOTTERY_ID_ARG).toIdOrNull()

    private val _details = MutableStateFlow<UiState<LotteryDetails>>(UiState.Loading)
    val details: StateFlow<UiState<LotteryDetails>> = _details.asStateFlow()

    private val _isCancelling = MutableStateFlow(false)
    val isCancelling: StateFlow<Boolean> = _isCancelling.asStateFlow()

    /** Loads (or reloads) the lottery. What is on screen stays there while it reloads. */
    fun load() {
        val id = lotteryId ?: run {
            _details.value = UiState.Error("Couldn't find this lottery")
            return
        }
        viewModelScope.launch {
            if (_details.value !is UiState.Content) _details.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load this lottery") { fetch(id) }
            if (fresh is UiState.Content || _details.value !is UiState.Content) _details.value = fresh
        }
    }

    /** Cancels the lottery, or deletes it if it hasn't opened yet. See [Database.cancelLottery]. */
    fun cancel(onResult: (CancelResult) -> Unit) {
        val id = lotteryId ?: return onResult(CancelResult.FAILED)
        if (_isCancelling.value) return
        _isCancelling.value = true
        viewModelScope.launch {
            val result = try {
                when (Database.cancelLottery(id)) {
                    CancelOutcome.DELETED -> CancelResult.DELETED
                    CancelOutcome.CANCELLED -> CancelResult.CANCELLED
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Firestore wraps exceptions thrown inside a transaction, so look at the cause too.
                if (e is LotteryNotCancellableException || e.cause is LotteryNotCancellableException) {
                    CancelResult.TOO_LATE
                } else {
                    Log.e("OwnerLotteryDetail", "Cancelling lottery $id failed", e)
                    CancelResult.FAILED
                }
            }
            _isCancelling.value = false
            onResult(result)
        }
    }

    private suspend fun fetch(id: Id): LotteryDetails = coroutineScope {
        val lottery = Database.fetchLottery(id)
        val names = lottery.campsites.map { campsiteId ->
            async { campsiteId to quietly("Couldn't load campsite $campsiteId") { Database.fetchCampsite(campsiteId).name } }
        }.awaitAll().mapNotNull { (campsiteId, name) -> name?.let { campsiteId to it } }.toMap()
        lottery.toLotteryDetails(names)
    }
}

/**
 * Formats a lottery for its details page. [campsiteNames] maps campsite ids to names; missing
 * ones (deleted, or failed to load) are shown as "Deleted campsite".
 */
internal fun Lottery.toLotteryDetails(
    campsiteNames: Map<Id, String>,
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): LotteryDetails {
    val phase = phase(now, zone)
    val thisYear = now.toJavaInstant().atZone(zone).year
    val summary = toSummary(campsiteNames, now, zone)
    val (status, variant) = phase.pill()

    val opens = firstEntryDay(zone).shortDate(thisYear)
    val closes = lastEntryDay(zone).shortDate(thisYear)
    val firstNight = firstStayNight.shortDate(thisYear)

    val phaseNote = when (phase) {
        LotteryPhase.UPCOMING -> "Entrants can join from $opens. Winners are drawn when entries close."
        LotteryPhase.OPEN -> "Entrants can join now. Winners are drawn at the end of $closes."
        LotteryPhase.DRAWING -> "Entries are closed. Winners are being drawn."
        LotteryPhase.BOOKING -> "Winners are drawn. They can book until the first night, $firstNight."
        LotteryPhase.STAYS -> "Stays are under way."
        LotteryPhase.FINISHED -> "This lottery is over."
        LotteryPhase.CANCELLED -> "You cancelled this lottery."
    }

    val joined = waitingList.users.size
    val joinedText = waitlistCapacity?.let { "$joined/$it joined" } ?: "$joined joined"
    val drawText = if (drawn) {
        "$joinedText · ${winners.size} of ${plural(winnerCount, "winner")} drawn · ${bookings.size} booked"
    } else {
        "$joinedText · ${plural(winnerCount, "winner")} to draw"
    }

    return LotteryDetails(
        id = id,
        title = summary.title,
        status = status,
        statusVariant = variant,
        campsites = campsites.map { CampsiteOption(it, campsiteNames[it] ?: "Deleted campsite") },
        entryText = "Opens $opens · Closes end of $closes",
        phaseNote = phaseNote,
        stayPeriods = stayPeriods,
        stayTexts = stayPeriods.map {
            "${it.start.shortDate(thisYear)} → ${it.end.shortDate(thisYear)} · ${plural(it.nights, "night")}"
        },
        drawText = drawText,
        canEdit = phase == LotteryPhase.UPCOMING || phase == LotteryPhase.OPEN,
        canCancel = phase.canCancel,
        deletesOnCancel = phase == LotteryPhase.UPCOMING,
    )
}
