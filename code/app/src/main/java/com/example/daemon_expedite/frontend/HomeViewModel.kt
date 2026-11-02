package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Data for the entrant home screen: the search text and the campsites to show.
 *
 * Loads suggested campsites on start, and searches again whenever the search text changes.
 * Also reloads quietly each time the screen is shown again (see [onScreenShown]), because this
 * ViewModel lives as long as the Home screen, which stays at the bottom of the back stack.
 *
 * Campsites owned by the signed-in user are left out (see [setViewer]): you can't book your own
 * campsite, so entrant home doesn't offer it. They still show on the owner home page.
 * Searching waits until the user stops typing for [SEARCH_DELAY_MS] so the database is not
 * queried on every key press.
 */
@OptIn(FlowPreview::class)
class HomeViewModel : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _campsites = MutableStateFlow<UiState<List<CampsiteSummary>>>(UiState.Loading)
    val campsites: StateFlow<UiState<List<CampsiteSummary>>> = _campsites.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(SEARCH_DELAY_MS)
                .distinctUntilChanged()
                // collectLatest cancels an older search if a newer one starts.
                .collectLatest { search(it) }
        }
    }

    fun onQueryChange(text: String) {
        _query.value = text
    }

    private var shownBefore = false

    /** The signed-in user's id. Their own campsites are hidden. Null until the user has loaded. */
    private var viewerId: Id? = null

    /**
     * Tells the ViewModel who is looking, so their own campsites can be hidden. Owner ids are
     * the same as user ids. If a list was already loaded without this, it is filtered straight away.
     */
    fun setViewer(userId: Id?) {
        if (userId == viewerId) return
        viewerId = userId
        val current = _campsites.value
        if (current is UiState.Content && userId != null) {
            _campsites.value = UiState.Content(current.data.filterNot { it.ownerId == userId })
        }
    }

    /**
     * Call each time the Home screen appears. The first time does nothing (the start-up search
     * already covers it). After that, for example coming back from the owner view after
     * registering a campsite, it reloads in the background: the current list stays on screen
     * until the new one arrives, and stays put if the reload fails.
     */
    fun onScreenShown() {
        if (!shownBefore) {
            shownBefore = true
            return
        }
        viewModelScope.launch {
            val fresh = loadAsUiState("Couldn't refresh campsites") { fetch(_query.value) }
            if (fresh is UiState.Content || _campsites.value !is UiState.Content) _campsites.value = fresh
        }
    }

    /** Runs the current search again, for example from a "try again" button. */
    fun retry() {
        viewModelScope.launch { search(_query.value) }
    }

    private suspend fun search(text: String) {
        _campsites.value = UiState.Loading
        _campsites.value = loadAsUiState("Couldn't load campsites") { fetch(text) }
    }

    // Matches the start of the address, case-sensitive (see Database.searchCampsite).
    // Asks for extra results so hiding the user's own campsites still leaves a full list.
    private suspend fun fetch(text: String): List<CampsiteSummary> {
        val viewer = viewerId
        return Database.searchCampsite(address = text.trim().ifBlank { null }, limit = RESULT_LIMIT + OWN_CAMPSITE_ALLOWANCE)
            .filterNot { viewer != null && it.ownerId == viewer }
            .take(RESULT_LIMIT)
            .map { it.toSummary() }
    }

    private companion object {
        const val SEARCH_DELAY_MS = 300L
        const val RESULT_LIMIT = 20

        /** Extra results fetched to make up for the viewer's own campsites being removed. */
        const val OWN_CAMPSITE_ALLOWANCE = 10
    }
}
