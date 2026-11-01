package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
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

    /** Runs the current search again, for example from a "try again" button. */
    fun retry() {
        viewModelScope.launch { search(_query.value) }
    }

    private suspend fun search(text: String) {
        _campsites.value = UiState.Loading
        _campsites.value = loadAsUiState("Couldn't load campsites") {
            // Matches the start of the address, case-sensitive (see Database.searchCampsite).
            Database.searchCampsite(address = text.trim().ifBlank { null }, limit = RESULT_LIMIT)
                .map { it.toSummary() }
        }
    }

    private companion object {
        const val SEARCH_DELAY_MS = 300L
        const val RESULT_LIMIT = 20
    }
}
