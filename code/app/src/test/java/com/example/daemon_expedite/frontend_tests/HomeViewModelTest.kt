package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.frontend.FilterState
import com.example.daemon_expedite.frontend.HomeViewModel
import com.example.daemon_expedite.frontend.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * The search itself needs the database, so only what happens before a search finishes is
 * tested here. The start-up search is queued and never run.
 */
class HomeViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun filterDefaults_matchAnyCampsiteForOneGuest() {
        val filter = FilterState()
        assertEquals("", filter.query)
        assertEquals("", filter.location)
        assertNull(filter.startDate)
        assertNull(filter.endDate)
        assertEquals(1, filter.guestCount)
    }

    @Test
    fun startsLoadingWithEmptyQuery() {
        val vm = HomeViewModel()
        assertEquals("", vm.query.value)
        assertEquals(UiState.Loading, vm.campsites.value)
    }

    @Test
    fun onQueryChange_updatesImmediately() {
        // The search waits for typing to stop, but the text box must not.
        val vm = HomeViewModel()
        vm.onQueryChange("P")
        assertEquals("P", vm.query.value)
        vm.onQueryChange("Pine ")
        assertEquals("Pine ", vm.query.value)
    }

    @Test
    fun onFilterClick_doesNotChangeTheQuery() {
        val vm = HomeViewModel()
        vm.onQueryChange("Pine")
        vm.onFilterClick("Banff", LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18), 4)
        assertEquals("Pine", vm.query.value)
    }

    @Test
    fun setViewer_beforeAnyResultsLeavesLoading() {
        val vm = HomeViewModel()
        vm.setViewer(Id())
        vm.setViewer(null)
        assertEquals(UiState.Loading, vm.campsites.value)
    }
}
