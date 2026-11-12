// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.Chat
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Image
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.backend.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

/**
 * Unit tests for [User]. The model only changes in-memory state; saving is done by
 * SessionViewModel through Database, so no database mock is needed here.
 */
class UserTest {

    private val stay = DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18))

    private fun user(
        name: String = "Sam",
        email: String = "sam@example.com",
        phone: String = "7805550123",
        lotteries: List<Id> = emptyList(),
        reservations: List<Id> = emptyList(),
        chats: List<Id> = emptyList(),
    ) = User(
        name = name,
        email = email,
        phoneNumber = phone,
        notificationsEnabled = true,
        lotteries = lotteries,
        reservations = reservations,
        chats = chats,
    )

    private fun lottery(id: Id = Id()) = Lottery(
        id = id,
        ownerId = Id(),
        campsites = listOf(Id()),
        entryStart = Instant.fromEpochSeconds(0),
        entryEnd = Instant.fromEpochSeconds(1),
        stayPeriods = listOf(stay),
        winnerCount = 1,
    )

    // region Profile

    @Test
    fun isProfileComplete_allFieldsFilled() {
        assertTrue(user().isProfileComplete())
    }

    @Test
    fun isProfileComplete_anyBlankFieldIsIncomplete() {
        assertFalse(user(name = "").isProfileComplete())
        assertFalse(user(email = "   ").isProfileComplete())
        assertFalse(user(phone = "").isProfileComplete())
        assertFalse(User(notificationsEnabled = false).isProfileComplete()) // brand new user
    }

    @Test
    fun updateProfile_changesFields() {
        val user = user()
        user.updateProfile("Alex", "alex@example.com", "5875550199")

        assertEquals("Alex", user.name)
        assertEquals("alex@example.com", user.email)
        assertEquals("5875550199", user.phoneNumber)
    }

    @Test
    fun updateProfileImage_setsImage() {
        val user = user()
        val image = Image(uploader = user.id, path = "profiles/a.jpg")
        user.updateProfileImage(image)

        assertSame(image, user.profileImage)
    }

    @Test
    fun notifications_canBeTurnedOffAndOn() {
        val user = user()
        user.enableOrDisableNotifications(false)
        assertFalse(user.notificationsEnabled)
        user.enableOrDisableNotifications(true)
        assertTrue(user.notificationsEnabled)
    }

    // endregion

    // region Lists (lotteries, reservations, chats)

    @Test
    fun addLottery_newIsAdded() {
        val user = user()
        val lottery = lottery()

        assertTrue(user.addLottery(lottery))
        assertEquals(listOf(lottery.id), user.lotteryIds)
    }

    @Test
    fun addLottery_duplicateIsIgnored() {
        val lottery = lottery()
        val user = user(lotteries = listOf(lottery.id))

        assertFalse(user.addLottery(lottery))
        assertEquals(listOf(lottery.id), user.lotteryIds)
    }

    @Test
    fun removeLottery_presentIsRemoved() {
        val lottery = lottery()
        val user = user(lotteries = listOf(lottery.id))

        assertTrue(user.removeLottery(lottery))
        assertTrue(user.lotteryIds.isEmpty())
    }

    @Test
    fun removeLottery_absentDoesNothing() {
        val user = user()
        assertFalse(user.removeLottery(lottery()))
    }

    @Test
    fun addReservation_newIsAddedDuplicateIgnored() {
        val user = user()
        val reservation = Reservation(campsite = Id(), period = stay, reservedBy = user.id)

        assertTrue(user.addReservation(reservation))
        assertFalse(user.addReservation(reservation))
        assertEquals(listOf(reservation.id), user.reservationIds)
    }

    @Test
    fun addChat_newIsAddedDuplicateIgnored() {
        val user = user()
        val chat = Chat(Id(), user.id, Id(), mutableListOf(), mutableListOf())

        assertTrue(user.addChat(chat))
        assertFalse(user.addChat(chat))
        assertEquals(listOf(chat.id), user.chatIds)
    }

    // endregion

    // region Joining and leaving lotteries

    @Test
    fun joinLottery_isRemembered() {
        val user = user()
        val lotteryId = Id()
        user.joinLottery(lotteryId)

        assertEquals(listOf(lotteryId), user.lotteryIds)
    }

    @Test
    fun joinLottery_alreadyRememberedIsNotDuplicated() {
        val lotteryId = Id()
        val user = user(lotteries = listOf(lotteryId))
        user.joinLottery(lotteryId)

        assertEquals(listOf(lotteryId), user.lotteryIds)
    }

    @Test
    fun leaveLottery_isForgotten() {
        val lotteryId = Id()
        val user = user(lotteries = listOf(lotteryId))
        user.leaveLottery(lotteryId)

        assertTrue(user.lotteryIds.isEmpty())
    }

    // endregion

    // region Deleting

    @Test
    fun deleteProfile_clearsLists() {
        val user = user(lotteries = listOf(Id()), reservations = listOf(Id()), chats = listOf(Id()))
        user.deleteProfile()

        assertTrue(user.lotteryIds.isEmpty())
        assertTrue(user.reservationIds.isEmpty())
        assertTrue(user.chatIds.isEmpty())
    }

    // endregion
}
