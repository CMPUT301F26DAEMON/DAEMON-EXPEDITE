// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the Register Campsite and Select Location screens from our Figma design", 2026-11-01.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Filter parameters combined from the search bar and filter bottom sheet.
 */
data class FilterState(
    val query: String = "",
    val location: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val guestCount: Int = 1,
)

/**
 * Data for the entrant and admin home screens: the search text and the campsites to show.
 * Each home screen gets its own instance.
 *
 * Loads suggested campsites on start, and searches again whenever search text or filter options change.
 * Also reloads quietly each time the screen is shown again (see [onScreenShown]), because this
 * ViewModel lives as long as the Home screen, which stays at the bottom of the back stack.
 *
 * Campsites owned by the signed-in user are left out (see [setViewer]): you can't book your own
 * campsite, so entrant home doesn't offer it. They still show on the owner home page. Admin home
 * never calls [setViewer], so it lists every campsite, the admin's own included.
 * Searching waits until the user stops typing for [SEARCH_DELAY_MS] so the database is not
 * queried on every key press.
 */
@OptIn(FlowPreview::class)
class HomeViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _location = MutableStateFlow("")
    private val _startdate = MutableStateFlow<LocalDate?>(null)
    private val _enddate = MutableStateFlow<LocalDate?>(null)
    private val _guestCount = MutableStateFlow(1)

    private val _campsites = MutableStateFlow<UiState<List<CampsiteSummary>>>(UiState.Loading)
    val campsites: StateFlow<UiState<List<CampsiteSummary>>> = _campsites.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _query,
                _location,
                _startdate,
                _enddate,
                _guestCount,
            ) { query, location, startDate, endDate, guestCount ->
                FilterState(query, location, startDate, endDate, guestCount)
            }
                .debounce(SEARCH_DELAY_MS)
                .distinctUntilChanged()
                // collectLatest cancels an older search if a newer one starts.
                .collectLatest { search(it) }
        }
    }

    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun onFilterClick(
        location: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        guestCount: Int,
    ) {
        _location.value = location
        _startdate.value = startDate
        _enddate.value = endDate
        _guestCount.value = guestCount
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
            val fresh = loadAsUiState("Couldn't refresh campsites") { fetch(currentFilterState()) }
            if (fresh is UiState.Content || _campsites.value !is UiState.Content) _campsites.value = fresh
        }
    }

    /** Runs the current search again, for example from a "try again" button. */
    fun retry() {
        viewModelScope.launch { search(currentFilterState()) }
    }

    private fun currentFilterState() = FilterState(
        query = _query.value,
        location = _location.value,
        startDate = _startdate.value,
        endDate = _enddate.value,
        guestCount = _guestCount.value,
    )

    private suspend fun search(filter: FilterState) {
        _campsites.value = UiState.Loading
        _campsites.value = loadAsUiState("Couldn't load campsites") { fetch(filter) }
    }

    // Matches title text, location, dates, and max guest occupancy.
    // Asks for extra results so hiding the user's own campsites still leaves a full list.
    private suspend fun fetch(filter: FilterState): List<CampsiteSummary> {
        val viewer = viewerId
        val datesPeriod = if (filter.startDate != null && filter.endDate != null) {
            DatePeriod(filter.startDate, filter.endDate)
        } else null

        return Database
            .searchCampsite(
                location = filter.location.trim().ifBlank { null },
                maxOccupancy = filter.guestCount.takeIf { it > 1 },
                dates = datesPeriod,
                titleText = filter.query.trim().ifBlank { null },
                limit = RESULT_LIMIT + OWN_CAMPSITE_ALLOWANCE,
            )
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
