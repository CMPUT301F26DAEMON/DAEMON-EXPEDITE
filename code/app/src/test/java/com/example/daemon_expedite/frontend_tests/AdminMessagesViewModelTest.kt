package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.frontend.AdminChatUser
import com.example.daemon_expedite.frontend.AdminMessagesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Selection rules for [AdminMessagesViewModel]. Database results aren't faked here. */
class AdminMessagesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val owner = AdminChatUser(Id(), "Oliver Hayes")
    private val entrant = AdminChatUser(Id(), "Maya")

    @Test
    fun start_nothingPickedNoMessages() {
        val vm = AdminMessagesViewModel()
        assertNull(vm.selectedOwner.value)
        assertNull(vm.selectedEntrant.value)
        assertTrue(vm.messages.value.isEmpty())
    }

    @Test
    fun picksAreKept() {
        val vm = AdminMessagesViewModel()
        vm.selectOwner(owner)
        vm.selectEntrant(entrant)
        assertEquals(owner, vm.selectedOwner.value)
        assertEquals(entrant, vm.selectedEntrant.value)
    }

    @Test
    fun pickingTheEntrantAsOwner_clearsTheEntrant() {
        val vm = AdminMessagesViewModel()
        vm.selectEntrant(entrant)
        vm.selectOwner(entrant)
        assertNull(vm.selectedEntrant.value)
    }
}
