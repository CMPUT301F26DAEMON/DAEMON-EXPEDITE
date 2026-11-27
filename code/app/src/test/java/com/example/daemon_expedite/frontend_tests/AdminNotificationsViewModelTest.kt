package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Notification
import com.example.daemon_expedite.backend.NotificationKind
import com.example.daemon_expedite.frontend.AdminChatUser
import com.example.daemon_expedite.frontend.AdminNotificationEntry
import com.example.daemon_expedite.frontend.AdminNotificationsViewModel
import com.example.daemon_expedite.frontend.UiState
import com.example.daemon_expedite.frontend.filterNotifications
import com.example.daemon_expedite.frontend.kindLabel
import com.example.daemon_expedite.frontend.recipientsLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Filtering and wording for the admin's notification log. Database results aren't faked here. */
class AdminNotificationsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val owner = Id()
    private val otherOwner = Id()
    private val maya = Id()
    private val lottery = Id()

    private fun entry(sender: Id) = AdminNotificationEntry(Id(), sender, "Owner", null, "Hi", "")

    private val a = entry(owner)
    private val b = entry(otherOwner)
    private val c = entry(owner)
    private val all = UiState.Content(listOf(a, b, c))

    @Test
    fun noFilters_showsEverything() {
        assertEquals(all, filterNotifications(all, null, null))
    }

    @Test
    fun sentBy_keepsThatSendersOnly() {
        assertEquals(UiState.Content(listOf(a, c)), filterNotifications(all, owner, null))
    }

    @Test
    fun receivedBy_keepsTheRecipientsOnly() {
        assertEquals(UiState.Content(listOf(b, c)), filterNotifications(all, null, UiState.Content(setOf(b.id, c.id))))
    }

    @Test
    fun bothFilters_mustBothMatch() {
        assertEquals(UiState.Content(listOf(c)), filterNotifications(all, owner, UiState.Content(setOf(b.id, c.id))))
    }

    @Test
    fun recipientStillLoadingOrFailed_isShownAsSuch() {
        assertEquals(UiState.Loading, filterNotifications(all, null, UiState.Loading))
        assertEquals(UiState.Error("x"), filterNotifications(all, null, UiState.Error("x")))
        assertEquals(UiState.Loading, filterNotifications(UiState.Loading, owner, null))
    }

    @Test
    fun recipientsLine_perKind() {
        val names = mapOf(maya to "Maya")
        val message = Notification(Id(), "Hi", lottery, owner)
        assertEquals("To: waiting list · Pine Lake", recipientsLine(message, "Pine Lake", names))
        assertEquals("To: waiting list", recipientsLine(message, null, names))

        val results = Notification(
            Id(), "Won", lottery, owner, kind = NotificationKind.LOTTERY_RESULT, recipientIds = listOf(maya, Id(), Id()),
        )
        assertEquals("To: 3 entrants · Pine Lake", recipientsLine(results, "Pine Lake", names))

        val confirmation = Notification(
            Id(), "Booked", lottery, owner, kind = NotificationKind.RESERVATION_CONFIRMED, recipientIds = listOf(maya),
        )
        assertEquals("To: Maya · Pine Lake", recipientsLine(confirmation, "Pine Lake", names))

        val gone = Notification(
            Id(), "Booked", lottery, owner, kind = NotificationKind.RESERVATION_CONFIRMED, recipientIds = listOf(Id()),
        )
        assertEquals("To: Deleted user", recipientsLine(gone, null, names))
    }

    @Test
    fun kindLabels() {
        assertEquals("Owner message", kindLabel(NotificationKind.OWNER_MESSAGE))
        assertEquals("Lottery result", kindLabel(NotificationKind.LOTTERY_RESULT))
        assertEquals("Reservation confirmed", kindLabel(NotificationKind.RESERVATION_CONFIRMED))
        assertEquals("Reservation cancelled", kindLabel(NotificationKind.RESERVATION_CANCELLED))
    }

    @Test
    fun lotteryResultKind_isReadBack() {
        assertEquals(NotificationKind.LOTTERY_RESULT, NotificationKind.fromStored("lottery_result"))
    }

    @Test
    fun picks_startAtAll_andCanBeCleared() {
        val vm = AdminNotificationsViewModel()
        assertNull(vm.selectedSender.value)
        assertNull(vm.selectedRecipient.value)
        val user = AdminChatUser(owner, "Oliver")
        vm.selectSender(user)
        assertEquals(user, vm.selectedSender.value)
        vm.selectSender(null)
        assertNull(vm.selectedSender.value)
    }
}
