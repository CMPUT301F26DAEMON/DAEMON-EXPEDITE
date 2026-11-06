package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Booking
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.backend.WaitingList
import com.example.daemon_expedite.frontend.EntrantLotteriesViewModel
import com.example.daemon_expedite.frontend.UiState
import com.example.daemon_expedite.frontend.isInProgressFor
import com.example.daemon_expedite.frontend.resultPill
import com.example.daemon_expedite.ui_components.PillVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/** ViewModel guards and edges of the tab helpers not covered by EntrantLotteriesTest. */
class EntrantLotteriesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun retry_beforeLoadDoesNothing() {
        val vm = EntrantLotteriesViewModel()
        vm.retry()
        assertEquals(UiState.Loading, vm.lotteries.value)
    }

    @Test
    fun removeFromInProgress_whileLoadingIsIgnored() {
        val vm = EntrantLotteriesViewModel()
        vm.removeFromInProgress(Id())
        assertEquals(UiState.Loading, vm.lotteries.value)
    }

    @Test
    fun load_showsLoadingUntilFetched() {
        val vm = EntrantLotteriesViewModel()
        vm.load(User(notificationsEnabled = true))
        assertEquals(UiState.Loading, vm.lotteries.value)
    }

    // region Tab helpers

    private val zone = ZoneId.of("America/Edmonton")
    private val me = Id()
    private val campsite = Id()
    private val stay = DatePeriod(day(10, 16), day(10, 18))

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()

    /** Entries 1–5 Oct; stay 16–18 Oct. */
    private fun lottery(
        users: List<Id> = listOf(me),
        winners: List<Id> = emptyList(),
        declined: List<Id> = emptyList(),
        bookings: List<Booking> = emptyList(),
        drawn: Boolean = false,
        cancelled: Boolean = false,
    ) = Lottery(
        ownerId = Id(),
        campsites = listOf(campsite),
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(stay),
        winnerCount = 1,
        waitingList = WaitingList(users = users),
        winners = winners,
        bookings = bookings,
        declined = declined,
        drawn = drawn,
        cancelled = cancelled,
    )

    @Test
    fun inProgress_notBeforeEntriesOpen() {
        assertFalse(lottery().isInProgressFor(me, midnight(day(9, 30)), zone))
    }

    @Test
    fun inProgress_fromEntryStartUntilTheDraw() {
        assertTrue(lottery().isInProgressFor(me, midnight(day(10, 1)), zone))
        assertTrue(lottery().isInProgressFor(me, midnight(day(10, 6)), zone)) // closed, waiting for draw
        // A late draw keeps it in progress, even into the stay dates.
        assertTrue(lottery().isInProgressFor(me, midnight(day(10, 17)), zone))
    }

    @Test
    fun results_notDrawnHasNoPill() {
        assertNull(lottery().resultPill(me, midnight(day(10, 7)), zone))
    }

    @Test
    fun results_lostStaysLostAfterTheStays() {
        val lost = lottery(users = listOf(me, Id()), winners = listOf(Id()), drawn = true)
        assertEquals("Lost" to PillVariant.Danger, lost.resultPill(me, midnight(day(10, 20)), zone))
    }

    @Test
    fun results_bookedStaysBookedAfterTheStays() {
        val booked = lottery(winners = listOf(me), bookings = listOf(Booking(me, campsite, stay)), drawn = true)
        assertEquals("Won · Booked" to PillVariant.Outlined, booked.resultPill(me, midnight(day(10, 20)), zone))
    }

    @Test
    fun results_bookingIsPendingUntilJustBeforeTheFirstNight() {
        val won = lottery(winners = listOf(me), drawn = true)
        val lastMoment = Instant.fromEpochMilliseconds(midnight(day(10, 16)).toEpochMilliseconds() - 1)
        assertEquals("Won · Action Pending" to PillVariant.Highlight, won.resultPill(me, lastMoment, zone))
        assertEquals("Won · Not Accepted" to PillVariant.Muted, won.resultPill(me, midnight(day(10, 16)), zone))
    }

    @Test
    fun results_cancelledAfterDrawHidesLosersToo() {
        val cancelled = lottery(users = listOf(me, Id()), winners = listOf(Id()), drawn = true, cancelled = true)
        assertNull(cancelled.resultPill(me, midnight(day(10, 10)), zone))
    }

    // endregion
}
