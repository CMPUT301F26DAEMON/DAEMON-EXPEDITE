// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.WaitingList
import com.example.daemon_expedite.frontend.toLotteryDetails
import com.example.daemon_expedite.frontend.toSummary
import com.example.daemon_expedite.ui_components.PillVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class LotterySummaryTest {

    private val zone = ZoneId.of("America/Edmonton")
    private val pine = Id()
    private val maple = Id()
    private val names = mapOf(pine to "Pine Creek", maple to "Maple")

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()

    /** Entries 1–5 Oct; stays 16–18 Oct and 23–25 Oct; 3 of 10 joined; 2 winners. */
    private fun lottery(campsites: List<Id> = listOf(pine, maple), cancelled: Boolean = false) = Lottery(
        ownerId = Id(),
        campsites = campsites,
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(DatePeriod(day(10, 16), day(10, 18)), DatePeriod(day(10, 23), day(10, 25))),
        winnerCount = 2,
        waitingList = WaitingList(capacity = 10, users = listOf(Id(), Id(), Id())),
        cancelled = cancelled,
    )

    @Test
    fun openLottery() {
        val summary = lottery().toSummary(names, midnight(day(10, 3)), zone)
        assertEquals("Pine Creek + 1 more", summary.title)
        assertEquals("Open", summary.status)
        assertEquals(PillVariant.Highlight, summary.statusVariant)
        assertEquals("Entries close 5 Oct", summary.entriesText)
        assertEquals("16 Oct – 25 Oct · 2 periods · 4 nights", summary.staysText)
        assertEquals("3/10 joined · 2 winners", summary.countsText)
    }

    @Test
    fun upcomingAndClosedEntryText() {
        assertEquals("Entries open 1 Oct", lottery().toSummary(names, midnight(day(9, 20)), zone).entriesText)
        assertEquals("Entries closed 5 Oct", lottery().toSummary(names, midnight(day(10, 7)), zone).entriesText)
    }

    @Test
    fun cancelledIsMuted() {
        val summary = lottery(cancelled = true).toSummary(names, midnight(day(10, 3)), zone)
        assertEquals("Cancelled", summary.status)
        assertEquals(PillVariant.Muted, summary.statusVariant)
    }

    @Test
    fun titleSkipsUnknownCampsites() {
        assertEquals("Pine Creek", lottery(listOf(pine, Id())).toSummary(names, midnight(day(10, 3)), zone).title)
        assertEquals("Deleted campsite", lottery(listOf(Id())).toSummary(names, midnight(day(10, 3)), zone).title)
    }

    @Test
    fun yearShownOnlyWhenNotThisYear() {
        val nextYearNow = midnight(LocalDate.of(2025, 12, 1))
        assertEquals("Entries open 1 Oct 2026", lottery().toSummary(names, nextYearNow, zone).entriesText)
    }

    // region Details page

    @Test
    fun details_openLottery() {
        val details = lottery().toLotteryDetails(names, midnight(day(10, 3)), zone)
        assertEquals(listOf("Pine Creek", "Maple"), details.campsites.map { it.name })
        assertEquals("Opens 1 Oct · Closes end of 5 Oct", details.entryText)
        assertEquals("Entrants can join now. Winners are drawn at the end of 5 Oct.", details.phaseNote)
        assertEquals(listOf("16 Oct → 18 Oct · 2 nights", "23 Oct → 25 Oct · 2 nights"), details.stayTexts)
        assertEquals("3/10 joined · 2 winners to draw", details.drawText)
        assertTrue(details.canEdit)
        assertTrue(details.canCancel)
        assertFalse(details.deletesOnCancel)
    }

    @Test
    fun details_upcomingDeletesOnCancel() {
        val details = lottery().toLotteryDetails(names, midnight(day(9, 20)), zone)
        assertTrue(details.deletesOnCancel)
        assertTrue(details.canEdit)
    }

    @Test
    fun details_afterEntriesCloseCannotEdit() {
        val details = lottery().toLotteryDetails(names, midnight(day(10, 7)), zone)
        assertFalse(details.canEdit)
        assertTrue(details.canCancel) // drawing: still before stays
    }

    @Test
    fun details_deletedCampsiteIsNamed() {
        val details = lottery(listOf(pine, Id())).toLotteryDetails(names, midnight(day(10, 3)), zone)
        assertEquals(listOf("Pine Creek", "Deleted campsite"), details.campsites.map { it.name })
    }

    // endregion
}
