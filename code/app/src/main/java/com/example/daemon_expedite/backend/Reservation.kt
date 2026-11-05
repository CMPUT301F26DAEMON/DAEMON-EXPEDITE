package com.example.daemon_expedite.backend

import java.time.LocalDate

/**
 * A booking of a [Campsite] by a [User] for a range of nights.
 *
 * A reservation is created when a lottery winner books their stay (see
 * [Database.bookLotteryStay]). It starts out not cancelled and can later be cancelled by the user
 * or rejected by the campsite owner, which sets [isCancelled] to `true`.
 * Whether the stay has finished is computed from [period] by [isPast], not stored.
 *
 * @property id unique identifier of this reservation.
 * @property campsite id of the [Campsite] being reserved.
 * @property period the nights reserved: from the first night to the check-out day.
 * @property reservedBy id of the [User] who holds the reservation.
 * @property lotteryId the [Lottery] this reservation was won in, or null if it wasn't.
 * @property isCancelled `true` if the reservation was cancelled (declined, or rejected). Only changed through [cancel].
 */
class Reservation(
    val id: Id = Id(),
    val campsite: Id,
    val period: DatePeriod,
    val reservedBy: Id,
    val lotteryId: Id? = null,
    isCancelled: Boolean = false,
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
        }
    }

    /**
     * Checks whether the stay is over: it is once the check-out day has arrived.
     *
     * @param today the current day; defaults to the device's today, overridable for testing.
     * @return `true` if the stay has finished, `false` otherwise.
     */
    fun isPast(today: LocalDate = LocalDate.now()): Boolean = !today.isBefore(period.end)
}
