package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.frontend.EntrantReservationsViewModel
import com.example.daemon_expedite.frontend.ReservationStatus
import com.example.daemon_expedite.frontend.UiState
import com.example.daemon_expedite.frontend.datesText
import com.example.daemon_expedite.frontend.groupReservations
import com.example.daemon_expedite.frontend.status
import com.example.daemon_expedite.frontend.unavailableCampsite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/** ViewModel guards and edges of the tab helpers not covered by EntrantReservationsTest. */
class EntrantReservationsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val cancelFailed = "Couldn't cancel the reservation. Check your connection and try again."

    // region ViewModel guards

    @Test
    fun startsLoadingWithNothingCancelling() {
        val vm = EntrantReservationsViewModel()
        assertEquals(UiState.Loading, vm.reservations.value)
        assertNull(vm.cancellingId.value)
    }

    @Test
    fun retry_beforeLoadDoesNothing() {
        val vm = EntrantReservationsViewModel()
        vm.retry()
        assertEquals(UiState.Loading, vm.reservations.value)
    }

    @Test
    fun cancel_beforeLoadFails() {
        val vm = EntrantReservationsViewModel()
        var result: String? = null
        vm.cancel(Id()) { result = it }
        assertEquals(cancelFailed, result)
        assertNull(vm.cancellingId.value)
    }

    @Test
    fun cancel_reservationNotOnScreenFails() {
        // The user is known but the reservation isn't one of theirs that loaded.
        val vm = EntrantReservationsViewModel()
        vm.load(User(notificationsEnabled = true, reservations = listOf(Id())))
        var result: String? = null
        vm.cancel(Id()) { result = it }
        assertEquals(cancelFailed, result)
        assertNull(vm.cancellingId.value)
    }

    // endregion

    // region Tab helpers

    private val zone = ZoneId.of("America/Edmonton")
    private val pine = Id()

    private fun day(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)
    private fun noon(date: LocalDate): Instant = date.atTime(12, 0).atZone(zone).toInstant().toKotlinInstant()

    private fun reservation(from: LocalDate, to: LocalDate, cancelled: Boolean = false) = Reservation(
        campsite = pine,
        period = DatePeriod(from, to),
        reservedBy = Id(),
        isCancelled = cancelled,
    )

    @Test
    fun status_pastOnCheckOutDay() {
        val now = noon(day(10, 3))
        assertEquals(ReservationStatus.PAST, reservation(day(10, 1), day(10, 3)).status(now, zone))
        assertEquals(ReservationStatus.ACTIVE, reservation(day(10, 1), day(10, 4)).status(now, zone))
    }

    @Test
    fun status_usesTheLocalDateNotUtc() {
        // 03:00 UTC on 3 Oct is still the evening of 2 Oct in Edmonton.
        val now = day(10, 3).atTime(3, 0).toInstant(ZoneOffset.UTC).toKotlinInstant()
        val endsOct3 = reservation(day(10, 1), day(10, 3))
        assertEquals(ReservationStatus.ACTIVE, endsOct3.status(now, zone))
        assertEquals(ReservationStatus.PAST, endsOct3.status(now, ZoneOffset.UTC))
    }

    @Test
    fun group_emptyGivesEmptyTabs() {
        val groups = groupReservations(emptyList(), emptyMap(), noon(day(10, 3)), zone)
        assertTrue(groups.active.isEmpty())
        assertTrue(groups.past.isEmpty())
        assertTrue(groups.cancelled.isEmpty())
    }

    @Test
    fun group_cancelledMostRecentFirst() {
        val older = reservation(day(9, 1), day(9, 3), cancelled = true)
        val newer = reservation(day(11, 1), day(11, 3), cancelled = true)
        val groups = groupReservations(listOf(older, newer), emptyMap(), noon(day(10, 3)), zone)
        assertEquals(listOf(newer.id, older.id), groups.cancelled.map { it.id })
        assertTrue(groups.active.isEmpty()) // a future cancelled stay isn't active
    }

    @Test
    fun unavailableCampsite_keepsIdSoTheCardStillWorks() {
        val id = Id()
        val card = unavailableCampsite(id)
        assertEquals(id, card.id)
        assertEquals("Campsite unavailable", card.name)
        assertNull(card.rating)
        assertNull(card.imageUrl)
    }

    @Test
    fun datesText_showsYearOnlyOnTheDateNotInThisYear() {
        val now = noon(day(1, 1))
        assertEquals("30 Dec 2025 – 2 Jan", reservation(day(12, 30, year = 2025), day(1, 2)).datesText(now, zone))
    }

    // endregion
}
