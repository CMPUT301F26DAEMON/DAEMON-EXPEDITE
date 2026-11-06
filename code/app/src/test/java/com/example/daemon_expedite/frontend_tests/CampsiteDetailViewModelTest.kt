package com.example.daemon_expedite.frontend_tests

import androidx.lifecycle.SavedStateHandle
import com.example.daemon_expedite.backend.Booking
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.backend.WaitingList
import com.example.daemon_expedite.frontend.CampsiteDetailViewModel
import com.example.daemon_expedite.frontend.Routes
import com.example.daemon_expedite.frontend.UiState
import com.example.daemon_expedite.frontend.campsiteIdArg
import com.example.daemon_expedite.frontend.entrantStatus
import com.example.daemon_expedite.frontend.loadAsUiState
import com.example.daemon_expedite.frontend.quietly
import com.example.daemon_expedite.frontend.toIdOrNull
import com.example.daemon_expedite.ui_components.LotteryStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class CampsiteDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val me = Id()
    private val viewer = User(id = me, notificationsEnabled = true)

    // region ViewModel guards

    @Test
    fun load_missingOrMalformedId_showsNotFound() {
        val noId = CampsiteDetailViewModel(SavedStateHandle())
        noId.load(viewer)
        assertEquals(UiState.Error("Couldn't find this campsite"), noId.details.value)

        val badId = CampsiteDetailViewModel(SavedStateHandle(mapOf(Routes.CAMPSITE_ID_ARG to "nope")))
        badId.load(viewer)
        assertEquals(UiState.Error("Couldn't find this campsite"), badId.details.value)
    }

    @Test
    fun retry_beforeAnyLoadDoesNothing() {
        val vm = CampsiteDetailViewModel(SavedStateHandle())
        vm.retry()
        assertEquals(UiState.Loading, vm.details.value)
    }

    @Test
    fun retry_afterNotFoundStaysNotFound() {
        val vm = CampsiteDetailViewModel(SavedStateHandle())
        vm.load(viewer)
        vm.retry()
        assertEquals(UiState.Error("Couldn't find this campsite"), vm.details.value)
    }

    @Test
    fun refresh_withoutIdLeavesStateAlone() {
        val vm = CampsiteDetailViewModel(SavedStateHandle())
        vm.refresh(viewer)
        assertEquals(UiState.Loading, vm.details.value)
    }

    // endregion

    // region Route ids

    @Test
    fun toIdOrNull_parsesIdsFromRoutes() {
        val id = Id()
        assertEquals(id, id.value.toString().toIdOrNull())
    }

    @Test
    fun toIdOrNull_rejectsMissingMalformedAndNonV7() {
        assertNull((null as String?).toIdOrNull())
        assertNull("".toIdOrNull())
        assertNull("campsite".toIdOrNull())
        // A well-formed v4 UUID parses, but Id only accepts v7.
        assertNull("550e8400-e29b-41d4-a716-446655440000".toIdOrNull())
    }

    @Test
    fun campsiteIdArg_readsTheCampsiteArgOnly() {
        val id = Id()
        assertEquals(id, SavedStateHandle(mapOf(Routes.CAMPSITE_ID_ARG to id.value.toString())).campsiteIdArg())
        assertNull(SavedStateHandle(mapOf(Routes.LOTTERY_ID_ARG to id.value.toString())).campsiteIdArg())
    }

    // endregion

    // region entrantStatus edge cases (the common cases are in EntrantStatusTest)

    private val zone = ZoneId.systemDefault()
    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()
    private val stay = DatePeriod(day(10, 16), day(10, 18))

    /** Entries 1–5 Oct, stays 16–18 Oct. */
    private fun lottery(
        users: List<Id> = listOf(me),
        winners: List<Id> = emptyList(),
        declined: List<Id> = emptyList(),
        bookings: List<Booking> = emptyList(),
        drawn: Boolean = false,
        cancelled: Boolean = false,
    ) = Lottery(
        ownerId = Id(),
        campsites = listOf(Id()),
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
    fun entrantStatus_entryBoundaries() {
        // Opens exactly at entryStart; closes exactly at entryEnd.
        assertEquals(LotteryStatus.JOINED, lottery().entrantStatus(me, midnight(day(10, 1))))
        assertEquals(LotteryStatus.DRAWING, lottery().entrantStatus(me, midnight(day(10, 6))))
    }

    @Test
    fun entrantStatus_cancelledHidesEverything() {
        assertEquals(LotteryStatus.NONE, lottery(cancelled = true).entrantStatus(me, midnight(day(10, 3))))
        assertEquals(
            LotteryStatus.NONE,
            lottery(winners = listOf(me), drawn = true, cancelled = true).entrantStatus(me, midnight(day(10, 10))),
        )
    }

    @Test
    fun entrantStatus_finishedShowsNothing() {
        val booked = lottery(winners = listOf(me), bookings = listOf(Booking(me, Id(), stay)), drawn = true)
        assertEquals(LotteryStatus.NONE, booked.entrantStatus(me, midnight(day(10, 18))))
    }

    @Test
    fun entrantStatus_duringStays() {
        val during = midnight(day(10, 17))
        // Declined stays declined; losers no longer see the card.
        assertEquals(
            LotteryStatus.DECLINED,
            lottery(winners = listOf(me), declined = listOf(me), drawn = true).entrantStatus(me, during),
        )
        assertEquals(LotteryStatus.NONE, lottery(winners = listOf(Id()), drawn = true).entrantStatus(me, during))
    }

    @Test
    fun entrantStatus_bookingDeadlineIsFirstNight() {
        val won = lottery(winners = listOf(me), drawn = true)
        val lastBookingMoment = Instant.fromEpochMilliseconds(midnight(day(10, 16)).toEpochMilliseconds() - 1)
        assertEquals(LotteryStatus.WON, won.entrantStatus(me, lastBookingMoment))
        assertEquals(LotteryStatus.MISSED, won.entrantStatus(me, midnight(day(10, 16))))
    }

    @Test
    fun entrantStatus_bookedBeatsDeclined() {
        val both = lottery(
            winners = listOf(me), declined = listOf(me), bookings = listOf(Booking(me, Id(), stay)), drawn = true,
        )
        assertEquals(LotteryStatus.BOOKED, both.entrantStatus(me, midnight(day(10, 10))))
    }

    @Test
    fun entrantStatus_fullListOnlyBlocksOthers() {
        val full = lottery(users = listOf(Id()))
        val capped = Lottery(
            ownerId = Id(), campsites = listOf(Id()),
            entryStart = midnight(day(10, 1)), entryEnd = midnight(day(10, 6)),
            stayPeriods = listOf(stay), winnerCount = 1,
            waitingList = WaitingList(capacity = 1, users = full.waitingList.users),
        )
        assertEquals(LotteryStatus.FULL, capped.entrantStatus(me, midnight(day(10, 3))))
        // Full lists don't matter once entries close for someone who never joined.
        assertEquals(LotteryStatus.NONE, capped.entrantStatus(me, midnight(day(10, 7))))
    }

    // endregion

    // region Loading helpers

    @Test
    fun loadAsUiState_wrapsResult() = runBlocking {
        assertEquals(UiState.Content(42), loadAsUiState("unused") { 42 })
    }

    @Test
    fun loadAsUiState_rethrowsCancellation() {
        assertThrows(CancellationException::class.java) {
            runBlocking { loadAsUiState<Int>("unused") { throw CancellationException("left the screen") } }
        }
    }

    @Test
    fun quietly_returnsResultAndRethrowsCancellation() {
        assertEquals("ok", runBlocking { quietly("unused") { "ok" } })
        assertThrows(CancellationException::class.java) {
            runBlocking { quietly<String>("unused") { throw CancellationException() } }
        }
    }

    // endregion
}
