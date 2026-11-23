// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

<<<<<<< Updated upstream
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

/** Where a [CampsiteInvite] stands. */
enum class InviteStatus { PENDING, ACCEPTED, DECLINED }

/**
 * A request from a lottery's creator to add another owner's campsite to it.
 *
 * @property campsite The campsite asked for.
 * @property campsiteOwner The campsite's owner, who accepts or declines. Copied from
 *   [Campsite.ownerId] so invites can be found by owner without loading the campsite.
 * @property status Accepted is final: a co-owner can't leave once they accept.
 */
data class CampsiteInvite(
    val campsite: Id,
    val campsiteOwner: Id,
    val status: InviteStatus = InviteStatus.PENDING,
)

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
 * Several owners can share a lottery: the creator invites other owners' campsites (see
 * [invite]) and each owner accepts or declines before entries close. Only the creator
 * edits, cancels and messages the waiting list.
 *
 * @property ownerId The owner who created it.
 * @property campsites Campsites in the lottery: the creator's own, plus invited ones once accepted.
 * @property invites Invites sent to other owners, in any status.
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
    invites: List<CampsiteInvite> = emptyList(),
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
    var invites: List<CampsiteInvite> = invites.toList()
        private set

    /** The optional waitlist limit, or null for no limit. */
    val waitlistCapacity: Int? get() = waitingList.capacity.takeIf { it != Int.MAX_VALUE }

    /** The creator plus every owner who accepted an invite. */
    val ownerIds: Set<Id>
        get() = setOf(ownerId) + invites.filter { it.status == InviteStatus.ACCEPTED }.map { it.campsiteOwner }

    /** Owners who still have to answer an invite. */
    val pendingOwners: Set<Id>
        get() = invites.filter { it.status == InviteStatus.PENDING }.map { it.campsiteOwner }.toSet()

    /** Campsites invited but not answered yet. Not part of [campsites]. */
    val pendingCampsites: List<Id>
        get() = invites.filter { it.status == InviteStatus.PENDING }.map { it.campsite }

    /** The owner of [campsite], which must be in [campsites]: a co-owner's if it came from an invite. */
    fun ownerOf(campsite: Id): Id =
        invites.firstOrNull { it.campsite == campsite && it.status == InviteStatus.ACCEPTED }?.campsiteOwner ?: ownerId

    /** Invites can be sent and answered until entries close, unless it was cancelled. */
    fun canChangeInvites(now: Instant = Clock.System.now()): Boolean = !cancelled && now < entryEnd

    /**
     * Invites [campsite] from another owner. Only changes memory; save afterwards.
     *
     * @return false if entries have closed, the lottery was cancelled, the campsite is the
     *   creator's own, already in the lottery, or was invited before (in any status).
     */
    fun invite(campsite: Campsite, now: Instant = Clock.System.now()): Boolean {
        if (!canChangeInvites(now)) return false
        if (campsite.ownerId == ownerId || campsite.id in campsites) return false
        if (invites.any { it.campsite == campsite.id }) return false
        invites = invites + CampsiteInvite(campsite.id, campsite.ownerId)
        return true
    }

    /**
     * [by] accepts the invite for [campsite], adding it to [campsites]. Final: there is no way
     * to leave afterwards. Only changes memory; save afterwards.
     *
     * @return false if entries have closed, the lottery was cancelled, there is no pending
     *   invite for [campsite], or [by] doesn't own it.
     */
    fun acceptInvite(campsite: Id, by: Id, now: Instant = Clock.System.now()): Boolean {
        val invite = pendingInvite(campsite, by, now) ?: return false
        invites = invites.map { if (it == invite) it.copy(status = InviteStatus.ACCEPTED) else it }
        campsites = campsites + campsite
        return true
    }

    /** [by] declines the invite for [campsite]. Same rules as [acceptInvite]. */
    fun declineInvite(campsite: Id, by: Id, now: Instant = Clock.System.now()): Boolean {
        val invite = pendingInvite(campsite, by, now) ?: return false
        invites = invites.map { if (it == invite) it.copy(status = InviteStatus.DECLINED) else it }
        return true
    }

    private fun pendingInvite(campsite: Id, by: Id, now: Instant): CampsiteInvite? {
        if (!canChangeInvites(now)) return null
        return invites.firstOrNull {
            it.campsite == campsite && it.campsiteOwner == by && it.status == InviteStatus.PENDING
        }
    }

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
=======
import kotlin.time.Clock
import kotlin.time.Instant

class Lottery(
    val id: Id = Id(),
    campsites: List<Id> = emptyList(),
    maxBookableByASingleUser: Int,
    registrationStart: Instant,
    registrationEnd: Instant,
    bookingStartDate: Instant,
    bookingEndDate: Instant,
    winnerCount: Int,
    winners: List<Id> = emptyList(),
    lotteryStart: Instant,
    lotteryEnd: Instant,
    availableDates: List<Instant> = emptyList(),
    waitingList: WaitingList = WaitingList()
) {
    var maxBookableByASingleUser: Int = maxBookableByASingleUser
        private set
    var registrationStart: Instant = registrationStart
        private set
    var registrationEnd: Instant = registrationEnd
        private set
    var bookingStartDate: Instant = bookingStartDate
        private set
    var bookingEndDate: Instant = bookingEndDate
        private set
    var winnerCount: Int = winnerCount
        private set
    var lotteryStart: Instant = lotteryStart
        private set
    var lotteryEnd: Instant = lotteryEnd
        private set
    var waitingList: WaitingList = waitingList
        private set

    private val _campsites = campsites.toMutableList()
    val campsites: List<Id> get() = _campsites

    private val _winners = winners.toMutableList()
    val winners: List<Id> get() = _winners

    private val _availableDates = availableDates.toMutableList()
    val availableDates: List<Instant> get() = _availableDates

    fun isActive(): Boolean {
        val now = Clock.System.now()
        return now >= lotteryStart && now < lotteryEnd
    }

    fun joinWaitingList(user: Id): Boolean {
        if (!isActive()) return false
        if (waitingList.add(user)) {
            Database.addEntrant(id, user)
            return true
        }
        return false
    }

    fun leaveWaitingList(user: Id): Boolean {
        if (!isActive()) return false
        if (waitingList.remove(user)) {
            Database.removeEntrant(id, user)
            return true
        }
        return false
    }

}
>>>>>>> Stashed changes
