// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import android.location.Location
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Image
import com.example.daemon_expedite.backend.Rating
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Unit tests for [Campsite]. The model only changes in-memory state; saving is up to ViewModels. */
class CampsiteTest {

    private fun campsite(images: List<Image> = emptyList(), rating: Rating = Rating()) = Campsite(
        name = "Pine Creek",
        description = "Quiet spot by the river",
        tags = listOf("Water"),
        location = mockk<Location>(relaxed = true),
        maxOccupancy = 4,
        pricePerNight = 40,
        images = images,
        ownerId = Id(),
        rating = rating,
        address = "1 Pine Rd",
    )

    @Test
    fun updateDetails_changesFields() {
        val campsite = campsite()
        campsite.updateDetails("Maple Hollow", 55, 6)

        assertEquals("Maple Hollow", campsite.name)
        assertEquals(55, campsite.pricePerNight)
        assertEquals(6, campsite.maxOccupancy)
    }

    @Test
    fun addImage_newImageIsAdded() {
        val campsite = campsite()
        val image = Image(uploader = campsite.ownerId, path = "campsites/a.jpg")

        assertTrue(campsite.addImage(image))
        assertEquals(listOf(image), campsite.images)
    }

    @Test
    fun addImage_sameImageTwiceIsIgnored() {
        val image = Image(uploader = Id(), path = "campsites/a.jpg")
        val campsite = campsite(images = listOf(image))

        assertFalse(campsite.addImage(image))
        assertEquals(1, campsite.images.size)
    }

    @Test
    fun updateRating_firstRatingSetsAverage() {
        val campsite = campsite()
        campsite.updateRating(4)

        assertEquals(1, campsite.rating.count)
        assertEquals(4.0, campsite.rating.average(), 0.001)
    }

    @Test
    fun updateRating_addsToExistingRatings() {
        val campsite = campsite(rating = Rating(totalScore = 10, count = 2)) // average 5
        campsite.updateRating(2)

        assertEquals(3, campsite.rating.count)
        assertEquals(4.0, campsite.rating.average(), 0.001)
    }

    @Test
    fun updateRating_outOfRangeIsRefused() {
        val campsite = campsite()
        for (bad in listOf(0, 6, -3)) {
            try {
                campsite.updateRating(bad)
                fail("Expected IllegalArgumentException for $bad")
            } catch (_: IllegalArgumentException) {
            }
        }
        assertEquals(0, campsite.rating.count)
    }
}
