// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import android.location.Location
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.Rating
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.backend.User
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

/** Unit tests for [Owner], with [Database] mocked by MockK. */
class OwnerTest {

    private val stay = DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18))

    @Before
    fun mockDatabase() {
        mockkObject(Database)
        coJustRun { Database.addOwner(any()) }
        coJustRun { Database.createLottery(any()) }
        coJustRun { Database.addReservation(any()) }
    }

    @After
    fun restoreDatabase() = unmockkAll()

    private fun owner(
        campsites: List<Id> = emptyList(),
        lotteries: List<Id> = emptyList(),
        reservations: List<Id> = emptyList(),
    ) = Owner(
        name = "Riley",
        notificationsEnabled = true,
        ownerCampsites = campsites,
        ownerLotteries = lotteries,
        ownerReservations = reservations,
    )

    private fun campsite(ownerId: Id) = Campsite(
        name = "Pine Creek",
        description = "Quiet spot",
        location = mockk<Location>(relaxed = true),
        maxOccupancy = 4,
        pricePerNight = 40,
        ownerId = ownerId,
        rating = Rating(),
        address = "1 Pine Rd",
    )

    private fun lottery(ownerId: Id) = Lottery(
        ownerId = ownerId,
        campsites = listOf(Id()),
        entryStart = Instant.fromEpochSeconds(0),
        entryEnd = Instant.fromEpochSeconds(1),
        stayPeriods = listOf(stay),
        winnerCount = 1,
    )

    private fun reservation() = Reservation(campsite = Id(), period = stay, reservedBy = Id())

    @Test
    fun owner_isAlsoAUser() {
        val owner = owner()
        assertTrue(owner is User)
        assertTrue(owner.ownerCampsites.isEmpty())
        assertTrue(owner.ownerLotteries.isEmpty())
        assertTrue(owner.ownerReservations.isEmpty())
    }

    // region Campsites

    @Test
    fun addCampsite_newIsRecordedAndOwnerSaved() = runTest {
        val owner = owner()
        val campsite = campsite(owner.id)
        owner.addCampsite(campsite)

        assertEquals(listOf(campsite.id), owner.ownerCampsites)
        coVerify(exactly = 1) { Database.addOwner(owner) }
    }

    @Test
    fun addCampsite_duplicateIsIgnored() = runTest {
        val placeholder = owner()
        val campsite = campsite(placeholder.id)
        val owner = owner(campsites = listOf(campsite.id))
        owner.addCampsite(campsite)

        assertEquals(listOf(campsite.id), owner.ownerCampsites)
        coVerify(exactly = 0) { Database.addOwner(any()) }
    }

    @Test
    fun addCampsite_failedSaveIsNotRecorded() = runTest {
        val owner = owner()
        coEvery { Database.addOwner(any()) } throws RuntimeException("offline")
        try {
            owner.addCampsite(campsite(owner.id))
            fail("Expected RuntimeException")
        } catch (_: RuntimeException) {
        }
        assertTrue(owner.ownerCampsites.isEmpty())
    }

    // endregion

    // region Lotteries

    @Test
    fun addOwnerLottery_ownLotteryIsCreatedAndRecorded() = runTest {
        val owner = owner()
        val lottery = lottery(owner.id)
        owner.addOwnerLottery(lottery)

        assertEquals(listOf(lottery.id), owner.ownerLotteries)
        coVerify(exactly = 1) { Database.createLottery(lottery) }
    }

    @Test
    fun addOwnerLottery_duplicateIsIgnored() = runTest {
        val first = owner()
        val lottery = lottery(first.id)
        val owner = Owner(id = first.id, notificationsEnabled = true, ownerLotteries = listOf(lottery.id))
        owner.addOwnerLottery(lottery)

        assertEquals(listOf(lottery.id), owner.ownerLotteries)
        coVerify(exactly = 0) { Database.createLottery(any()) }
    }

    @Test
    fun addOwnerLottery_otherOwnersLotteryIsRefused() = runTest {
        val owner = owner()
        try {
            owner.addOwnerLottery(lottery(ownerId = Id()))
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(owner.ownerLotteries.isEmpty())
        coVerify(exactly = 0) { Database.createLottery(any()) }
    }

    @Test
    fun addOwnerLottery_failedSaveIsNotRecorded() = runTest {
        val owner = owner()
        coEvery { Database.createLottery(any()) } throws RuntimeException("offline")
        try {
            owner.addOwnerLottery(lottery(owner.id))
            fail("Expected RuntimeException")
        } catch (_: RuntimeException) {
        }
        assertTrue(owner.ownerLotteries.isEmpty())
    }

    // endregion

    // region Reservations

    @Test
    fun addOwnerReservation_newIsSavedThenOwnerSaved() = runTest {
        val owner = owner()
        val reservation = reservation()
        owner.addOwnerReservation(reservation)

        assertEquals(listOf(reservation.id), owner.ownerReservations)
        coVerify(exactly = 1) { Database.addReservation(reservation) }
        coVerify(exactly = 1) { Database.addOwner(owner) }
    }

    @Test
    fun addOwnerReservation_duplicateIsIgnored() = runTest {
        val reservation = reservation()
        val owner = owner(reservations = listOf(reservation.id))
        owner.addOwnerReservation(reservation)

        coVerify(exactly = 0) { Database.addReservation(any()) }
        coVerify(exactly = 0) { Database.addOwner(any()) }
    }

    @Test
    fun addOwnerReservation_failedSaveIsNotRecorded() = runTest {
        val owner = owner()
        coEvery { Database.addReservation(any()) } throws RuntimeException("offline")
        try {
            owner.addOwnerReservation(reservation())
            fail("Expected RuntimeException")
        } catch (_: RuntimeException) {
        }
        assertTrue(owner.ownerReservations.isEmpty())
        coVerify(exactly = 0) { Database.addOwner(any()) }
    }

    @Test
    fun addOwnerReservation_failedOwnerSaveIsNotRecorded() = runTest {
        val owner = owner()
        coEvery { Database.addOwner(any()) } throws RuntimeException("offline")
        try {
            owner.addOwnerReservation(reservation())
            fail("Expected RuntimeException")
        } catch (_: RuntimeException) {
        }
        assertTrue(owner.ownerReservations.isEmpty())
    }

    // endregion
}
