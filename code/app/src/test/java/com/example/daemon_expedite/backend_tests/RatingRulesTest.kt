// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Hook up the campsite rating pipeline", 2026-11-12.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import android.location.Location
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Rating
import com.example.daemon_expedite.backend.Reservation
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RatingRulesTest {

    private val pine = Id()
    private val me = Id()
    private val stranger = Id()

    private fun day(d: Int) = LocalDate.of(2026, 10, d)

    /** My stay at Pine, 16–18 Oct (check-out on the 18th). */
    private fun myStay(cancelled: Boolean = false) =
        Reservation(campsite = pine, period = DatePeriod(day(16), day(18)), reservedBy = me, isCancelled = cancelled)

    private fun ratingFor(reservation: Reservation, author: Id = me, score: Int = 4) =
        Rating(campsiteId = pine, authorId = author, score = score, reservationId = reservation.id)

    @Test
    fun rate_allowedFromCheckOutDay() {
        assertNotNull(Rating.rateProblem(myStay(), me, emptyList(), day(17))) // still staying
        assertNull(Rating.rateProblem(myStay(), me, emptyList(), day(18)))    // check-out day
        assertNull(Rating.rateProblem(myStay(), me, emptyList(), day(30)))
    }

    @Test
    fun rate_onlyByWhoStayed() {
        assertNotNull(Rating.rateProblem(myStay(), stranger, emptyList(), day(20)))
    }

    @Test
    fun rate_notForCancelledStays() {
        assertNotNull(Rating.rateProblem(myStay(cancelled = true), me, emptyList(), day(20)))
    }

    @Test
    fun rate_oncePerStay() {
        val stay = myStay()
        assertNotNull(Rating.rateProblem(stay, me, listOf(ratingFor(stay)), day(20)))
    }

    @Test
    fun rate_otherStaysRatingsDontBlock() {
        // Someone else's review of Pine, and my review of an earlier stay, don't count.
        val other = myStay()
        val existing = listOf(ratingFor(other, author = stranger), ratingFor(other))
        assertNull(Rating.rateProblem(myStay(), me, existing, day(20)))
    }

    @Test
    fun cleanReview_blankIsStarsOnly() {
        assertNull(Rating.cleanReview(null))
        assertNull(Rating.cleanReview(""))
        assertNull(Rating.cleanReview("   \n "))
    }

    @Test
    fun cleanReview_trimsAndCaps() {
        assertEquals("Lovely", Rating.cleanReview("  Lovely \n"))
        assertEquals(Rating.MAX_REVIEW_LENGTH, Rating.cleanReview("a".repeat(5000))!!.length)
    }

    @Test
    fun average_followsTheRatingsAddedToTheCampsite() {
        // What the transaction appends is exactly what averageRating reads back.
        val campsite = Campsite(
            name = "Pine", description = "", location = mockk<Location>(relaxed = true),
            maxOccupancy = 4, pricePerNight = 40, ownerId = Id(), address = "",
            ratings = listOf(ratingFor(myStay(), author = stranger, score = 5)),
        )
        campsite.addRating(ratingFor(myStay(), score = 2))
        assertEquals(3.5, campsite.averageRating(), 0.001)
    }
}
