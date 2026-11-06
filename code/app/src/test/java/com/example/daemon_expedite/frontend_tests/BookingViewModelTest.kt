package com.example.daemon_expedite.frontend_tests

import androidx.lifecycle.SavedStateHandle
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.frontend.BookingSelection
import com.example.daemon_expedite.frontend.BookingViewModel
import com.example.daemon_expedite.frontend.Routes
import com.example.daemon_expedite.frontend.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class BookingViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val me = Id()

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)

    /** A booking screen opened from a route. Its load is queued, never run. */
    private fun booking(lotteryId: String? = Id().value.toString(), campsiteId: String? = null) =
        BookingViewModel(
            SavedStateHandle(
                buildMap {
                    lotteryId?.let { put(Routes.LOTTERY_ID_ARG, it) }
                    campsiteId?.let { put(Routes.CAMPSITE_ID_ARG, it) }
                },
            ),
        )

    // region BookingSelection.period

    @Test
    fun period_nullUntilBothEndsPicked() {
        assertNull(BookingSelection().period)
        assertNull(BookingSelection(firstNight = day(10, 16)).period)
        assertNull(BookingSelection(checkOut = day(10, 18)).period)
    }

    @Test
    fun period_needsAtLeastOneNight() {
        // Same day or check-out first can't make a stay, and mustn't crash DatePeriod's check.
        assertNull(BookingSelection(firstNight = day(10, 16), checkOut = day(10, 16)).period)
        assertNull(BookingSelection(firstNight = day(10, 18), checkOut = day(10, 16)).period)
    }

    @Test
    fun period_oneNightAndLonger() {
        assertEquals(1, BookingSelection(firstNight = day(10, 16), checkOut = day(10, 17)).period!!.nights)
        assertEquals(2, BookingSelection(firstNight = day(10, 16), checkOut = day(10, 18)).period!!.nights)
    }

    // endregion

    @Test
    fun routeCampsite_isPickedFirst() {
        val campsite = Id()
        val form = booking(campsiteId = campsite.value.toString())
        assertEquals(campsite, form.selection.value.campsite)
        assertNull(form.selection.value.firstNight)
        assertFalse(form.selection.value.agreedToRules)
    }

    @Test
    fun malformedRouteCampsite_picksNothing() {
        assertNull(booking(campsiteId = "not-an-id").selection.value.campsite)
    }

    @Test
    fun missingLotteryId_showsErrorWithoutLoading() {
        assertEquals(UiState.Error("Couldn't find this lottery"), booking(lotteryId = null).options.value)
        assertEquals(UiState.Error("Couldn't find this lottery"), booking(lotteryId = "garbage").options.value)
    }

    @Test
    fun validLotteryId_startsLoading() {
        assertEquals(UiState.Loading, booking().options.value)
    }

    @Test
    fun selectCampsite_newCampsiteClearsDatesButKeepsAgreement() {
        val form = booking()
        form.selectCampsite(Id())
        form.onDatesChange(day(10, 16), day(10, 18))
        form.setAgreedToRules(true)

        val other = Id()
        form.selectCampsite(other)

        val selection = form.selection.value
        assertEquals(other, selection.campsite)
        assertNull(selection.firstNight)
        assertNull(selection.checkOut)
        assertTrue(selection.agreedToRules) // the rules are the same for every campsite
    }

    @Test
    fun selectCampsite_sameCampsiteKeepsDates() {
        val form = booking()
        val campsite = Id()
        form.selectCampsite(campsite)
        form.onDatesChange(day(10, 16), day(10, 18))
        form.selectCampsite(campsite)
        assertEquals(day(10, 16), form.selection.value.firstNight)
        assertEquals(day(10, 18), form.selection.value.checkOut)
    }

    @Test
    fun onDatesChange_canClearEitherEnd() {
        val form = booking()
        form.onDatesChange(day(10, 16), day(10, 18))
        form.onDatesChange(day(10, 16), null)
        assertEquals(day(10, 16), form.selection.value.firstNight)
        assertNull(form.selection.value.checkOut)
        assertNull(form.selection.value.period)
        form.onDatesChange(null, null)
        assertNull(form.selection.value.firstNight)
    }

    @Test
    fun setAgreedToRules_togglesBothWays() {
        val form = booking()
        form.setAgreedToRules(true)
        assertTrue(form.selection.value.agreedToRules)
        form.setAgreedToRules(false)
        assertFalse(form.selection.value.agreedToRules)
    }

    @Test
    fun problem_isNullUntilTheLotteryHasLoaded() {
        val form = booking()
        // Even with nothing picked, there's nothing to check against yet.
        assertNull(form.problem(me))
        form.onDatesChange(day(10, 16), day(10, 18))
        assertNull(form.problem(me))
    }

    @Test
    fun decline_withoutLotteryIdFailsStraightAway() {
        val form = booking(lotteryId = null)
        var result: String? = "not called"
        form.decline(me) { result = it }
        assertEquals("Couldn't find this lottery", result)
        assertFalse(form.isDeclining.value)
    }

    @Test
    fun decline_secondTapWhileDecliningIsIgnored() {
        val form = booking()
        var calls = 0
        form.decline(me) { calls++ } // queued; stays "declining"
        assertTrue(form.isDeclining.value)
        form.decline(me) { calls++ }
        assertEquals(0, calls)
    }
}
