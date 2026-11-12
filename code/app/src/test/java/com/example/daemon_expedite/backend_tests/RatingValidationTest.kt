// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Rating
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Input checks for [Rating]: valid stars (1 to [Rating.MAX_RATING]) vs. out of range.
 * The other Rating tests are in androidTest/RatingTest.
 */
class RatingValidationTest {

    private fun rating(score: Int) =
        Rating(campsiteId = Id(), authorId = Id(), score = score, reservationId = Id())

    @Test
    fun lowestAndHighestStarsAreAccepted() {
        assertEquals(1, rating(1).score)
        assertEquals(Rating.MAX_RATING, rating(Rating.MAX_RATING).score)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroStarsIsRefused() {
        rating(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aboveMaxIsRefused() {
        rating(Rating.MAX_RATING + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeIsRefused() {
        rating(-10)
    }
}
