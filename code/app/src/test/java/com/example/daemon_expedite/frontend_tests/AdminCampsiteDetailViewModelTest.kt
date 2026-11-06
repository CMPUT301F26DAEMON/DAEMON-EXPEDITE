package com.example.daemon_expedite.frontend_tests

import androidx.lifecycle.SavedStateHandle
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.frontend.AdminCampsiteDetailViewModel
import com.example.daemon_expedite.frontend.Routes
import com.example.daemon_expedite.frontend.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AdminCampsiteDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private fun withRoute(campsiteId: String?) = AdminCampsiteDetailViewModel(
        SavedStateHandle(campsiteId?.let { mapOf(Routes.CAMPSITE_ID_ARG to it) } ?: emptyMap()),
    )

    @Test
    fun startsLoadingWithNothingKnown() {
        val vm = withRoute(Id().value.toString())
        assertEquals(UiState.Loading, vm.details.value)
        assertNull(vm.ownerId)
        assertFalse(vm.isDeleting.value)
    }

    @Test
    fun load_missingOrMalformedId_showsNotFound() {
        val noId = withRoute(null)
        noId.load()
        assertEquals(UiState.Error("Couldn't find this campsite"), noId.details.value)

        val badId = withRoute("12345")
        badId.load()
        assertEquals(UiState.Error("Couldn't find this campsite"), badId.details.value)
    }

    @Test
    fun deleteCampsite_beforeLoadFails() {
        val vm = withRoute(Id().value.toString())
        var result: Boolean? = null
        vm.deleteCampsite { result = it }
        assertEquals(false, result)
        assertFalse(vm.isDeleting.value)
    }

    @Test
    fun deleteOwner_beforeLoadFails() {
        // The owner is only known once the campsite has loaded.
        val vm = withRoute(Id().value.toString())
        var result: Boolean? = null
        vm.deleteOwner { result = it }
        assertEquals(false, result)
        assertFalse(vm.isDeleting.value)
    }

    @Test
    fun deletes_failAfterNotFound() {
        val vm = withRoute(null)
        vm.load()
        val results = mutableListOf<Boolean>()
        vm.deleteCampsite { results += it }
        vm.deleteOwner { results += it }
        assertEquals(listOf(false, false), results)
        assertNull(vm.ownerId)
    }
}
