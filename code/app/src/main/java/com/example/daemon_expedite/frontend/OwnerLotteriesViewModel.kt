// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.InviteStatus
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

/**
 * A lottery another owner invited this owner's campsites to, for the Requests tab.
 *
 * @property title The lottery's campsites, counting the invited ones, e.g. "Pine Creek Hideaway + 1 more".
 * @property invitedByText Who sent it, e.g. "Invited by Alex Chen".
 * @property staysText When the stays are, e.g. "16 Oct – 18 Oct · 2 nights".
 * @property awaitingText How many of this owner's campsites wait for an answer.
 */
data class CollaborationRequest(
    val id: Id,
    val title: String,
    val invitedByText: String,
    val staysText: String,
    val awaitingText: String,
)

/** The owner's lotteries split for the Active and Past tabs, plus invites for the Requests tab. */
data class OwnerLotteries(
    val active: List<LotterySummary>,
    val past: List<LotterySummary>,
    val requests: List<CollaborationRequest> = emptyList(),
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
        val invitedTo = async { Database.fetchPendingInvites(owner.id) }
        val lotteries = Database.fetchOwnerLotteries(owner.id)
        val requests = invitedTo.await()
        // Look each campsite up once, even if it is in several lotteries.
        val campsiteIds = lotteries.flatMap { it.campsites } + requests.flatMap { it.campsites + it.pendingCampsites }
        val names = campsiteIds.distinct().map { id ->
            async { id to quietly("Couldn't load campsite $id") { Database.fetchCampsite(id).name } }
        }.awaitAll().mapNotNull { (id, name) -> name?.let { id to it } }.toMap()
        val creators = requests.map { it.ownerId }.distinct().map { id ->
            async { id to quietly("Couldn't load owner $id") { Database.fetchUser(id)["name"] } }
        }.awaitAll().mapNotNull { (id, name) -> name?.takeIf { it.isNotBlank() }?.let { id to it } }.toMap()

        val now = Clock.System.now()
        val zone = ZoneId.systemDefault()
        val (past, active) = lotteries.partition { it.phase(now, zone).isPast }
        fun Lottery.summary() = toSummary(names, now, zone).let {
            if (ownerId == owner.id) it else it.copy(entriesText = "Collaborator · ${it.entriesText}")
        }
        OwnerLotteries(
            // Soonest first; most recent first for past ones.
            active = active.sortedBy { it.entryStart }.map { it.summary() },
            past = past.sortedByDescending { it.lastCheckOut }.map { it.summary() },
            requests = requests.sortedBy { it.entryEnd }.map { it.toRequest(owner.id, names, creators[it.ownerId], now, zone) },
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

    val title = campsiteTitle(campsites.mapNotNull { campsiteNames[it] })

    val firstEntryDay = entryFirstDay(zone)
    val lastEntryDay = entryLastDay(zone)
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

/**
 * Formats a lottery that invited [viewerId]'s campsites for the Requests tab. [creatorName]
 * is the inviting owner's name, or null if it couldn't be loaded.
 */
internal fun Lottery.toRequest(
    viewerId: Id,
    campsiteNames: Map<Id, String>,
    creatorName: String?,
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): CollaborationRequest {
    val mine = invites.filter { it.campsiteOwner == viewerId && it.status == InviteStatus.PENDING }
    val withInvited = campsites + mine.map { it.campsite }
    val summary = toSummary(campsiteNames, now, zone)
    return CollaborationRequest(
        id = id,
        title = campsiteTitle(withInvited.mapNotNull { campsiteNames[it] }),
        invitedByText = "Invited by ${creatorName ?: "another owner"}",
        staysText = summary.staysText,
        awaitingText = "${mine.size} of your campsites awaiting approval",
    )
}

/** "Pine Creek Hideaway", or "Pine Creek Hideaway + 2 more" for several campsites. */
internal fun campsiteTitle(names: List<String>): String = when (names.size) {
    0 -> "Deleted campsite"
    1 -> names.single()
    else -> "${names.first()} + ${names.size - 1} more"
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

/** "16 Oct", or "16 Oct 2027" when not in [thisYear]. */
internal fun LocalDate.shortDate(thisYear: Int): String = format(if (year == thisYear) DAY_MONTH else DAY_MONTH_YEAR)

internal fun plural(count: Int, word: String) = if (count == 1) "1 $word" else "$count ${word}s"

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
