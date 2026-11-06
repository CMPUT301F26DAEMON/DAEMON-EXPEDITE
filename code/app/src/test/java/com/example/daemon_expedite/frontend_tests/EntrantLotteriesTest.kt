package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Booking
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.WaitingList
import com.example.daemon_expedite.frontend.isInProgressFor
import com.example.daemon_expedite.frontend.resultPill
import com.example.daemon_expedite.ui_components.PillVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class EntrantLotteriesTest {

    private val zone = ZoneId.of("America/Edmonton")
    private val me = Id()
    private val campsite = Id()

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()

    private val duringEntries = midnight(day(10, 3))
    private val duringBooking = midnight(day(10, 10))
    private val duringStays = midnight(day(10, 17))

    /** Entries 1–5 Oct; stay 16–18 Oct. [me] is on the waiting list. */
    private fun lottery(
        drawn: Boolean = false,
        winners: List<Id> = emptyList(),
        declined: List<Id> = emptyList(),
        bookings: List<Booking> = emptyList(),
        cancelled: Boolean = false,
        users: List<Id> = listOf(me, Id()),
    ) = Lottery(
        ownerId = Id(),
        campsites = listOf(campsite),
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(DatePeriod(day(10, 16), day(10, 18))),
        winnerCount = 1,
        waitingList = WaitingList(capacity = 10, users = users),
        winners = winners,
        bookings = bookings,
        declined = declined,
        drawn = drawn,
        cancelled = cancelled,
    )

    @Test
    fun inProgressWhileOpenOrWaitingForDraw() {
        assertTrue(lottery().isInProgressFor(me, duringEntries, zone))
        assertTrue(lottery(drawn = false).isInProgressFor(me, duringBooking, zone)) // entries closed, not drawn
        assertNull(lottery().resultPill(me, duringEntries, zone))
    }

    @Test
    fun notInProgressOnceDrawnOrIfNotJoined() {
        assertFalse(lottery(drawn = true).isInProgressFor(me, duringBooking, zone))
        assertFalse(lottery(users = listOf(Id())).isInProgressFor(me, duringEntries, zone))
        assertFalse(lottery(cancelled = true).isInProgressFor(me, duringEntries, zone))
    }

    @Test
    fun lost() {
        assertEquals("Lost" to PillVariant.Danger, lottery(drawn = true, winners = listOf(Id())).resultPill(me, duringBooking, zone))
    }

    @Test
    fun wonStates() {
        assertEquals(
            "Won · Action Pending" to PillVariant.Highlight,
            lottery(drawn = true, winners = listOf(me)).resultPill(me, duringBooking, zone),
        )
        assertEquals(
            "Won · Not Accepted" to PillVariant.Muted,
            lottery(drawn = true, winners = listOf(me), declined = listOf(me)).resultPill(me, duringBooking, zone),
        )
        val booking = Booking(me, campsite, DatePeriod(day(10, 16), day(10, 18)))
        assertEquals(
            "Won · Booked" to PillVariant.Outlined,
            lottery(drawn = true, winners = listOf(me), bookings = listOf(booking)).resultPill(me, duringStays, zone),
        )
    }

    @Test
    fun missedDeadlineIsNotAccepted() {
        assertEquals(
            "Won · Not Accepted" to PillVariant.Muted,
            lottery(drawn = true, winners = listOf(me)).resultPill(me, duringStays, zone),
        )
    }

    @Test
    fun cancelledOrNotEnteredIsHidden() {
        assertNull(lottery(drawn = true, winners = listOf(me), cancelled = true).resultPill(me, duringBooking, zone))
        assertNull(lottery(drawn = true, users = listOf(Id())).resultPill(me, duringBooking, zone))
    }
}
