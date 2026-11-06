package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.frontend.OwnerViewModel
import com.example.daemon_expedite.frontend.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OwnerViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun startsLoadingWithEmptySearch() {
        val vm = OwnerViewModel()
        assertEquals(UiState.Loading, vm.owner.value)
        assertEquals(UiState.Loading, vm.campsites.value)
        assertEquals("", vm.query.value)
    }

    @Test
    fun onQueryChange_keepsTextAsTyped() {
        val vm = OwnerViewModel()
        vm.onQueryChange("  Pine ")
        assertEquals("  Pine ", vm.query.value)
        vm.onQueryChange("")
        assertEquals("", vm.query.value)
    }

    @Test
    fun refreshCampsites_withoutOwnerShowsError() {
        // No owner has loaded, so there's no campsite list to fetch.
        val vm = OwnerViewModel()
        vm.refreshCampsites()
        main.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(UiState.Error("Couldn't load your campsites"), vm.campsites.value)
        assertEquals(UiState.Loading, vm.owner.value)
    }

    @Test
    fun deleteOwnerProfile_beforeLoadFailsAndKeepsSearch() {
        val vm = OwnerViewModel()
        vm.onQueryChange("Pine")
        var result: Boolean? = null
        vm.deleteOwnerProfile { result = it }
        assertEquals(false, result)
        assertEquals("Pine", vm.query.value)
    }
}
