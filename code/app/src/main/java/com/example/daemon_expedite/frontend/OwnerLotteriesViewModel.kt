package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.ui_components.PillVariant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** Everything a [com.example.daemon_expedite.ui_components.LotteryCard] shows, already formatted. */
data class LotterySummary(
    val id: Id,
    val title: String,
    val status: String,
    val statusVariant: PillVariant,
    val entriesText: String,
    val staysText: String,
    val countsText: String,
)

/** The owner's lotteries split for the Active and Past tabs. */
data class OwnerLotteries(
    val active: List<LotterySummary>,
    val past: List<LotterySummary>,
)

/**
 * Data for the owner's My Lotteries screen.
 *
 * Call [load] every time the screen is shown, so a lottery created on the Create screen
 * appears when the owner comes back. What is on screen stays there while it reloads.
 */
class OwnerLotteriesViewModel : ViewModel() {

    private val _lotteries = MutableStateFlow<UiState<OwnerLotteries>>(UiState.Loading)
    val lotteries: StateFlow<UiState<OwnerLotteries>> = _lotteries.asStateFlow()

    private var owner: Owner? = null

    fun load(owner: Owner) {
        this.owner = owner
        viewModelScope.launch {
            if (_lotteries.value !is UiState.Content) _lotteries.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load your lotteries") { fetch(owner) }
            if (fresh is UiState.Content || _lotteries.value !is UiState.Content) _lotteries.value = fresh
        }
    }

    fun retry() {
        owner?.let(::load)
    }

    private suspend fun fetch(owner: Owner): OwnerLotteries = coroutineScope {
        val lotteries = Database.fetchOwnerLotteries(owner.id)
        // Look each campsite up once, even if it is in several lotteries.
        val names = lotteries.flatMap { it.campsites }.distinct().map { id ->
            async { id to quietly("Couldn't load campsite $id") { Database.fetchCampsite(id).name } }
        }.awaitAll().mapNotNull { (id, name) -> name?.let { id to it } }.toMap()

        val now = Clock.System.now()
        val zone = ZoneId.systemDefault()
        val (past, active) = lotteries.partition { it.phase(now, zone).isPast }
        OwnerLotteries(
            // Soonest first; most recent first for past ones.
            active = active.sortedBy { it.entryStart }.map { it.toSummary(names, now, zone) },
            past = past.sortedByDescending { it.lastCheckOut }.map { it.toSummary(names, now, zone) },
        )
    }
}

/**
 * Formats a lottery for its card. [campsiteNames] maps campsite ids to names; campsites
 * missing from it (deleted, or failed to load) are left out of the title.
 */
internal fun Lottery.toSummary(
    campsiteNames: Map<Id, String>,
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): LotterySummary {
    val phase = phase(now, zone)
    val thisYear = now.toJavaInstant().atZone(zone).year
    fun LocalDate.short() = shortDate(thisYear)

    val names = campsites.mapNotNull { campsiteNames[it] }
    val title = when (names.size) {
        0 -> "Deleted campsite"
        1 -> names.single()
        else -> "${names.first()} + ${names.size - 1} more"
    }

    val firstEntryDay = firstEntryDay(zone)
    val lastEntryDay = lastEntryDay(zone)
    val entriesText = when (phase) {
        LotteryPhase.UPCOMING -> "Entries open ${firstEntryDay.short()}"
        LotteryPhase.OPEN -> "Entries close ${lastEntryDay.short()}"
        else -> "Entries closed ${lastEntryDay.short()}"
    }

    val nights = stayPeriods.sumOf { it.nights }
    val range = "${firstStayNight.short()} – ${lastCheckOut.short()}"
    val staysText = buildString {
        append(range)
        if (stayPeriods.size > 1) append(" · ${stayPeriods.size} periods")
        append(" · ").append(plural(nights, "night"))
    }

    val joined = waitingList.users.size
    val countsText = buildString {
        append(waitlistCapacity?.let { "$joined/$it joined" } ?: "$joined joined")
        append(" · ").append(plural(winnerCount, "winner"))
    }

    val (status, variant) = phase.pill()
    return LotterySummary(id, title, status, variant, entriesText, staysText, countsText)
}

/** Label and look of the status pill for a lottery in this phase. */
internal fun LotteryPhase.pill(): Pair<String, PillVariant> = when (this) {
    LotteryPhase.UPCOMING -> "Upcoming" to PillVariant.Outlined
    LotteryPhase.OPEN -> "Open" to PillVariant.Highlight
    LotteryPhase.DRAWING -> "Drawing" to PillVariant.Highlight
    LotteryPhase.BOOKING -> "Booking" to PillVariant.Highlight
    LotteryPhase.STAYS -> "In progress" to PillVariant.Outlined
    LotteryPhase.FINISHED -> "Finished" to PillVariant.Muted
    LotteryPhase.CANCELLED -> "Cancelled" to PillVariant.Muted
}

/** The last day entrants can join: [Lottery.entryEnd] is midnight after it. */
internal fun Lottery.lastEntryDay(zone: ZoneId): LocalDate =
    // A minute before midnight is still that day, even across a daylight-saving change.
    (entryEnd - 1.minutes).toJavaInstant().atZone(zone).toLocalDate()

internal fun Lottery.firstEntryDay(zone: ZoneId): LocalDate = entryStart.toJavaInstant().atZone(zone).toLocalDate()

/** "16 Oct", or "16 Oct 2027" when not in [thisYear]. */
internal fun LocalDate.shortDate(thisYear: Int): String = format(if (year == thisYear) DAY_MONTH else DAY_MONTH_YEAR)

internal fun plural(count: Int, word: String) = if (count == 1) "1 $word" else "$count ${word}s"

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
