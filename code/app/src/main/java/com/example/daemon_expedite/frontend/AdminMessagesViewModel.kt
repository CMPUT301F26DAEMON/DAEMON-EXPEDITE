// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the admin Messages screen", 2026-11-26.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Message
import com.example.daemon_expedite.backend.NamedUser
import com.example.daemon_expedite.backend.OwnerChatPartner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** One message on the admin's Messages screen, ready to show. */
data class AdminMessageUi(
    val id: String,
    val text: String,
    val date: LocalDate,
    /** True for the owner's messages (shown on the left), false for the entrant's (right). */
    val fromOwner: Boolean,
)

/**
 * Everything the admin's Messages screen shows.
 *
 * @property owners Every owner in the app, for the owner dropdown.
 * @property selectedOwner The picked owner, or null until one is picked.
 * @property entrants The picked owner's chats, for the entrant dropdown. Null until an owner is picked.
 * @property selectedEntrant The picked chat, or null until one is picked.
 * @property messages The picked chat's messages, newest first. Null until both are picked.
 * @property canLoadOlder True if the last page was full, so there may be older messages.
 * @property loadingOlder True while an older page is loading.
 */
data class AdminMessagesState(
    val owners: UiState<List<NamedUser>> = UiState.Loading,
    val selectedOwner: NamedUser? = null,
    val entrants: UiState<List<OwnerChatPartner>>? = null,
    val selectedEntrant: OwnerChatPartner? = null,
    val messages: UiState<List<AdminMessageUi>>? = null,
    val canLoadOlder: Boolean = false,
    val loadingOlder: Boolean = false,
)

/**
 * Data for the admin's Messages screen ([AdminMessagesScreen]): pick an owner, then one of the
 * entrants they have chatted with, to read that conversation.
 *
 * Messages load once when the pair is picked (no live updates), 50 at a time; [loadOlder]
 * fetches the 50 before the oldest one shown.
 */
class AdminMessagesViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminMessagesState())
    val state: StateFlow<AdminMessagesState> = _state.asStateFlow()

    /** The oldest message loaded so far, where [loadOlder] continues from. */
    private var oldestMessage: Message? = null

    /** The running entrants or messages load. Picking something new cancels it. */
    private var selectionJob: Job? = null

    init {
        loadOwners()
    }

    /** Loads every owner. Also the retry for the owner list. */
    fun loadOwners() {
        _state.update { it.copy(owners = UiState.Loading) }
        viewModelScope.launch {
            val owners = loadAsUiState("Couldn't load owners") { Database.fetchAllOwners() }
            _state.update { it.copy(owners = owners) }
        }
    }

    /** Picks [owner] and loads the entrants they've chatted with. Clears the picked entrant. */
    fun selectOwner(owner: NamedUser) {
        if (owner.id == _state.value.selectedOwner?.id && _state.value.entrants is UiState.Content) return
        oldestMessage = null
        _state.update {
            it.copy(
                selectedOwner = owner,
                entrants = UiState.Loading,
                selectedEntrant = null,
                messages = null,
                canLoadOlder = false,
                loadingOlder = false,
            )
        }
        selectionJob?.cancel()
        selectionJob =
            viewModelScope.launch {
                val entrants = loadAsUiState("Couldn't load ${owner.name}'s chats") {
                    Database.fetchOwnerChatPartners(owner.id)
                }
                _state.update { it.copy(entrants = entrants) }
            }
    }

    /** Picks [partner] and loads the latest messages between them and the picked owner. */
    fun selectEntrant(partner: OwnerChatPartner) {
        val owner = _state.value.selectedOwner ?: return
        oldestMessage = null
        _state.update {
            it.copy(selectedEntrant = partner, messages = UiState.Loading, canLoadOlder = false, loadingOlder = false)
        }
        selectionJob?.cancel()
        selectionJob =
            viewModelScope.launch {
                var page: List<Message> = emptyList()
                val messages = loadAsUiState("Couldn't load messages") {
                    page = Database.fetchChat(partner.chatId).messages
                    page.map { it.toUi(owner.id) }
                }
                oldestMessage = page.lastOrNull()
                _state.update { it.copy(messages = messages, canLoadOlder = page.size >= PAGE_SIZE) }
            }
    }

    /** Retries whatever failed last: the entrant list, or the messages. */
    fun retrySelection() {
        val current = _state.value
        when {
            current.selectedEntrant != null -> selectEntrant(current.selectedEntrant)
            current.selectedOwner != null -> {
                _state.update { it.copy(entrants = null) } // lets selectOwner run again
                selectOwner(current.selectedOwner)
            }
        }
    }

    /**
     * Loads the 50 messages before the oldest one shown and adds them to the end of the list
     * (the top of the screen). Ignored while a page is loading or when there are no more.
     * A failure keeps what is shown and allows trying again on the next scroll.
     */
    fun loadOlder() {
        val current = _state.value
        val owner = current.selectedOwner ?: return
        val partner = current.selectedEntrant ?: return
        val shown = (current.messages as? UiState.Content)?.data ?: return
        val from = oldestMessage ?: return
        if (!current.canLoadOlder || current.loadingOlder) return
        _state.update { it.copy(loadingOlder = true) }
        selectionJob =
            viewModelScope.launch {
                var page: List<Message> = emptyList()
                val ok = runCatchingAll("Loading older messages failed") {
                    page = Database.fetchChat(partner.chatId, from).messages
                }
                if (ok) oldestMessage = page.lastOrNull() ?: from
                _state.update {
                    it.copy(
                        messages = if (ok) UiState.Content(shown + page.map { m -> m.toUi(owner.id) }) else it.messages,
                        canLoadOlder = if (ok) page.size >= PAGE_SIZE else it.canLoadOlder,
                        loadingOlder = false,
                    )
                }
            }
    }

    private companion object {
        /** How many messages [Database.fetchChat] returns at a time. */
        const val PAGE_SIZE = 50
    }
}

/** Formats a message for the screen. Its date comes from the id, which records when it was made. */
private fun Message.toUi(ownerId: Id) =
    AdminMessageUi(
        id = id.value.toString(),
        text = message,
        date = id.getTime().atZone(ZoneId.systemDefault()).toLocalDate(),
        fromOwner = sender == ownerId,
    )
