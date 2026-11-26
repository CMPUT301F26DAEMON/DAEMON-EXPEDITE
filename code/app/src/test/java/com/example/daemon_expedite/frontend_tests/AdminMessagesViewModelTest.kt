package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.NamedUser
import com.example.daemon_expedite.backend.OwnerChatPartner
import com.example.daemon_expedite.frontend.AdminMessagesViewModel
import com.example.daemon_expedite.frontend.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Selection guards for [AdminMessagesViewModel]. Database results aren't faked here. */
class AdminMessagesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val owner = NamedUser(Id(), "Oliver Hayes")
    private val partner = OwnerChatPartner(Id(), NamedUser(Id(), "Maya"))

    @Test
    fun start_nothingPicked() {
        val state = AdminMessagesViewModel().state.value
        assertNull(state.selectedOwner)
        assertNull(state.entrants) // entrant dropdown says "Select owner first"
        assertNull(state.messages) // centre text asks to pick both
    }

    @Test
    fun selectEntrant_beforeOwnerDoesNothing() {
        val vm = AdminMessagesViewModel()
        vm.selectEntrant(partner)
        assertNull(vm.state.value.selectedEntrant)
        assertNull(vm.state.value.messages)
    }

    @Test
    fun selectOwner_picksOwnerAndLoadsTheirChats() {
        val vm = AdminMessagesViewModel()
        vm.selectOwner(owner)
        assertEquals(owner, vm.state.value.selectedOwner)
        assertEquals(UiState.Loading, vm.state.value.entrants)
    }

    @Test
    fun selectOwner_clearsPickedEntrant() {
        val vm = AdminMessagesViewModel()
        vm.selectOwner(owner)
        vm.selectEntrant(partner)
        assertEquals(partner, vm.state.value.selectedEntrant)

        vm.selectOwner(NamedUser(Id(), "Elena Park"))
        assertNull(vm.state.value.selectedEntrant)
        assertNull(vm.state.value.messages)
    }

    @Test
    fun loadOlder_beforeMessagesDoesNothing() {
        val vm = AdminMessagesViewModel()
        vm.loadOlder()
        assertEquals(false, vm.state.value.loadingOlder)
    }
}
