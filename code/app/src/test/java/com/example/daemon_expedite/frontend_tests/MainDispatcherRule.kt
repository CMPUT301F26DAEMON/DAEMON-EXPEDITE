package com.example.daemon_expedite.frontend_tests

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` (which `viewModelScope` runs on) for a test dispatcher.
 *
 * A [StandardTestDispatcher] queues launched coroutines instead of running them, so a ViewModel
 * that starts loading from the database can be created without reaching Firebase. Call
 * `dispatcher.scheduler.advanceUntilIdle()` only when the queued work doesn't touch the database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
