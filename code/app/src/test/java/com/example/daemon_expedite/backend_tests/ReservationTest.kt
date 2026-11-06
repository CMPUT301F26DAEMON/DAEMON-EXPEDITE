package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Reservation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Unit tests for [Reservation]. It has no database calls, so nothing is mocked. */
class ReservationTest {

    private fun day(d: Int) = LocalDate.of(2026, 10, d)

    /** Nights of 16 and 17 Oct, check out on the 18th. */
    private fun reservation(cancelled: Boolean = false) = Reservation(
        campsite = Id(),
        period = DatePeriod(day(16), day(18)),
        reservedBy = Id(),
        isCancelled = cancelled,
    )

    @Test
    fun newReservation_isActiveAndNotFromALottery() {
        val reservation = reservation()
        assertFalse(reservation.isCancelled)
        assertNull(reservation.lotteryId)
    }

    @Test
    fun cancel_activeReservationBecomesCancelled() {
        val reservation = reservation()
        reservation.cancel()
        assertTrue(reservation.isCancelled)
    }

    @Test
    fun cancel_alreadyCancelledStaysCancelled() {
        val reservation = reservation(cancelled = true)
        reservation.cancel()
        assertTrue(reservation.isCancelled)
    }

    @Test
    fun isPast_beforeAndDuringTheStayIsNotPast() {
        assertFalse(reservation().isPast(today = day(10))) // well before
        assertFalse(reservation().isPast(today = day(16))) // first night
        assertFalse(reservation().isPast(today = day(17))) // last night
    }

    @Test
    fun isPast_fromCheckOutDayOnIsPast() {
        assertTrue(reservation().isPast(today = day(18))) // check-out day (boundary)
        assertTrue(reservation().isPast(today = day(25))) // well after
    }
}
