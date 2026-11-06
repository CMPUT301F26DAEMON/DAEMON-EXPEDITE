// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.BookingRejectedException
import com.example.daemon_expedite.backend.Chat
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Image
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryClosedException
import com.example.daemon_expedite.backend.LotteryFullException
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.backend.User
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

/**
 * Unit tests for [User]. [Database] is replaced with a MockK mock, so no Firebase is needed:
 * each test checks the in-memory change and which database call was made (or not made).
 */
class UserTest {

    private val stay = DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18))

    @Before
    fun mockDatabase() {
        mockkObject(Database)
        coJustRun { Database.updateUser(any()) }
        coJustRun { Database.deleteUser(any<Id>()) }
        coJustRun { Database.addUserToLottery(any(), any()) }
        coJustRun { Database.removeUserFromLottery(any(), any()) }
    }

    @After
    fun restoreDatabase() = unmockkAll()

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
    fun updateProfile_changesFieldsAndSaves() = runTest {
        val user = user()
        user.updateProfile("Alex", "alex@example.com", "5875550199")

        assertEquals("Alex", user.name)
        assertEquals("alex@example.com", user.email)
        assertEquals("5875550199", user.phoneNumber)
        coVerify(exactly = 1) { Database.updateUser(user) }
    }

    @Test
    fun updateProfileImage_setsImageAndSaves() = runTest {
        val user = user()
        val image = Image(uploader = user.id, path = "profiles/a.jpg")
        user.updateProfileImage(image)

        assertSame(image, user.profileImage)
        coVerify(exactly = 1) { Database.updateUser(user) }
    }

    @Test
    fun notifications_canBeTurnedOffAndOn() = runTest {
        val user = user()
        user.enableOrDisableNotifications(false)
        assertFalse(user.notificationsEnabled)
        user.enableOrDisableNotifications(true)
        assertTrue(user.notificationsEnabled)
        coVerify(exactly = 2) { Database.updateUser(user) }
    }

    // endregion

    // region Lists (lotteries, reservations, chats)

    @Test
    fun addLottery_newIsAddedAndSaved() = runTest {
        val user = user()
        val lottery = lottery()
        user.addLottery(lottery)

        assertEquals(listOf(lottery.id), user.lotteryIds)
        coVerify(exactly = 1) { Database.updateUser(user) }
    }

    @Test
    fun addLottery_duplicateIsIgnored() = runTest {
        val lottery = lottery()
        val user = user(lotteries = listOf(lottery.id))
        user.addLottery(lottery)

        assertEquals(listOf(lottery.id), user.lotteryIds)
        coVerify(exactly = 0) { Database.updateUser(any()) }
    }

    @Test
    fun removeLottery_presentIsRemovedAndSaved() = runTest {
        val lottery = lottery()
        val user = user(lotteries = listOf(lottery.id))
        user.removeLottery(lottery)

        assertTrue(user.lotteryIds.isEmpty())
        coVerify(exactly = 1) { Database.updateUser(user) }
    }

    @Test
    fun removeLottery_absentDoesNothing() = runTest {
        val user = user()
        user.removeLottery(lottery())
        coVerify(exactly = 0) { Database.updateUser(any()) }
    }

    @Test
    fun addReservation_newIsAddedDuplicateIgnored() = runTest {
        val user = user()
        val reservation = Reservation(campsite = Id(), period = stay, reservedBy = user.id)
        user.addReservation(reservation)
        user.addReservation(reservation)

        assertEquals(listOf(reservation.id), user.reservationIds)
        coVerify(exactly = 1) { Database.updateUser(user) }
    }

    @Test
    fun addChat_newIsAddedDuplicateIgnored() = runTest {
        val user = user()
        val chat = Chat(Id(), user.id, Id(), mutableListOf(), mutableListOf())
        user.addChat(chat)
        user.addChat(chat)

        assertEquals(listOf(chat.id), user.chatIds)
        coVerify(exactly = 1) { Database.updateUser(user) }
    }

    // endregion

    // region Joining and leaving lotteries

    @Test
    fun joinLottery_openLotteryIsRemembered() = runTest {
        val user = user()
        val lotteryId = Id()
        user.joinLottery(lotteryId)

        assertEquals(listOf(lotteryId), user.lotteryIds)
        coVerify(exactly = 1) { Database.addUserToLottery(lotteryId, user.id) }
    }

    @Test
    fun joinLottery_alreadyRememberedIsNotDuplicated() = runTest {
        val lotteryId = Id()
        val user = user(lotteries = listOf(lotteryId))
        user.joinLottery(lotteryId)
        assertEquals(listOf(lotteryId), user.lotteryIds)
    }

    @Test
    fun joinLottery_closedLotteryIsNotRemembered() = runTest {
        val user = user()
        val lotteryId = Id()
        coEvery { Database.addUserToLottery(lotteryId, user.id) } throws LotteryClosedException(lotteryId)

        try {
            user.joinLottery(lotteryId)
            fail("Expected LotteryClosedException")
        } catch (_: LotteryClosedException) {
        }
        assertTrue(user.lotteryIds.isEmpty())
    }

    @Test
    fun joinLottery_fullLotteryIsNotRemembered() = runTest {
        val user = user()
        val lotteryId = Id()
        coEvery { Database.addUserToLottery(lotteryId, user.id) } throws LotteryFullException(lotteryId)

        try {
            user.joinLottery(lotteryId)
            fail("Expected LotteryFullException")
        } catch (_: LotteryFullException) {
        }
        assertTrue(user.lotteryIds.isEmpty())
    }

    @Test
    fun leaveLottery_openLotteryIsForgotten() = runTest {
        val lotteryId = Id()
        val user = user(lotteries = listOf(lotteryId))
        user.leaveLottery(lotteryId)

        assertTrue(user.lotteryIds.isEmpty())
        coVerify(exactly = 1) { Database.removeUserFromLottery(lotteryId, user.id) }
    }

    @Test
    fun leaveLottery_closedLotteryIsKept() = runTest {
        val lotteryId = Id()
        val user = user(lotteries = listOf(lotteryId))
        coEvery { Database.removeUserFromLottery(lotteryId, user.id) } throws LotteryClosedException(lotteryId)

        try {
            user.leaveLottery(lotteryId)
            fail("Expected LotteryClosedException")
        } catch (_: LotteryClosedException) {
        }
        assertEquals(listOf(lotteryId), user.lotteryIds)
    }

    // endregion

    // region Booking

    @Test
    fun bookLotteryStay_acceptedBookingIsRemembered() = runTest {
        val user = user()
        val lotteryId = Id()
        val campsiteId = Id()
        val saved = Reservation(campsite = campsiteId, period = stay, reservedBy = user.id, lotteryId = lotteryId)
        coEvery { Database.bookLotteryStay(lotteryId, user.id, campsiteId, stay) } returns saved

        val result = user.bookLotteryStay(lotteryId, campsiteId, stay)

        assertSame(saved, result)
        assertEquals(listOf(saved.id), user.reservationIds)
    }

    @Test
    fun bookLotteryStay_rejectedBookingIsNotRemembered() = runTest {
        val user = user()
        coEvery { Database.bookLotteryStay(any(), any(), any(), any()) } throws BookingRejectedException("Taken")

        try {
            user.bookLotteryStay(Id(), Id(), stay)
            fail("Expected BookingRejectedException")
        } catch (e: BookingRejectedException) {
            assertEquals("Taken", e.message)
        }
        assertTrue(user.reservationIds.isEmpty())
    }

    // endregion

    // region Deleting

    @Test
    fun deleteProfile_deletesAndClearsLists() = runTest {
        val user = user(lotteries = listOf(Id()), reservations = listOf(Id()), chats = listOf(Id()))
        user.deleteProfile()

        coVerify(exactly = 1) { Database.deleteUser(user.id) }
        assertTrue(user.lotteryIds.isEmpty())
        assertTrue(user.reservationIds.isEmpty())
        assertTrue(user.chatIds.isEmpty())
    }

    @Test
    fun deleteProfile_failedDeleteKeepsLists() = runTest {
        val lotteryId = Id()
        val user = user(lotteries = listOf(lotteryId))
        coEvery { Database.deleteUser(user.id) } throws RuntimeException("offline")

        try {
            user.deleteProfile()
            fail("Expected RuntimeException")
        } catch (_: RuntimeException) {
        }
        assertEquals(listOf(lotteryId), user.lotteryIds)
    }

    // endregion
}
