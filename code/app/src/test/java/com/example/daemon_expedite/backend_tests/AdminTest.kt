// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Write unit tests for the model classes with a mocked database", 2026-11-06.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.Admin
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.User
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Admin]. Admin moderation (deleting images, users, owners and campsites) is
 * done by the admin ViewModels through Database, so the model only needs to be an Owner and a User.
 */
class AdminTest {

    private val admin = Admin(name = "Admin", notificationsEnabled = false)

    @Test
    fun admin_isAlsoAnOwnerAndAUser() {
        assertTrue(admin is Owner)
        assertTrue(admin is User)
    }
}
