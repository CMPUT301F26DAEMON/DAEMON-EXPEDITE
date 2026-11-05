package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.Booking
import com.example.daemon_expedite.backend.BookingRules
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.backend.nightsList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.toKotlinInstant

class BookingRulesTest {

    private val zone = ZoneId.of("America/Edmonton")
    private val pine = Id()
    private val maple = Id()
    private val me = Id()
    private val rival = Id()

    private fun day(d: Int) = LocalDate.of(2026, 10, d)
    private fun nights(from: Int, to: Int) = DatePeriod(day(from), day(to))

    /** Stays 16–20 Oct and 23–25 Oct; [rival] booked Pine 16–18; both of us won. */
    private fun lottery(declined: List<Id> = emptyList(), extraBookings: List<Booking> = emptyList()) = Lottery(
        ownerId = Id(),
        campsites = listOf(pine, maple),
        entryStart = day(1).atStartOfDay(zone).toInstant().toKotlinInstant(),
        entryEnd = day(6).atStartOfDay(zone).toInstant().toKotlinInstant(),
        stayPeriods = listOf(nights(16, 20), nights(23, 25)),
        winnerCount = 2,
        winners = listOf(me, rival),
        bookings = listOf(Booking(rival, pine, nights(16, 18))) + extraBookings,
        declined = declined,
        drawn = true,
    )

    private fun check(campsite: Id = pine, period: DatePeriod = nights(18, 20), user: Id = me, l: Lottery = lottery(), phase: LotteryPhase = LotteryPhase.BOOKING) =
        BookingRules.check(l, phase, user, campsite, period)

    @Test
    fun nightsList_excludesCheckOutDay() {
        assertEquals(listOf(day(16), day(17)), nights(16, 18).nightsList())
    }

    @Test
    fun bookedNights_onlyForThatCampsite() {
        assertEquals(setOf(day(16), day(17)), BookingRules.bookedNights(lottery(), pine))
        assertTrue(BookingRules.bookedNights(lottery(), maple).isEmpty())
    }

    @Test
    fun freeRangeCanBeBooked() {
        assertNull(check()) // starts on the rival's check-out day: fine
        assertNull(check(campsite = maple, period = nights(16, 18))) // same nights, other campsite
        assertNull(check(period = nights(23, 24))) // one night is enough
    }

    @Test
    fun takenNightsAreRefused() {
        assertNotNull(check(period = nights(17, 19)))
    }

    @Test
    fun mustFitInsideOneStayPeriod() {
        assertNotNull(check(period = nights(19, 24))) // spans the gap between periods
        assertNotNull(check(period = nights(20, 21))) // outside every period
    }

    @Test
    fun onlyEligibleWinnersCanBook() {
        assertNotNull(check(user = Id())) // not a winner
        assertNotNull(check(l = lottery(declined = listOf(me))))
        assertNotNull(check(l = lottery(extraBookings = listOf(Booking(me, maple, nights(23, 25)))))) // one booking each
        assertNotNull(check(phase = LotteryPhase.STAYS)) // window closed
        assertNotNull(check(campsite = Id())) // not in the lottery
    }

    @Test
    fun selectableDays_firstNightsAndCheckOuts() {
        val l = lottery()
        // 16 and 17 are taken on Pine, but 18 is free (and is also the rival's check-out day).
        assertFalse(BookingRules.isSelectable(l, pine, day(16)))
        assertFalse(BookingRules.isSelectable(l, pine, day(17)))
        assertTrue(BookingRules.isSelectable(l, pine, day(18)))
        // 20 is the end of the period: a check-out day only.
        assertTrue(BookingRules.isSelectable(l, pine, day(20)))
        // 21 and 22 are between periods.
        assertFalse(BookingRules.isSelectable(l, pine, day(21)))
        assertFalse(BookingRules.isSelectable(l, pine, day(22)))
        // Everything in a period is open on Maple.
        assertTrue(BookingRules.isSelectable(l, maple, day(16)))
    }

    // region Cancelling

