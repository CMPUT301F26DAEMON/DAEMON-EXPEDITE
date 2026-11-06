package com.example.daemon_expedite.frontend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EntrantInWaitingList(val id: Id, val name: String)

/**
 * Data for the owner's waiting list page: the entrants who joined a lottery, in join order.
 *
 * The lottery id comes from the navigation route (see [Routes.OWNER_LOTTERY_WAITLIST]).
 * Call [load] every time the page is shown, so entrants who joined since show up.
 */
class LotteryWaitingListViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val lotteryId: Id? = savedState.get<String>(Routes.LOTTERY_ID_ARG).toIdOrNull()

    private val _entrants = MutableStateFlow<UiState<List<EntrantInWaitingList>>>(UiState.Loading)
    val entrants: StateFlow<UiState<List<EntrantInWaitingList>>> = _entrants.asStateFlow()

    /** Loads (or reloads) the waiting list. What is on screen stays there while it reloads. */
    fun load() {
        val id = lotteryId ?: run {
            _entrants.value = UiState.Error("Couldn't find this lottery")
            return
        }
        viewModelScope.launch {
            if (_entrants.value !is UiState.Content) _entrants.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load the waiting list") { fetch(id) }
            if (fresh is UiState.Content || _entrants.value !is UiState.Content) _entrants.value = fresh
        }
    }

    private suspend fun fetch(id: Id): List<EntrantInWaitingList> = coroutineScope {
        val lottery = Database.fetchLottery(id)
        lottery.waitingList.users.map { entrantId ->
            async {
                val name = quietly("Couldn't load entrant $entrantId") { Database.fetchUser(entrantId)["name"] }
                EntrantInWaitingList(entrantId, name?.takeIf { it.isNotBlank() } ?: "Unnamed entrant")
            }
        }.awaitAll()
    }
}
