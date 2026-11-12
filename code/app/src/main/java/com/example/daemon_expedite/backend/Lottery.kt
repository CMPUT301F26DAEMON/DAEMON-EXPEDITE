// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * One winner's booking: a single campsite for one continuous range of nights.
 *
 * @property user The winner who booked.
 * @property campsite The campsite booked. Must be one of the lottery's campsites.
 * @property period The nights booked. Must fit inside one of the lottery's stay periods.
 * @property reservation The [Reservation] created for this booking.
 */
data class Booking(val user: Id, val campsite: Id, val period: DatePeriod, val reservation: Id? = null)

/**
 * Where a lottery is in its life. Worked out from its dates by [Lottery.phase]; only
 * [Lottery.drawn] and [Lottery.cancelled] are stored.
 */
enum class LotteryPhase {
    /** Entry period hasn't started yet. */
    UPCOMING,

    /** Entrants can join or leave the waiting list. */
    OPEN,

    /** Entry period is over, waiting for the Cloud Function to draw winners. */
    DRAWING,

    /** Winners are drawn and can book, first come, first served, until the first stay night. */
    BOOKING,

    /** Stays are under way. Nothing more can be booked. */
    STAYS,

    /** The last stay is over. */
    FINISHED,

    /** The owner cancelled it. */
    CANCELLED;

    /** True for lotteries that belong under "Past": finished or canceled. */
    val isPast: Boolean get() = this == FINISHED || this == CANCELLED

    /** True while the owner may still cancel: any time before the first stay night. */
    val canCancel: Boolean get() = this in setOf(UPCOMING, OPEN, DRAWING, BOOKING)
}

/**
 * A lottery for one or more campsites.
 *
 * Timeline:
 * 1. **Entry period** ([entryStart] to [entryEnd]): entrants join the waiting list.
 * 2. **Draw**: at [entryEnd] a Cloud Function picks [winnerCount] winners (everyone wins if
 *    fewer joined) and sets [drawn].
 * 3. **Booking window**: until the first night of [stayPeriods], each winner can book one
 *    campsite for one continuous range of nights, first come, first served (see [bookings]).
 * 4. **Stays**: the nights in [stayPeriods].
 *
 * A campsite can only be in one lottery that isn't past (see [LotteryPhase.isPast]).
 *
 * @property ownerId The owner who created it.
 * @property campsites Campsites in the lottery. All belong to [ownerId].
 * @property entryStart When entrants can start joining (start of the owner's first entry day).
 * @property entryEnd When joining closes and winners are drawn (end of the owner's last entry day).
 * @property stayPeriods The nights winners can book, sorted and not overlapping. Need not be
 * next to each other, for example three weekends.
 * @property winnerCount How many entrants to draw. See [LotteryRules.maxWinners].
 * @property waitingList Entrants who joined. Its capacity is the optional waitlist limit.
 * @property winners Entrants drawn. Empty until [drawn].
 * @property bookings Bookings made by winners, at most one per winner.
 * @property declined Winners who turned their win down. Final: they can't book anymore.
 * @property drawn Set by the Cloud Function once winners are picked.
 * @property cancelled Set when the owner cancels. Canceled lotteries are kept so entrants
 * can still see them under past lotteries.
 */
class Lottery(
    val id: Id = Id(),
    val ownerId: Id,
    campsites: List<Id>,
    entryStart: Instant,
    entryEnd: Instant,
    stayPeriods: List<DatePeriod>,
    winnerCount: Int,
    val waitingList: WaitingList = WaitingList(),
    winners: List<Id> = emptyList(),
    bookings: List<Booking> = emptyList(),
    declined: List<Id> = emptyList(),
    drawn: Boolean = false,
    cancelled: Boolean = false,
) {
    var campsites: List<Id> = campsites.toList()
        private set
    var entryStart: Instant = entryStart
        private set
    var entryEnd: Instant = entryEnd
        private set
    var stayPeriods: List<DatePeriod> = stayPeriods.sortedBy { it.start }
        private set
    var winnerCount: Int = winnerCount
        private set
    var winners: List<Id> = winners.toList()
        private set
    var bookings: List<Booking> = bookings.toList()
        private set
    var declined: List<Id> = declined.toList()
        private set
    var drawn: Boolean = drawn
        private set
    var cancelled: Boolean = cancelled
        private set

    /** The optional waitlist limit, or null for no limit. */
    val waitlistCapacity: Int? get() = waitingList.capacity.takeIf { it != Int.MAX_VALUE }

    /** First night anyone can stay. Booking closes when this day starts. */
    val firstStayNight: LocalDate get() = stayPeriods.first().start

    /** Check-out day of the last stay. The lottery is finished once this day starts. */
    val lastCheckOut: LocalDate get() = stayPeriods.maxOf { it.end }

    /** First day entrants can join, in [zone]. */
    fun entryFirstDay(zone: ZoneId): LocalDate = entryStart.toJavaInstant().atZone(zone).toLocalDate()

    /**
     * Last day entrants can join, in [zone]. [entryEnd] is midnight after it; a minute earlier
     * is still that day, even across a daylight-saving change.
     */
    fun entryLastDay(zone: ZoneId): LocalDate = (entryEnd - 1.minutes).toJavaInstant().atZone(zone).toLocalDate()

    /**
     * Where the lottery is at [now]. Stay dates are turned into moments using [zone], the
     * device's time zone by default (the owner's zone, for the owner).
     */
    fun phase(now: Instant = Clock.System.now(), zone: ZoneId = ZoneId.systemDefault()): LotteryPhase =
        when {
            cancelled -> LotteryPhase.CANCELLED
            now < entryStart -> LotteryPhase.UPCOMING
            now < entryEnd -> LotteryPhase.OPEN
            !drawn -> LotteryPhase.DRAWING
            now < firstStayNight.startOfDay(zone) -> LotteryPhase.BOOKING
            now < lastCheckOut.startOfDay(zone) -> LotteryPhase.STAYS
            else -> LotteryPhase.FINISHED
        }

    /**
     * Adds [user] to the waiting list.
     *
     * @return false if the lottery isn't open, is full, or [user] already joined.
     */
    fun joinWaitingList(user: Id): Boolean {
        if (phase() != LotteryPhase.OPEN) return false
        if (user in waitingList.users || waitingList.isFull()) return false
        return waitingList.add(user)
    }

    /**
     * Removes [user] from the waiting list.
     * Returns false if the lottery isn't open, or they weren't on it.
     */
    fun leaveWaitingList(user: Id): Boolean {
        if (phase() != LotteryPhase.OPEN) return false
        if (user !in waitingList.users) return false
        return waitingList.remove(user)
    }
}

/** Midnight at the start of this day in [zone]. */
internal fun LocalDate.startOfDay(zone: ZoneId): Instant = atStartOfDay(zone).toInstant().toKotlinInstant()

enum class CancelOutcome {
    /** It hadn't opened yet, so it was deleted. */
    DELETED,

    /** It was marked canceled and kept. */
    CANCELLED,
}