    private fun myReservation(period: DatePeriod = nights(18, 20), cancelled: Boolean = false) =
        Reservation(campsite = pine, period = period, reservedBy = me, isCancelled = cancelled)

    @Test
    fun cancel_allowedUntilTheDayBeforeTheStay() {
        assertNull(BookingRules.cancelProblem(myReservation(), me, day(17)))
        assertNotNull(BookingRules.cancelProblem(myReservation(), me, day(18))) // first night
        assertNotNull(BookingRules.cancelProblem(myReservation(), me, day(19))) // under way
    }

    @Test
    fun cancel_onlyByWhoBookedAndOnlyOnce() {
        assertNotNull(BookingRules.cancelProblem(myReservation(), rival, day(10)))
        assertNotNull(BookingRules.cancelProblem(myReservation(cancelled = true), me, day(10)))
    }

    @Test
    fun cancel_freesTheNightsSoTheWinnerCanBookAgain() {
        val reservation = myReservation()
        val mine = Booking(me, pine, reservation.period, reservation.id)
        val l = lottery(extraBookings = listOf(mine))
        assertNotNull(check(l = l)) // already booked

        val left = BookingRules.bookingsAfterCancel(l, reservation)
        assertEquals(listOf(rival), left.map { it.user }) // the rival's booking stays
        assertNull(check(l = lottery(extraBookings = left - lottery().bookings.toSet()))) // free again, and I may rebook
    }

    @Test
    fun cancel_matchesOldBookingsWithoutAReservationId() {
        val l = lottery(extraBookings = listOf(Booking(me, pine, nights(18, 20))))
        assertEquals(listOf(rival), BookingRules.bookingsAfterCancel(l, myReservation()).map { it.user })
    }

    @Test
    fun cancel_isFinalOnlyAfterTheBookingWindow() {
        assertFalse(BookingRules.cancelGivesUpWin(LotteryPhase.BOOKING))
        assertTrue(BookingRules.cancelGivesUpWin(LotteryPhase.STAYS))
    }

    // endregion

    // region Calendars

    @Test
    fun freeNightRanges_leaveOutBookedNights() {
        // Pine: stays 16–20 and 23–25, rival has the nights of 16 and 17.
        assertEquals(listOf(day(18)..day(19), day(23)..day(24)), BookingRules.freeNightRanges(lottery(), pine))
        // Nothing booked on Maple: every night of both stays.
        assertEquals(listOf(day(16)..day(19), day(23)..day(24)), BookingRules.freeNightRanges(lottery(), maple))
    }

    @Test
    fun bookedNightRanges_mergeBackToBackBookingsAndSkipOtherCampsites() {
        val l = lottery(extraBookings = listOf(Booking(me, pine, nights(18, 20)), Booking(Id(), maple, nights(23, 25))))
        assertEquals(listOf(day(16)..day(19)), BookingRules.bookedNightRanges(listOf(l), pine))
        assertEquals(listOf(day(23)..day(24)), BookingRules.bookedNightRanges(listOf(l), maple))
    }

    @Test
    fun bookedNightRanges_ignoreCancelledLotteries() {
        val cancelled = Lottery(
            ownerId = Id(),
            campsites = listOf(pine),
            entryStart = day(1).atStartOfDay(zone).toInstant().toKotlinInstant(),
            entryEnd = day(6).atStartOfDay(zone).toInstant().toKotlinInstant(),
            stayPeriods = listOf(nights(16, 20)),
            winnerCount = 1,
            winners = listOf(me),
            bookings = listOf(Booking(me, pine, nights(16, 18))),
            drawn = true,
            cancelled = true,
        )
        assertTrue(BookingRules.bookedNightRanges(listOf(cancelled), pine).isEmpty())
    }

    @Test
    fun nightRuns_sortsAndSplitsOnGaps() {
        assertEquals(
            listOf(day(1)..day(2), day(5)..day(5)),
            BookingRules.nightRuns(listOf(day(5), day(2), day(1), day(2))),
        )
    }

    // endregion
}
