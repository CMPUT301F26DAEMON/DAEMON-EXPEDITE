package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.frontend.LotteryActionResult
import com.example.daemon_expedite.frontend.SessionState
import com.example.daemon_expedite.frontend.SessionViewModel
import com.example.daemon_expedite.frontend.runCatchingAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Signing in needs Firebase, so the start-up load is queued and never run. These tests cover
 * what every action does while there is no signed-in user yet.
 */
class SessionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val genericError = "Something went wrong. Check your connection and try again."

    @Test
    fun startsLoadingWithNoUser() {
        val session = SessionViewModel()
        assertEquals(SessionState.Loading, session.state.value)
        assertNull(session.user)
    }

    @Test
    fun retry_onlyAfterFailure() {
        val session = SessionViewModel()
        session.retry() // still loading: ignored
        assertEquals(SessionState.Loading, session.state.value)
    }

    @Test
    fun userChanged_whileLoadingStaysLoading() {
        val session = SessionViewModel()
        session.userChanged()
        assertEquals(SessionState.Loading, session.state.value)
    }

    @Test
    fun profileChanges_withoutUserFail() {
        val session = SessionViewModel()
        val results = mutableListOf<Boolean>()
        session.updateProfile("Sam", "sam@example.com", "7805551234") { results += it }
        session.setNotificationsEnabled(false) { results += it }
        session.removeProfilePhoto { results += it }
        session.deleteProfile { results += it }
        assertEquals(listOf(false, false, false, false), results)
        assertEquals(SessionState.Loading, session.state.value)
    }

    @Test
    fun lotteryActions_withoutUserFail() {
        val session = SessionViewModel()
        val results = mutableListOf<LotteryActionResult>()
        session.joinLottery(Id()) { results += it }
        session.leaveLottery(Id()) { results += it }
        assertEquals(listOf(LotteryActionResult.FAILED, LotteryActionResult.FAILED), results)
    }

    @Test
    fun bookLotteryStay_withoutUserGivesMessage() {
        val session = SessionViewModel()
        var result: String? = null
        session.bookLotteryStay(Id(), Id(), DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18))) {
            result = it
        }
        assertEquals(genericError, result)
    }

    @Test
    fun readyState_revisionMakesEqualUsersCompareDifferent() {
        // The user object is mutable, so revision is what tells Compose something changed.
        val user = com.example.daemon_expedite.backend.User(notificationsEnabled = true)
        val before = SessionState.Ready(user)
        assertEquals(0, before.revision)
        assertTrue(before != before.copy(revision = 1))
        assertEquals(before, before.copy())
    }

    // region runCatchingAll

    @Test
    fun runCatchingAll_trueWhenBlockFinishes() {
        var ran = false
        assertTrue(runBlocking { runCatchingAll("unused") { ran = true } })
        assertTrue(ran)
    }

    @Test
    fun runCatchingAll_rethrowsCancellation() {
        // Cancellation must not be reported as a failure, or a closed screen would show an error.
        assertThrows(CancellationException::class.java) {
            runBlocking { runCatchingAll("unused") { throw CancellationException("left the screen") } }
        }
    }

    // endregion
}
