// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the admin Messages screen", 2026-11-26.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** A user in the admin's Messages dropdowns. */
data class AdminChatUser(val id: Id, val name: String)

/**
 * Data for the admin's Messages tab ([AdminMessagesScreen]): pick a campsite owner and an
 * entrant to read their conversation.
 *
 * Like the chat screen, it shows the latest 50 messages and updates live. Failures are only
 * logged; the screen then shows empty lists.
 */
class AdminMessagesViewModel : ViewModel() {

    /** Every owner, for the owner dropdown. */
    private val _owners = MutableStateFlow<List<AdminChatUser>>(emptyList())
    val owners: StateFlow<List<AdminChatUser>> = _owners.asStateFlow()

    /** Every user, for the entrant dropdown. */
    private val _users = MutableStateFlow<List<AdminChatUser>>(emptyList())
    val users: StateFlow<List<AdminChatUser>> = _users.asStateFlow()

    private val _selectedOwner = MutableStateFlow<AdminChatUser?>(null)
    val selectedOwner: StateFlow<AdminChatUser?> = _selectedOwner.asStateFlow()

    private val _selectedEntrant = MutableStateFlow<AdminChatUser?>(null)
    val selectedEntrant: StateFlow<AdminChatUser?> = _selectedEntrant.asStateFlow()

    /** The picked pair's messages, oldest first. Empty until both are picked. */
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /** Watches the picked pair's chat. Replaced when either pick changes. */
    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            val users = quietly("Couldn't load users") { Database.fetchAllUsers() }.orEmpty()
                .mapNotNull { user ->
                    val id = user["id"].toIdOrNull() ?: return@mapNotNull null
                    AdminChatUser(id, user["name"]?.takeIf { it.isNotBlank() } ?: "Unnamed user")
                }.sortedBy { it.name.lowercase() }
            val ownerIds = quietly("Couldn't load owners") { Database.fetchAllOwnerCampsites() }?.keys.orEmpty()
            _users.value = users
            _owners.value = users.filter { it.id in ownerIds }
        }
    }

    fun selectOwner(owner: AdminChatUser) {
        _selectedOwner.value = owner
        if (_selectedEntrant.value?.id == owner.id) _selectedEntrant.value = null // can't chat with themselves
        watchChat()
    }

    fun selectEntrant(entrant: AdminChatUser) {
        _selectedEntrant.value = entrant
        watchChat()
    }

    /** Shows the picked pair's chat live, the same way the chat screen does. */
    private fun watchChat() {
        chatJob?.cancel()
        _messages.value = emptyList()
        val owner = _selectedOwner.value ?: return
        val entrant = _selectedEntrant.value ?: return
        chatJob =
            viewModelScope.launch {
                // No chat yet (or it can't be read): nothing to show.
                val chat = quietly("No chat between ${entrant.name} and ${owner.name}") {
                    Database.fetchChatWithUserId(entrant.id, owner.id)
                } ?: return@launch
                _messages.value = chat.messages.reversed()
                Database.chatListener(chat.id)
                    .catch { e -> Log.e(TAG, "Watching chat ${chat.id.value} failed", e) } // keeps what is shown
                    .collect { _messages.value = it.reversed() }
            }
    }

    private companion object {
        const val TAG = "AdminMessagesViewModel"
    }
}
