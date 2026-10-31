package com.example.daemon_expedite.backend

import kotlin.time.Instant
import kotlin.time.Clock

/**
 * A booking of a [Campsite] by a [User] for a set of dates.
 *
 * A reservation is created after a user wins a [Lottery] for a campsite and decided to reserve.
 * It starts out not cancelled and can later be declined/rejected by the user/campsite owner,
 * which sets [isCancelled] to `true`.
 * Whether the stay has finished is computed from [reservationDates] by [isPast], not stored.
 *
 * @property id unique identifier of this reservation.
 * @property campsite id of the [Campsite] being reserved.
 * @property reservationDates the dates covered by this reservation.
 * @property reservedBy id of the [User] who holds the reservation.
 * @property isCancelled `true` if the reservation was canceled (declined, or rejected). Only changed through [cancel].
 */
class Reservation(
    val id: Id = Id(),
    private val campsite: Id,
    val reservationDates: Array<Instant>,
    val reservedBy: Id,
    isCancelled: Boolean = false
) {

    var isCancelled: Boolean = isCancelled
        private set


    /**
     * Cancels this reservation, e.g. when the user cancels their booking.
     * Has no effect if it is already cancelled.
     */
    fun cancel() {
        if (!isCancelled) {
            isCancelled = true
            // Database.updateReservation(this)
        }
    }


    /**
     * Checks whether the last date in [reservationDates] is before [now].
     *
     * @param now the current time; defaults to the system clock, overridable for testing.
     * @return `true` if the stay has finished, `false` otherwise (or if there are no dates).
     */
    fun isPast(now: Instant = Clock.System.now()): Boolean {
        val lastDate = reservationDates.maxOrNull() ?: return false
        return lastDate < now
    }
}