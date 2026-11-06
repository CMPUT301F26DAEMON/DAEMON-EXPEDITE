// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import android.location.Location
import com.example.daemon_expedite.backend.Admin
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.NotFoundException
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.Rating
import com.example.daemon_expedite.backend.User
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Unit tests for [Admin], with [Database] mocked by MockK. */
class AdminTest {

    private val admin = Admin(name = "Admin", notificationsEnabled = false)

    @Before
    fun mockDatabase() {
        mockkObject(Database)
        coJustRun { Database.deleteImage(any()) }
        coJustRun { Database.deleteUser(any<Id>()) }
        coJustRun { Database.deleteOwner(any<Id>()) }
        coJustRun { Database.deleteCampsite(any()) }
    }

    @After
    fun restoreDatabase() = unmockkAll()

    @Test
    fun admin_isAlsoAnOwnerAndAUser() {
        assertTrue(admin is Owner)
        assertTrue(admin is User)
    }

    @Test
    fun removeImage_deletesThatImage() = runTest {
        val imageId = Id()
        admin.removeImage(imageId)
        coVerify(exactly = 1) { Database.deleteImage(imageId) }
    }

    @Test
    fun removeProfile_deletesThatUser() = runTest {
        val userId = Id()
        admin.removeProfile(userId)
        coVerify(exactly = 1) { Database.deleteUser(userId) }
    }

    @Test
    fun removeOwner_deletesThatOwner() = runTest {
        val ownerId = Id()
        admin.removeOwner(ownerId)
        coVerify(exactly = 1) { Database.deleteOwner(ownerId) }
    }

    @Test
    fun removeCampsite_fetchesThenDeletesTheFullCampsite() = runTest {
        val campsite = Campsite(
            name = "Pine Creek",
            description = "",
            location = mockk<Location>(relaxed = true),
            maxOccupancy = 2,
            pricePerNight = 30,
            ownerId = Id(),
            rating = Rating(),
            address = "",
        )
        coEvery { Database.fetchCampsite(campsite.id) } returns campsite

        admin.removeCampsite(campsite.id)

        coVerifyOrder {
            Database.fetchCampsite(campsite.id)
            Database.deleteCampsite(campsite)
        }
    }

    @Test
    fun removeCampsite_missingCampsiteDeletesNothing() = runTest {
        val missing = Id()
        coEvery { Database.fetchCampsite(missing) } throws NotFoundException("Campsite $missing not found")

        try {
            admin.removeCampsite(missing)
            fail("Expected NotFoundException")
        } catch (_: NotFoundException) {
        }
        coVerify(exactly = 0) { Database.deleteCampsite(any()) }
    }
}
