package com.example.daemon_expedite.backend

import java.time.LocalDate

/**
 * The rules for a lottery winner's booking. Pure functions, so they're easy to test, and run
 * both on the booking screen and again inside the booking transaction.
 *
 * Nights are counted as in [DatePeriod]: a booking from 16 Oct to 18 Oct takes the nights of the
 * 16th and 17th, and the 18th is the check-out day.
 */
object BookingRules {

    /** Nights of [campsite] already taken by bookings in [lottery]. */
    fun bookedNights(lottery: Lottery, campsite: Id): Set<LocalDate> =
        lottery.bookings
            .filter { it.campsite == campsite }
            .flatMap { it.period.nightsList() }
            .toSet()

    /** True if [night] is a bookable night of [lottery] that nobody has taken on [campsite]. */
    fun isFreeNight(lottery: Lottery, campsite: Id, night: LocalDate, booked: Set<LocalDate> = bookedNights(lottery, campsite)): Boolean =
        night !in booked && lottery.stayPeriods.any { !night.isBefore(it.start) && night.isBefore(it.end) }

    /**
     * True if [day] can be tapped on the booking calendar: either as a first night (it is a free
     * night) or as a check-out day (the night before it is free, in the same stay period).
     */
    fun isSelectable(lottery: Lottery, campsite: Id, day: LocalDate, booked: Set<LocalDate> = bookedNights(lottery, campsite)): Boolean =
        isFreeNight(lottery, campsite, day, booked) ||
            lottery.stayPeriods.any { period ->
                val nightBefore = day.minusDays(1)
                day.isAfter(period.start) && !day.isAfter(period.end) &&
                    isFreeNight(lottery, campsite, nightBefore, booked)
            }

    /**
     * Checks a booking of [campsite] for [period] by [user]. Returns a message saying what's
     * wrong, or null if it can be booked.
     *
     * - The lottery must be in its booking window and [user] must be a winner who hasn't
     *   declined and hasn't booked already (one campsite, one stay per winner).
     * - [campsite] must be in the lottery.
     * - Every night must be inside one stay period and not booked by anyone else.
     */
    fun check(lottery: Lottery, phase: LotteryPhase, user: Id, campsite: Id, period: DatePeriod): String? {
        val booked = bookedNights(lottery, campsite)
        return when {
            phase != LotteryPhase.BOOKING -> "Booking for this lottery is closed"
            user !in lottery.winners -> "Only winners can book"
            user in lottery.declined -> "You declined this lottery"
            lottery.bookings.any { it.user == user } -> "You've already booked your stay"
            campsite !in lottery.campsites -> "That campsite isn't in this lottery"
            lottery.stayPeriods.none { it.contains(period) } -> "Pick nights inside one of the stay periods"
            period.nightsList().any { it in booked } -> "Some of those nights were just booked by someone else"
            else -> null
        }
    }
}

/** Every night in this period: from [DatePeriod.start] up to the day before [DatePeriod.end]. */
fun DatePeriod.nightsList(): List<LocalDate> = (0 until nights).map { start.plusDays(it.toLong()) }
