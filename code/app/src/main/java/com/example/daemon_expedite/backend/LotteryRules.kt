package com.example.daemon_expedite.backend

import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

/**
 * Problems found in a lottery's settings. A null field means that setting is fine.
 * Messages are written to be shown under the matching form field.
 */
data class LotteryProblems(
    val campsites: String? = null,
    val entryPeriod: String? = null,
    val stayPeriods: String? = null,
    val winnerCount: String? = null,
    val waitlistCapacity: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(campsites, entryPeriod, stayPeriods, winnerCount, waitlistCapacity).all { it == null }
}

/** The rules a lottery's settings must follow. Pure functions, so they're easy to test. */
object LotteryRules {

    /**
     * Most winners a lottery can have: every night of every campsite booked by a different
     * winner. For example 2 campsites × 3 nights = 6.
     */
    fun maxWinners(campsiteCount: Int, stayPeriods: List<DatePeriod>): Int =
        campsiteCount * stayPeriods.sumOf { it.nights }

    /**
     * The entry period as moments: from the start of [firstDay] to the end of [lastDay]
     * (midnight after it), in the owner's [zone]. Winners are drawn at the second moment.
     */
    fun entryWindow(firstDay: LocalDate, lastDay: LocalDate, zone: ZoneId): Pair<Instant, Instant> =
        firstDay.startOfDay(zone) to lastDay.plusDays(1).startOfDay(zone)

    /**
     * Checks a new lottery's settings.
     *
     * - At least one campsite.
     * - Entries open today or later, and close on or after the day they open.
     * - At least one stay period, none overlapping, all after the last entry day (entrants
     *   need the time in between to book).
     * - 1 to [maxWinners] winners.
     * - The waitlist limit, if set, holds at least as many entrants as there are winners.
     *
     * @param entryFirstDay First day entrants can join, or null if not picked yet.
     * @param entryLastDay Last day entrants can join, or null if not picked yet.
     * @param winnerCount Null if the field is empty.
     * @param waitlistCapacity Null for no limit.
     * @param today The owner's today, passed in so tests can fix it.
     * @param entriesAlreadyOpen True when editing a lottery whose entries have opened: its open
     *   date is in the past and can't change, so the "can't open in the past" rule is skipped.
     */
    fun check(
        campsiteCount: Int,
        entryFirstDay: LocalDate?,
        entryLastDay: LocalDate?,
        stayPeriods: List<DatePeriod>,
        winnerCount: Int?,
        waitlistCapacity: Int?,
        today: LocalDate,
        entriesAlreadyOpen: Boolean = false,
    ): LotteryProblems {
        val sortedStays = stayPeriods.sortedBy { it.start }
        val firstStayNight = sortedStays.firstOrNull()?.start
        val maxWinners = maxWinners(campsiteCount, stayPeriods)

        return LotteryProblems(
            campsites = if (campsiteCount == 0) "Pick at least one campsite" else null,

            entryPeriod = when {
                entryFirstDay == null || entryLastDay == null -> "Pick when entries open and close"
                !entriesAlreadyOpen && entryFirstDay.isBefore(today) -> "Entries can't open in the past"
                entryLastDay.isBefore(entryFirstDay) -> "Entries must close on or after the day they open"
                firstStayNight != null && !entryLastDay.isBefore(firstStayNight) ->
                    "Entries must close before the first stay night, so winners have time to book"
                else -> null
            },

            stayPeriods = when {
                sortedStays.isEmpty() -> "Add at least one range of nights"
                !firstStayNight!!.isAfter(today) -> "Stay nights must start after today"
                sortedStays.zipWithNext().any { (a, b) -> a.overlaps(b) } -> "Stay periods can't overlap"
                else -> null
            },

            winnerCount = when {
                winnerCount == null || winnerCount < 1 -> "Enter at least 1 winner"
                maxWinners in 1..<winnerCount ->
                    "At most $maxWinners winners (campsites × nights available)"
                else -> null
            },

            waitlistCapacity = when {
                waitlistCapacity == null -> null
                winnerCount != null && waitlistCapacity < winnerCount ->
                    "The waitlist must hold at least as many entrants as there are winners"
                waitlistCapacity < 1 -> "Enter at least 1, or leave empty for no limit"
                else -> null
            },
        )
    }

    /**
     * Checks edited settings for [original]. Runs the same checks as [check], plus:
     *
     * - Only [LotteryPhase.UPCOMING] and [LotteryPhase.OPEN] lotteries can be edited.
     * - Once entries are open, the lottery can only grow, so nothing entrants signed up for is
     *   taken away: campsites and stay periods can be added but not removed, the open date is
     *   fixed, the close date can only move later, and the winner count and waitlist limit can
     *   only go up (or the limit be removed).
     * - The waitlist limit can never be below the number of entrants already on it.
     *
     * The database runs this again inside the saving transaction with the latest waitlist.
     *
     * @param phase [original]'s phase now.
     * @param zone The owner's time zone, to turn [original]'s entry times into days.
     */
    fun checkEdit(
        original: Lottery,
        phase: LotteryPhase,
        campsites: Set<Id>,
        entryFirstDay: LocalDate?,
        entryLastDay: LocalDate?,
        stayPeriods: List<DatePeriod>,
        winnerCount: Int?,
        waitlistCapacity: Int?,
        today: LocalDate,
        zone: ZoneId,
    ): LotteryProblems {
        if (phase != LotteryPhase.UPCOMING && phase != LotteryPhase.OPEN) {
            return LotteryProblems(entryPeriod = "Entries have closed, so this lottery can't be edited any more")
        }
        val open = phase == LotteryPhase.OPEN
        val base = check(
            campsites.size, entryFirstDay, entryLastDay, stayPeriods, winnerCount, waitlistCapacity,
            today, entriesAlreadyOpen = open,
        )
        val joined = original.waitingList.users.size
        val originalCapacity = original.waitlistCapacity

        return LotteryProblems(
            campsites = base.campsites ?: when {
                open && !campsites.containsAll(original.campsites) ->
                    "Entries are open, so campsites can be added but not removed"
                else -> null
            },
            entryPeriod = base.entryPeriod ?: when {
                open && entryFirstDay != original.entryFirstDay(zone) ->
                    "Entries are open, so the open date can't change"
                open && entryLastDay!!.isBefore(original.entryLastDay(zone)) ->
                    "Entries are open, so the close date can only move later"
                else -> null
            },
            stayPeriods = base.stayPeriods ?: when {
                open && !stayPeriods.containsAll(original.stayPeriods) ->
                    "Entries are open, so stay periods can be added but not removed"
                else -> null
            },
            winnerCount = base.winnerCount ?: when {
                open && winnerCount!! < original.winnerCount ->
                    "Entries are open, so winners can only go up (at least ${original.winnerCount})"
                else -> null
            },
            waitlistCapacity = base.waitlistCapacity ?: when {
                waitlistCapacity != null && waitlistCapacity < joined ->
                    "$joined entrants have already joined, so the limit can't be lower than that"
                open && waitlistCapacity != null && originalCapacity != null && waitlistCapacity < originalCapacity ->
                    "Entries are open, so the limit can only go up (at least $originalCapacity)"
                else -> null
            },
        )
    }
}
