// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.frontend.CreateLotteryViewModel
import com.example.daemon_expedite.frontend.SubmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CreateLotteryViewModelTest {

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)

    @Test
    fun toggleCampsite_addsThenRemoves() {
        val form = CreateLotteryViewModel()
        val id = Id()
        form.toggleCampsite(id)
        assertEquals(setOf(id), form.draft.value.campsites)
        form.toggleCampsite(id)
        assertTrue(form.draft.value.campsites.isEmpty())
    }

    @Test
    fun openDateAfterCloseDate_clearsCloseDate() {
        val form = CreateLotteryViewModel()
        form.setEntryFirstDay(day(10, 1))
        form.setEntryLastDay(day(10, 5))
        form.setEntryFirstDay(day(10, 3))
        assertEquals(day(10, 5), form.draft.value.entryLastDay) // still after: kept
        form.setEntryFirstDay(day(10, 8))
        assertNull(form.draft.value.entryLastDay) // now before: cleared
    }

    @Test
    fun stayPeriods_keptSortedAndRemovable() {
        val form = CreateLotteryViewModel()
        form.addStayPeriod(day(10, 23), day(10, 25))
        form.addStayPeriod(day(10, 16), day(10, 18))
        assertEquals(listOf(day(10, 16), day(10, 23)), form.draft.value.stayPeriods.map { it.start })
        form.removeStayPeriod(0)
        assertEquals(listOf(day(10, 23)), form.draft.value.stayPeriods.map { it.start })
    }

    @Test
    fun numberFields_keepOnlyDigits() {
        val form = CreateLotteryViewModel()
        form.onWinnerCountChange("1a2.5")
        form.onWaitlistCapacityChange("-40")
        assertEquals("125", form.draft.value.winnerCount)
        assertEquals("40", form.draft.value.waitlistCapacity)
    }

    @Test
    fun maxWinners_isCampsitesTimesNights() {
        val form = CreateLotteryViewModel()
        form.toggleCampsite(Id())
        form.toggleCampsite(Id())
        form.addStayPeriod(day(10, 16), day(10, 18))
        assertEquals(4, form.draft.value.maxWinners)
    }

    @Test
    fun submit_withEmptyForm_isInvalidAndShowsProblems() {
        val form = CreateLotteryViewModel()
        var result: SubmitResult? = null
        form.submit(Owner(notificationsEnabled = true)) { result = it }
        assertEquals(SubmitResult.INVALID, result)
        val problems = form.problems.value
        assertNotNull(problems.campsites)
        assertNotNull(problems.entryPeriod)
        assertNotNull(problems.stayPeriods)
        assertNotNull(problems.winnerCount)
        assertNull(problems.waitlistCapacity) // empty means no limit
    }

    @Test
    fun problems_updateAfterFirstSubmit() {
        val form = CreateLotteryViewModel()
        form.submit(Owner(notificationsEnabled = true)) {}
        form.toggleCampsite(Id())
        assertNull(form.problems.value.campsites)
    }
}
