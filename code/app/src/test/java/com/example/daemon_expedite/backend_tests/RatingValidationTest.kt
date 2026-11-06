package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.Rating
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Input checks for [Rating.addRating]: valid stars (1 to [Rating.MAX_RATING]) vs. out of range.
 * The other Rating tests are in androidTest/RatingTest.
 */
class RatingValidationTest {

    @Test
    fun lowestAndHighestStarsAreAccepted() {
        val rating = Rating()
        rating.addRating(1)
        rating.addRating(Rating.MAX_RATING)
        assertEquals(2, rating.count)
        assertEquals(1 + Rating.MAX_RATING, rating.totalScore)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroStarsIsRefused() {
        Rating().addRating(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aboveMaxIsRefused() {
        Rating().addRating(Rating.MAX_RATING + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeIsRefused() {
        Rating().addRating(-10)
    }

    @Test
    fun refusedRatingLeavesTotalsUnchanged() {
        val rating = Rating(totalScore = 8, count = 2)
        try {
            rating.addRating(99)
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(8, rating.totalScore)
        assertEquals(2, rating.count)
        assertEquals(4.0, rating.average(), 0.001)
    }
}
