package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.WaitingList
import com.example.daemon_expedite.frontend.OwnerLotteriesViewModel
import com.example.daemon_expedite.frontend.UiState
import com.example.daemon_expedite.frontend.pill
import com.example.daemon_expedite.frontend.plural
import com.example.daemon_expedite.frontend.shortDate
import com.example.daemon_expedite.frontend.toSummary
import com.example.daemon_expedite.ui_components.PillVariant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/** ViewModel guards and the edges of [toSummary] not covered by LotterySummaryTest. */
class OwnerLotteriesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun retry_beforeLoadDoesNothing() {
        val vm = OwnerLotteriesViewModel()
        vm.retry()
        assertEquals(UiState.Loading, vm.lotteries.value)
    }

    @Test
    fun load_showsLoadingUntilFetched() {
        val vm = OwnerLotteriesViewModel()
        vm.load(Owner(notificationsEnabled = true))
        assertEquals(UiState.Loading, vm.lotteries.value)
    }

    // region Formatting helpers

    @Test
    fun pill_everyPhase() {
        assertEquals("Upcoming" to PillVariant.Outlined, LotteryPhase.UPCOMING.pill())
        assertEquals("Open" to PillVariant.Highlight, LotteryPhase.OPEN.pill())
        assertEquals("Drawing" to PillVariant.Highlight, LotteryPhase.DRAWING.pill())
        assertEquals("Booking" to PillVariant.Highlight, LotteryPhase.BOOKING.pill())
        assertEquals("In progress" to PillVariant.Outlined, LotteryPhase.STAYS.pill())
        assertEquals("Finished" to PillVariant.Muted, LotteryPhase.FINISHED.pill())
        assertEquals("Cancelled" to PillVariant.Muted, LotteryPhase.CANCELLED.pill())
    }

    @Test
    fun plural_zeroOneMany() {
        assertEquals("0 nights", plural(0, "night"))
        assertEquals("1 night", plural(1, "night"))
        assertEquals("12 winners", plural(12, "winner"))
    }

    @Test
    fun shortDate_yearOnlyWhenDifferent() {
        assertEquals("16 Oct", LocalDate.of(2026, 10, 16).shortDate(2026))
        assertEquals("16 Oct 2027", LocalDate.of(2027, 10, 16).shortDate(2026))
        assertEquals("1 Jan 2025", LocalDate.of(2025, 1, 1).shortDate(2026))
    }

    // endregion

    // region toSummary

    private val zone = ZoneId.of("America/Edmonton")
    private val pine = Id()
    private val maple = Id()
    private val birch = Id()
    private val names = mapOf(pine to "Pine Creek", maple to "Maple", birch to "Birch")

    private fun day(month: Int, day: Int, year: Int = 2026) = LocalDate.of(year, month, day)
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()

    private fun lottery(
        campsites: List<Id> = listOf(pine),
        entryFirst: LocalDate = day(10, 1),
        entryLast: LocalDate = day(10, 5),
        stays: List<DatePeriod> = listOf(DatePeriod(day(10, 16), day(10, 18))),
        winnerCount: Int = 2,
        waitingList: WaitingList = WaitingList(capacity = 10),
        drawn: Boolean = false,
    ) = Lottery(
        ownerId = Id(),
        campsites = campsites,
        entryStart = midnight(entryFirst),
        entryEnd = midnight(entryLast.plusDays(1)),
        stayPeriods = stays,
        winnerCount = winnerCount,
        waitingList = waitingList,
        drawn = drawn,
    )

    @Test
    fun titleCountsEveryOtherKnownCampsite() {
        val summary = lottery(campsites = listOf(pine, maple, birch)).toSummary(names, midnight(day(10, 3)), zone)
        assertEquals("Pine Creek + 2 more", summary.title)
    }

    @Test
    fun singleOneNightPeriod_noPeriodCountAndSingularNight() {
        val summary = lottery(stays = listOf(DatePeriod(day(10, 16), day(10, 17))))
            .toSummary(names, midnight(day(10, 3)), zone)
        assertEquals("16 Oct – 17 Oct · 1 night", summary.staysText)
    }

    @Test
    fun noCapacityAndOneWinner() {
        val summary = lottery(winnerCount = 1, waitingList = WaitingList(users = listOf(Id(), Id())))
            .toSummary(names, midnight(day(10, 3)), zone)
        assertEquals("2 joined · 1 winner", summary.countsText)
    }

    @Test
    fun drawing_entriesShowAsClosed() {
        val summary = lottery().toSummary(names, midnight(day(10, 7)), zone)
        assertEquals("Drawing", summary.status)
        assertEquals("Entries closed 5 Oct", summary.entriesText)
    }

    @Test
    fun staysCrossingNewYear_showYearOnlyOnNextYearDate() {
        val summary = lottery(stays = listOf(DatePeriod(day(12, 30), day(1, 2, year = 2027))))
            .toSummary(names, midnight(day(10, 3)), zone)
        assertEquals("30 Dec – 2 Jan 2027 · 3 nights", summary.staysText)
    }

    @Test
    fun lastEntryDayIsRightAcrossDaylightSavingChange() {
        // Clocks go back on 1 Nov 2026 in Edmonton; entries still close at the end of 1 Nov.
        val summary = lottery(entryFirst = day(10, 28), entryLast = day(11, 1), stays = listOf(DatePeriod(day(11, 20), day(11, 22))))
            .toSummary(names, midnight(day(10, 30)), zone)
        assertEquals("Entries close 1 Nov", summary.entriesText)
    }

    @Test
    fun staysTextUsesFirstNightAndLastCheckOutEvenWithGaps() {
        val summary = lottery(
            stays = listOf(DatePeriod(day(10, 23), day(10, 25)), DatePeriod(day(10, 16), day(10, 17))),
        ).toSummary(names, midnight(day(10, 3)), zone)
        assertEquals("16 Oct – 25 Oct · 2 periods · 3 nights", summary.staysText)
    }

    // endregion
}
