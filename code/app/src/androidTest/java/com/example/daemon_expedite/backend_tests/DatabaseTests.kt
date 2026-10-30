package com.example.daemon_expedite.backend_tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.User
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @Test
    fun create_signsInAnonymously() {
        runBlocking {
            val auth = FirebaseAuth.getInstance()

            // Ensure we test the anonymous sign-in path.
            auth.signOut()

            Database.init()

            assertNotNull(auth.currentUser)
            assertTrue(auth.currentUser!!.isAnonymous)
        }
    }

    @Test
    fun add_testUser() {
        runBlocking {
            Database.init()
            val randomId = "01a17b5a-7d88-75c9-98a0-831fa7ffc7ce"
            val user =
                User(
                    Id(Uuid.parse(randomId)),
                    "test",
                    "test",
                    "test",
                    Id(Uuid.parse(randomId)),
                    List(1) { Id(Uuid.parse(randomId)) },
                    List(1) { Id(Uuid.parse(randomId)) },
                    List(1) { Id(Uuid.parse(randomId)) },
                )
            Database.addUser(user)
        }
    }

    @Test
    fun test_ImageFunction() {
        runBlocking {
            Database.init()
            println(
                Database.getImage(
                    "campsites/01a18eca-6027-7c70-82a8-12b2a05cf4c2/01a18f07-3600-76fc-bd41-e46057e7da7d.jpg",
                ),
            )
        }
    }
}
