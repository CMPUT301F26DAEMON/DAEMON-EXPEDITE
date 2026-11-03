package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.backend.LotteryRules
import com.example.daemon_expedite.backend.WaitingList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class LotteryTest {

    private val zone = ZoneId.of("America/Edmonton")

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)

    private fun period(fromMonth: Int, fromDay: Int, toMonth: Int, toDay: Int) =
        DatePeriod(day(fromMonth, fromDay), day(toMonth, toDay))

    /** Midnight at the start of [date] in [zone]. */
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()

    // region DatePeriod

    @Test
    fun datePeriod_countsNightsNotDays() {
        // Check in the 16th, check out the 18th: nights of the 16th and 17th.
        assertEquals(2, period(10, 16, 10, 18).nights)
        assertEquals(1, period(10, 31, 11, 1).nights)
    }

    @Test(expected = IllegalArgumentException::class)
    fun datePeriod_needsAtLeastOneNight() {
        period(10, 16, 10, 16)
    }

    @Test
    fun datePeriod_backToBackPeriodsDoNotOverlap() {
        val first = period(10, 16, 10, 18)
        assertFalse(first.overlaps(period(10, 18, 10, 20)))
        assertTrue(first.overlaps(period(10, 17, 10, 19)))
        assertTrue(first.overlaps(period(10, 10, 10, 30)))
    }

    @Test
    fun datePeriod_contains() {
        val stay = period(10, 16, 10, 20)
        assertTrue(stay.contains(period(10, 16, 10, 20)))
        assertTrue(stay.contains(period(10, 17, 10, 18)))
        assertFalse(stay.contains(period(10, 19, 10, 21)))
    }

    // endregion

    // region Phase

    /** Entries 1 to 5 Oct, stays the weekends of 16 Oct and 23 Oct. */
    private fun lottery(drawn: Boolean = false, cancelled: Boolean = false) = Lottery(
        ownerId = Id(),
        campsites = listOf(Id()),
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(period(10, 23, 10, 25), period(10, 16, 10, 18)),
        winnerCount = 2,
        drawn = drawn,
        cancelled = cancelled,
    )

    @Test
    fun lottery_sortsStayPeriods() {
        val lottery = lottery()
        assertEquals(day(10, 16), lottery.firstStayNight)
        assertEquals(day(10, 25), lottery.lastCheckOut)
    }

    @Test
    fun phase_followsTheTimeline() {
        val notDrawn = lottery()
        val drawn = lottery(drawn = true)
        assertEquals(LotteryPhase.UPCOMING, notDrawn.phase(midnight(day(9, 30)), zone))
        assertEquals(LotteryPhase.OPEN, notDrawn.phase(midnight(day(10, 1)), zone))
        assertEquals(LotteryPhase.OPEN, notDrawn.phase(midnight(day(10, 6)) - kotlin.time.Duration.parse("1s"), zone))
        assertEquals(LotteryPhase.DRAWING, notDrawn.phase(midnight(day(10, 6)), zone))
        assertEquals(LotteryPhase.BOOKING, drawn.phase(midnight(day(10, 6)), zone))
        assertEquals(LotteryPhase.STAYS, drawn.phase(midnight(day(10, 16)), zone))
        assertEquals(LotteryPhase.STAYS, drawn.phase(midnight(day(10, 20)), zone)) // between the weekends
        assertEquals(LotteryPhase.FINISHED, drawn.phase(midnight(day(10, 25)), zone))
    }

    @Test
    fun phase_cancelledWinsOverEverything() {
        assertEquals(LotteryPhase.CANCELLED, lottery(cancelled = true).phase(midnight(day(10, 2)), zone))
        assertTrue(LotteryPhase.CANCELLED.isPast)
        assertTrue(LotteryPhase.FINISHED.isPast)
        assertFalse(LotteryPhase.BOOKING.isPast)
    }

    @Test
    fun canCancel_onlyBeforeStaysBegin() {
        assertTrue(LotteryPhase.UPCOMING.canCancel)
        assertTrue(LotteryPhase.OPEN.canCancel)
        assertTrue(LotteryPhase.DRAWING.canCancel)
        assertTrue(LotteryPhase.BOOKING.canCancel)
        assertFalse(LotteryPhase.STAYS.canCancel)
        assertFalse(LotteryPhase.FINISHED.canCancel)
        assertFalse(LotteryPhase.CANCELLED.canCancel)
    }

    @Test
    fun waitlistCapacity_nullMeansNoLimit() {
        assertNull(lottery().waitlistCapacity)
        val limited = Lottery(
            ownerId = Id(), campsites = listOf(Id()),
            entryStart = midnight(day(10, 1)), entryEnd = midnight(day(10, 6)),
            stayPeriods = listOf(period(10, 16, 10, 18)), winnerCount = 1,
            waitingList = WaitingList(capacity = 10),
        )
        assertEquals(10, limited.waitlistCapacity)
    }

    // endregion

    // region Rules

    private val today = day(10, 1)

    private fun check(
        campsites: Int = 2,
        entryFirst: LocalDate? = day(10, 1),
        entryLast: LocalDate? = day(10, 5),
        stays: List<DatePeriod> = listOf(period(10, 16, 10, 18)),
        winners: Int? = 2,
        capacity: Int? = null,
    ) = LotteryRules.check(campsites, entryFirst, entryLast, stays, winners, capacity, today)

    @Test
    fun rules_validLotteryHasNoProblems() {
        assertTrue(check().isEmpty)
        assertTrue(check(entryFirst = day(10, 3), entryLast = day(10, 3)).isEmpty) // one-day entry, in the future
    }

    @Test
    fun rules_maxWinnersIsCampsitesTimesNights() {
        // 2 campsites × (2 + 3 nights) = 10
        assertEquals(10, LotteryRules.maxWinners(2, listOf(period(10, 16, 10, 18), period(10, 23, 10, 26))))
        assertTrue(check(winners = 4).isEmpty)
        assertNotNull(check(winners = 5).winnerCount)
        assertNotNull(check(winners = 0).winnerCount)
        assertNotNull(check(winners = null).winnerCount)
    }

    @Test
    fun rules_entryPeriod() {
        assertNotNull(check(entryFirst = null).entryPeriod)
        assertNotNull(check(entryFirst = day(9, 30)).entryPeriod) // in the past
        assertNotNull(check(entryFirst = day(10, 5), entryLast = day(10, 4)).entryPeriod) // backwards
        assertNotNull(check(entryLast = day(10, 16)).entryPeriod) // closes on the first stay night
        assertTrue(check(entryLast = day(10, 15)).isEmpty) // closes the day before
    }

    @Test
    fun rules_stayPeriods() {
        assertNotNull(check(stays = emptyList()).stayPeriods)
        assertNotNull(check(stays = listOf(period(10, 16, 10, 18), period(10, 17, 10, 19))).stayPeriods)
        assertTrue(check(stays = listOf(period(10, 23, 10, 25), period(10, 16, 10, 18))).isEmpty)
    }

    @Test
    fun rules_campsitesAndCapacity() {
        assertNotNull(check(campsites = 0).campsites)
        assertNotNull(check(winners = 3, capacity = 2).waitlistCapacity)
        assertTrue(check(winners = 3, capacity = 3).isEmpty)
    }

    @Test
    fun entryWindow_runsFromStartOfFirstDayToEndOfLastDay() {
        val (start, end) = LotteryRules.entryWindow(day(10, 1), day(10, 5), zone)
        assertEquals(midnight(day(10, 1)), start)
        assertEquals(midnight(day(10, 6)), end)
    }

    // endregion

    // region Edit rules

    private val pine = Id()
    private val maple = Id()

    /** Entries 1–5 Oct, stays 16–18 Oct, 2 winners, limit 10, 3 joined. */
    private fun editable() = Lottery(
        ownerId = Id(),
        campsites = listOf(pine),
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(period(10, 16, 10, 18)),
        winnerCount = 2,
        waitingList = WaitingList(capacity = 10, users = listOf(Id(), Id(), Id())),
    )

    private fun edit(
        phase: LotteryPhase = LotteryPhase.OPEN,
        campsites: Set<Id> = setOf(pine),
        first: LocalDate = day(10, 1),
        last: LocalDate = day(10, 5),
        stays: List<DatePeriod> = listOf(period(10, 16, 10, 18)),
        winners: Int = 2,
        capacity: Int? = 10,
        today: LocalDate = day(10, 3),
    ) = LotteryRules.checkEdit(editable(), phase, campsites, first, last, stays, winners, capacity, today, zone)

    @Test
    fun checkEdit_openLotteryUnchangedIsFine() {
        // The open date is in the past, which is fine once entries are open.
        assertTrue(edit().isEmpty)
    }

    @Test
    fun checkEdit_openLotteryCanGrow() {
        assertTrue(
            edit(
                campsites = setOf(pine, maple),
                last = day(10, 8),
                stays = listOf(period(10, 16, 10, 18), period(10, 23, 10, 25)),
                winners = 5,
                capacity = 20,
            ).isEmpty
        )
        assertTrue(edit(capacity = null).isEmpty) // removing the limit is growing too
    }

    @Test
    fun checkEdit_openLotteryCannotShrink() {
        assertNotNull(edit(campsites = setOf(maple)).campsites)
        assertNotNull(edit(first = day(10, 2)).entryPeriod)
        assertNotNull(edit(last = day(10, 4)).entryPeriod)
        assertNotNull(edit(stays = listOf(period(10, 23, 10, 25))).stayPeriods)
        assertNotNull(edit(winners = 1).winnerCount)
        assertNotNull(edit(capacity = 9).waitlistCapacity)
    }

    @Test
    fun checkEdit_capacityNeverBelowJoined() {
        // Upcoming lotteries can shrink, but not below who has joined (3 here).
        val upcoming = LotteryPhase.UPCOMING
        assertTrue(edit(upcoming, capacity = 3, today = day(9, 20)).isEmpty)
        assertNotNull(edit(upcoming, capacity = 2, winners = 2, today = day(9, 20)).waitlistCapacity)
    }

    @Test
    fun checkEdit_upcomingCanChangeAnything() {
        val problems = edit(
            LotteryPhase.UPCOMING,
            campsites = setOf(maple),
            first = day(9, 25),
            last = day(9, 30),
            stays = listOf(period(10, 10, 10, 11)),
            winners = 1,
            capacity = 5,
            today = day(9, 20),
        )
        assertTrue(problems.isEmpty)
    }

    @Test
    fun checkEdit_closedLotteryCannotBeEdited() {
        assertNotNull(edit(LotteryPhase.DRAWING).entryPeriod)
        assertNotNull(edit(LotteryPhase.CANCELLED).entryPeriod)
    }

    // endregion
}
