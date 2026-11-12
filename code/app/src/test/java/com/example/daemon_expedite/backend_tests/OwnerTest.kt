// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import android.location.Location
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.Rating
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.backend.User
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

/** Unit tests for [Owner]. The model only changes in-memory state; saving is up to ViewModels. */
class OwnerTest {

    private val stay = DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18))

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
    fun addCampsite_newIsRecorded() {
        val owner = owner()
        val campsite = campsite(owner.id)

        assertTrue(owner.addCampsite(campsite))
        assertEquals(listOf(campsite.id), owner.ownerCampsites)
    }

    @Test
    fun addCampsite_duplicateIsIgnored() {
        val placeholder = owner()
        val campsite = campsite(placeholder.id)
        val owner = owner(campsites = listOf(campsite.id))

        assertFalse(owner.addCampsite(campsite))
        assertEquals(listOf(campsite.id), owner.ownerCampsites)
    }

    @Test
    fun removeCampsite_undoesAdd() {
        val owner = owner()
        val campsite = campsite(owner.id)
        owner.addCampsite(campsite)

        assertTrue(owner.removeCampsite(campsite))
        assertTrue(owner.ownerCampsites.isEmpty())
    }

    // endregion

    // region Lotteries

    @Test
    fun addOwnerLottery_ownLotteryIsRecorded() {
        val owner = owner()
        val lottery = lottery(owner.id)

        assertTrue(owner.addOwnerLottery(lottery))
        assertEquals(listOf(lottery.id), owner.ownerLotteries)
    }

    @Test
    fun addOwnerLottery_duplicateIsIgnored() {
        val first = owner()
        val lottery = lottery(first.id)
        val owner = Owner(id = first.id, notificationsEnabled = true, ownerLotteries = listOf(lottery.id))

        assertFalse(owner.addOwnerLottery(lottery))
        assertEquals(listOf(lottery.id), owner.ownerLotteries)
    }

    @Test
    fun addOwnerLottery_otherOwnersLotteryIsRefused() {
        val owner = owner()
        try {
            owner.addOwnerLottery(lottery(ownerId = Id()))
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(owner.ownerLotteries.isEmpty())
    }

    // endregion

    // region Reservations

    @Test
    fun addOwnerReservation_newIsRecorded() {
        val owner = owner()
        val reservation = reservation()

        assertTrue(owner.addOwnerReservation(reservation))
        assertEquals(listOf(reservation.id), owner.ownerReservations)
    }

    @Test
    fun addOwnerReservation_duplicateIsIgnored() {
        val reservation = reservation()
        val owner = owner(reservations = listOf(reservation.id))

        assertFalse(owner.addOwnerReservation(reservation))
        assertEquals(listOf(reservation.id), owner.ownerReservations)
    }

    // endregion
}
