package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Stores the campsite filters selected by the entrant.
 *
 * @property location Location entered by the user.
 * @property startDate Beginning of the selected stay.
 * @property endDate End of the selected stay.
 * @property guests Number of guests.
 */
data class CampsiteFilters(
    val location: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val guests: Int = 1
)

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

    private val _filters = MutableStateFlow(CampsiteFilters())
    val filters: StateFlow<CampsiteFilters> = _filters.asStateFlow()

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

    /**
     * Updates the location selected in the filter popup.
     */
    fun onFilterLocationChange(location: String) {
        _filters.value = _filters.value.copy(location = location)
    }

    /**
     * Updates the selected campsite stay dates.
     */
    fun onFilterDateChange(start: LocalDate?, end: LocalDate?) {
        _filters.value = _filters.value.copy(
            startDate = start,
            endDate = end
        )
    }

    /**
     * Increases or decreases the number of guests.
     * The minimum number of guests is one.
     */
    fun onFilterGuestsChange(change: Int) {
        val current = _filters.value

        _filters.value = current.copy(
            guests = (current.guests + change).coerceAtLeast(1)
        )
    }

    /**
     * Resets the filter selections to their default values.
     */
    fun resetFilters() {
        _filters.value = CampsiteFilters()
    }

    /**
     * Applies the selected filters and updates the displayed campsites.
     *
     * Filters campsites by location, stay dates, and guest capacity.
     * Campsites owned by the signed-in user are excluded.
     */
    fun applyFilters() {
        viewModelScope.launch {
            val selectedFilters = _filters.value

            _campsites.value = UiState.Loading

            _campsites.value = loadAsUiState("Couldn't filter campsites") {
                val viewer = viewerId

                Database.filterCampsites(
                    location = selectedFilters.location.ifBlank { null },
                    startDate = selectedFilters.startDate,
                    endDate = selectedFilters.endDate,
                    guests = selectedFilters.guests
                )
                    .filterNot { viewer != null && it.ownerId == viewer }
                    .map { it.toSummary() }
            }
        }
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
        return Database
            .searchCampsite(address = text.trim().ifBlank { null }, limit = RESULT_LIMIT + OWN_CAMPSITE_ALLOWANCE)
            .campsites
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