package com.example.daemon_expedite.frontend_tests

import androidx.lifecycle.SavedStateHandle
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.frontend.DeleteResult
import com.example.daemon_expedite.frontend.OwnerCampsiteDetailViewModel
import com.example.daemon_expedite.frontend.Routes
import com.example.daemon_expedite.frontend.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class OwnerCampsiteDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val owner = Owner(notificationsEnabled = true)

    private fun withRoute(campsiteId: String?) = OwnerCampsiteDetailViewModel(
        SavedStateHandle(campsiteId?.let { mapOf(Routes.CAMPSITE_ID_ARG to it) } ?: emptyMap()),
    )

    @Test
    fun startsLoading() {
        val vm = withRoute(Id().value.toString())
        assertEquals(UiState.Loading, vm.details.value)
        assertFalse(vm.isDeleting.value)
    }

    @Test
    fun load_missingOrMalformedId_showsNotFound() {
        val noId = withRoute(null)
        noId.load(owner)
        assertEquals(UiState.Error("Couldn't find this campsite"), noId.details.value)

        val badId = withRoute("owner_campsite")
        badId.load(owner)
        assertEquals(UiState.Error("Couldn't find this campsite"), badId.details.value)
    }

    @Test
    fun retry_beforeLoadDoesNothing() {
        val vm = withRoute(null)
        vm.retry() // no owner yet, so there's nothing to retry with
        assertEquals(UiState.Loading, vm.details.value)
    }

    @Test
    fun retry_reusesTheLastOwner() {
        val vm = withRoute(null)
        vm.load(owner)
        vm.retry()
        assertEquals(UiState.Error("Couldn't find this campsite"), vm.details.value)
    }

    @Test
    fun delete_beforeLoadFails() {
        val vm = withRoute(Id().value.toString())
        var result: DeleteResult? = null
        vm.delete { result = it }
        assertEquals(DeleteResult.FAILED, result)
        assertFalse(vm.isDeleting.value)
    }

    @Test
    fun delete_withOwnerButNoCampsiteFails() {
        // The owner is known but the campsite never loaded, so there's nothing to delete.
        val vm = withRoute(null)
        vm.load(owner)
        var result: DeleteResult? = null
        vm.delete { result = it }
        assertEquals(DeleteResult.FAILED, result)
        assertFalse(vm.isDeleting.value)
    }
}
