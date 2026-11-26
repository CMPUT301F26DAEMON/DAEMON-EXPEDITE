package com.example.daemon_expedite.frontend_tests

import android.location.Location
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.frontend.OwnerReservationTab
import com.example.daemon_expedite.frontend.ownerReservationCards
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.toKotlinInstant

/** Tests for [ownerReservationCards], which formats the owner's Reservations tab. */
class OwnerReservationsTest {

    private val zone = ZoneId.of("America/Edmonton")
    private val owner = Id()
    private val guest = Id()
    private val pine = Campsite(
        name = "Pine Creek",
        description = "Quiet spot",
        location = mockk<Location>(relaxed = true),
        maxOccupancy = 4,
        pricePerNight = 40,
        ownerId = owner,
        address = "1 Pine Rd",
    )
    private val now = LocalDate.of(2026, 10, 10).atTime(12, 0).atZone(zone).toInstant().toKotlinInstant()

    private fun stay(from: Int, to: Int, cancelled: Boolean = false, campsite: Id = pine.id) =
        Reservation(
            campsite = campsite,
            period = DatePeriod(LocalDate.of(2026, 10, from), LocalDate.of(2026, 10, to)),
            reservedBy = guest,
            isCancelled = cancelled,
        )

    private fun cards(vararg reservations: Reservation) =
        ownerReservationCards(reservations.toList(), mapOf(pine.id to pine), mapOf(guest to "Sam"), now, zone)

    @Test
    fun ordersActiveSoonestFirstThenPastThenCancelled() {
        val later = stay(20, 22)
        val sooner = stay(12, 14)
        val past = stay(1, 3)
        val cancelled = stay(15, 16, cancelled = true)
        val result = cards(later, cancelled, past, sooner)
        assertEquals(
            listOf(sooner, later, past, cancelled).map { it.id.value.toString() },
            result.map { it.id },
        )
        assertEquals(
            listOf(OwnerReservationTab.ACTIVE, OwnerReservationTab.ACTIVE, OwnerReservationTab.PAST, OwnerReservationTab.CANCELLED),
            result.map { it.status },
        )
    }

    @Test
    fun fillsInCampsiteGuestAndNights() {
        val card = cards(stay(12, 15)).single()
        assertEquals("Pine Creek", card.campsiteName)
        assertEquals("1 Pine Rd", card.location)
        assertEquals(40, card.pricePerNight)
        assertEquals("Sam", card.guestName)
        assertEquals(3, card.nights)
        assertTrue(card.confirmed)
        assertNull(card.rating) // no ratings yet
    }

    @Test
    fun usesCoverImageWhenThereIsOneAndDefaultOtherwise() {
        val url = "https://example.com/pine-cover.jpg"
        val withCover = ownerReservationCards(
            listOf(stay(12, 14)), mapOf(pine.id to pine), mapOf(guest to "Sam"), now, zone,
            coverUrls = mapOf(pine.id to url),
        ).single()
        assertEquals(url, withCover.imageUrl)
        // No cover URL means the card falls back to the app's default image.
        assertNull(cards(stay(12, 14)).single().imageUrl)
    }

    @Test
    fun guestCancelAndOwnerRejectAreToldApart() {
        val byGuest = stay(12, 14).apply { cancel() }
        val byOwner = stay(16, 18).apply { reject() }
        val result = cards(byGuest, byOwner).associateBy { it.id }
        assertTrue(result.getValue(byGuest.id.value.toString()).cancelledByGuest)
        assertFalse(result.getValue(byOwner.id.value.toString()).cancelledByGuest)
    }

    @Test
    fun missingCampsiteStillGetsACard() {
        val card = cards(stay(12, 14, campsite = Id())).single()
        assertEquals("Campsite unavailable", card.campsiteName)
        assertNull(card.pricePerNight)
    }
}
