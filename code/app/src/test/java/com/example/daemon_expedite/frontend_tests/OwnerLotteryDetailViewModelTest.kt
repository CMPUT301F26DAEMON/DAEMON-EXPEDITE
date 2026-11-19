package com.example.daemon_expedite.frontend_tests

import androidx.lifecycle.SavedStateHandle
import com.example.daemon_expedite.backend.Booking
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.WaitingList
import com.example.daemon_expedite.frontend.CancelResult
import com.example.daemon_expedite.frontend.OwnerLotteryDetailViewModel
import com.example.daemon_expedite.frontend.Routes
import com.example.daemon_expedite.frontend.UiState
import com.example.daemon_expedite.frontend.toLotteryDetails
import com.example.daemon_expedite.ui_components.PillVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/** ViewModel guards and the phases of [toLotteryDetails] not covered by LotterySummaryTest. */
class OwnerLotteryDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    // region ViewModel guards

    private fun withRoute(lotteryId: String?) = OwnerLotteryDetailViewModel(
        SavedStateHandle(lotteryId?.let { mapOf(Routes.LOTTERY_ID_ARG to it) } ?: emptyMap()),
    )

    @Test
    fun load_missingOrMalformedId_showsNotFound() {
        val noId = withRoute(null)
        noId.load()
        assertEquals(UiState.Error("Couldn't find this lottery"), noId.details.value)

        val badId = withRoute("lottery-1")
        badId.load()
        assertEquals(UiState.Error("Couldn't find this lottery"), badId.details.value)
    }

    @Test
    fun cancel_withoutIdFails() {
        val vm = withRoute(null)
        var result: CancelResult? = null
        vm.cancel { result = it }
        assertEquals(CancelResult.FAILED, result)
        assertFalse(vm.isCancelling.value)
    }

    @Test
    fun cancel_secondTapWhileCancellingIsIgnored() {
        val vm = withRoute(Id().value.toString())
        val results = mutableListOf<CancelResult>()
        vm.cancel { results += it } // queued, so still cancelling
        assertTrue(vm.isCancelling.value)
        vm.cancel { results += it }
        assertTrue(results.isEmpty())
    }

    // endregion

    // region toLotteryDetails

    private val zone = ZoneId.of("America/Edmonton")
    private val pine = Id()
    private val names = mapOf(pine to "Pine Creek")

    private fun day(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()

    /** Entries 1–5 Oct; stays 16–18 Oct; 3 of 10 joined; 2 winners. */
    private fun lottery(
        drawn: Boolean = false,
        winners: List<Id> = emptyList(),
        bookings: List<Booking> = emptyList(),
        cancelled: Boolean = false,
        winnerCount: Int = 2,
        waitingList: WaitingList = WaitingList(capacity = 10, users = listOf(Id(), Id(), Id())),
    ) = Lottery(
        ownerId = Id(),
        campsites = listOf(pine),
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(DatePeriod(day(10, 16), day(10, 18))),
        winnerCount = winnerCount,
        waitingList = waitingList,
        winners = winners,
        bookings = bookings,
        drawn = drawn,
        cancelled = cancelled,
    )

    private fun Lottery.at(now: Instant) = toLotteryDetails(names, now, zone)

    @Test
    fun upcoming_note() {
        assertEquals(
            "Entrants can join from 1 Oct. Winners are drawn when entries close.",
            lottery().at(midnight(day(9, 20))).phaseNote,
        )
    }

    @Test
    fun editableExactlyFromEntryStartUntilEntryEnd() {
        val atOpen = lottery().at(midnight(day(10, 1)))
        assertTrue(atOpen.canEdit)
        assertFalse(atOpen.deletesOnCancel) // open now, so cancelling keeps it

        val atClose = lottery().at(midnight(day(10, 6)))
        assertFalse(atClose.canEdit)
        assertEquals("Drawing", atClose.status)
    }

    @Test
    fun drawing() {
        val details = lottery().at(midnight(day(10, 7)))
        assertEquals("Entries are closed. Winners are being drawn.", details.phaseNote)
        assertEquals("3/10 joined · 2 winners to draw", details.drawText)
        assertEquals(PillVariant.Highlight, details.statusVariant)
        assertTrue(details.canCancel)
        assertFalse(details.canEdit)
        assertFalse(details.deletesOnCancel)
    }

    @Test
    fun booking_showsDrawnAndBookedCounts() {
        val winner = Id()
        val details = lottery(
            drawn = true,
            winners = listOf(winner, Id()),
            bookings = listOf(Booking(winner, pine, DatePeriod(day(10, 16), day(10, 18)))),
        ).at(midnight(day(10, 10)))
        assertEquals("Booking", details.status)
        assertEquals("Winners are drawn. They can book until the first night, 16 Oct.", details.phaseNote)
        assertEquals("3/10 joined · 2 of 2 winners drawn · 1 booked", details.drawText)
        assertTrue(details.canCancel)
        assertFalse(details.canEdit)
    }

    @Test
    fun stays_cannotCancel() {
        val details = lottery(drawn = true).at(midnight(day(10, 16)))
        assertEquals("In progress", details.status)
        assertEquals(PillVariant.Outlined, details.statusVariant)
        assertEquals("Stays are under way.", details.phaseNote)
        assertFalse(details.canCancel)
        assertFalse(details.canEdit)
    }

    @Test
    fun finished_onLastCheckOutDay() {
        val details = lottery(drawn = true).at(midnight(day(10, 18)))
        assertEquals("Finished", details.status)
        assertEquals(PillVariant.Muted, details.statusVariant)
        assertEquals("This lottery is over.", details.phaseNote)
        assertFalse(details.canCancel)
    }

    @Test
    fun cancelled_duringEntriesIsLocked() {
        val details = lottery(cancelled = true).at(midnight(day(10, 3)))
        assertEquals("Cancelled", details.status)
        assertEquals("You cancelled this lottery.", details.phaseNote)
        assertFalse(details.canEdit)
        assertFalse(details.canCancel)
        assertFalse(details.deletesOnCancel)
    }

    @Test
    fun noCapacityAndOneWinner() {
        val details = lottery(winnerCount = 1, waitingList = WaitingList()).at(midnight(day(10, 3)))
        assertEquals("0 joined · 1 winner to draw", details.drawText)
    }

    @Test
    fun otherYearDatesShowTheYear() {
        val details = lottery().at(midnight(day(12, 1, year = 2025)))
        assertEquals("Opens 1 Oct 2026 · Closes end of 5 Oct 2026", details.entryText)
        assertEquals(listOf("16 Oct 2026 → 18 Oct 2026 · 2 nights"), details.stayTexts)
    }

    @Test
    fun oneNightStayIsSingular() {
        val oneNight = Lottery(
            ownerId = Id(), campsites = listOf(pine),
            entryStart = midnight(day(10, 1)), entryEnd = midnight(day(10, 6)),
            stayPeriods = listOf(DatePeriod(day(10, 16), day(10, 17))), winnerCount = 1,
        )
        assertEquals(listOf("16 Oct → 17 Oct · 1 night"), oneNight.at(midnight(day(10, 3))).stayTexts)
    }

    // endregion
}
