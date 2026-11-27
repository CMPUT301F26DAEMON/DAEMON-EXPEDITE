// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Admin notification log with sent by / received by filters", 2026-11-26.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Notification
import com.example.daemon_expedite.backend.NotificationKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One row of the admin's notification log.
 *
 * @property senderId Who sent it, for the Sent-by filter.
 * @property sender The sender's name, shown in bold.
 * @property details Who it went to, then what kind it is and when, on two lines.
 */
data class AdminNotificationEntry(
    val id: Id,
    val senderId: Id,
    val sender: String,
    val title: String?,
    val message: String,
    val details: String,
)

/**
 * Data for the admin's Notifications tab ([AdminNotificationsScreen]): a log of every
 * notification, newest first, narrowed by who sent it and who received it.
 *
 * Both filters start at "All" (null) and can be used alone or together. Sent by is the
 * notification's sender. Received by is the picked user's `receivedNotifications`, loaded when
 * they are picked: owner messages don't list their recipients, so that is the only record of
 * who got them.
 *
 * Call [load] every time the tab is opened. A failed reload keeps what is shown.
 */
class AdminNotificationsViewModel : ViewModel() {

    private val _all = MutableStateFlow<UiState<List<AdminNotificationEntry>>>(UiState.Loading)

    /** Every owner, for the Sent-by dropdown. */
    private val _owners = MutableStateFlow<List<AdminChatUser>>(emptyList())
    val owners: StateFlow<List<AdminChatUser>> = _owners.asStateFlow()

    /** Every user, for the Received-by dropdown. */
    private val _users = MutableStateFlow<List<AdminChatUser>>(emptyList())
    val users: StateFlow<List<AdminChatUser>> = _users.asStateFlow()

    private val _selectedSender = MutableStateFlow<AdminChatUser?>(null)
    val selectedSender: StateFlow<AdminChatUser?> = _selectedSender.asStateFlow()

    private val _selectedRecipient = MutableStateFlow<AdminChatUser?>(null)
    val selectedRecipient: StateFlow<AdminChatUser?> = _selectedRecipient.asStateFlow()

    /** The picked recipient's received notification ids, or null when Received by is All. */
    private val _receivedIds = MutableStateFlow<UiState<Set<Id>>?>(null)

    /** The log with both filters applied. */
    val notifications: StateFlow<UiState<List<AdminNotificationEntry>>> =
        combine(_all, _selectedSender, _receivedIds) { all, sender, received ->
            filterNotifications(all, sender?.id, received)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    /** Loads the picked recipient's notifications. Replaced when the pick changes. */
    private var recipientJob: Job? = null

    /** Loads (or reloads) the log, the owners and the users. */
    fun load() {
        viewModelScope.launch {
            if (_all.value !is UiState.Content) _all.value = UiState.Loading
            // Names only label rows, so failing to load them doesn't fail the log.
            val usersLoad = async { quietly("Couldn't load users") { Database.fetchAllUsers() }.orEmpty() }
            val ownersLoad = async { quietly("Couldn't load owners") { Database.fetchAllOwnerCampsites() }?.keys.orEmpty() }
            val fresh = loadAsUiState("Couldn't load notifications") {
                val notifications = Database.fetchAllNotifications()
                val users = usersLoad.await()
                    .mapNotNull { user ->
                        val id = user["id"].toIdOrNull() ?: return@mapNotNull null
                        AdminChatUser(id, user["name"]?.takeIf { it.isNotBlank() } ?: "Unnamed user")
                    }.sortedBy { it.name.lowercase() }
                val ownerIds = ownersLoad.await()
                _users.value = users
                _owners.value = users.filter { it.id in ownerIds }
                toEntries(notifications, users.associate { it.id to it.name })
            }
            if (fresh is UiState.Content || _all.value !is UiState.Content) _all.value = fresh
            // A notification sent since the recipient was picked should show up too.
            _selectedRecipient.value?.let(::loadReceived)
        }
    }

    /** Filters by sender; null shows every sender. */
    fun selectSender(sender: AdminChatUser?) {
        _selectedSender.value = sender
    }

    /** Filters by recipient; null shows every recipient. */
    fun selectRecipient(recipient: AdminChatUser?) {
        _selectedRecipient.value = recipient
        if (recipient == null) {
            recipientJob?.cancel()
            _receivedIds.value = null
        } else {
            loadReceived(recipient)
        }
    }

    private fun loadReceived(recipient: AdminChatUser) {
        recipientJob?.cancel()
        _receivedIds.value = UiState.Loading
        recipientJob =
            viewModelScope.launch {
                _receivedIds.value = loadAsUiState("Couldn't load ${recipient.name}'s notifications") {
                    Database.fetchReceivedNotifications(recipient.id).map { it.id }.toSet()
                }
            }
    }

    private suspend fun toEntries(
        notifications: List<Notification>,
        names: Map<Id, String>,
    ): List<AdminNotificationEntry> {
        val places = lotteryHeadings(notifications.map { it.receiversOfLottery })
        return notifications.map { n ->
            AdminNotificationEntry(
                id = n.id,
                senderId = n.senderId,
                sender = names[n.senderId] ?: DELETED_USER,
                title = n.title,
                message = n.text,
                details = recipientsLine(n, places[n.receiversOfLottery], names) + "\n" +
                    kindLabel(n.kind) + " · " + SENT_AT.format(n.id.getTime()),
            )
        }
    }

    private companion object {
        val SENT_AT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US).withZone(ZoneId.systemDefault())
    }
}

internal const val DELETED_USER = "Deleted user"

/**
 * [all] narrowed to notifications from [senderId] (null: anyone) that are in [received]
 * (null: received by anyone). While [received] is loading or failed, so is the result.
 */
internal fun filterNotifications(
    all: UiState<List<AdminNotificationEntry>>,
    senderId: Id?,
    received: UiState<Set<Id>>?,
): UiState<List<AdminNotificationEntry>> {
    if (all !is UiState.Content) return all
    val ids = when (received) {
        null -> null
        is UiState.Content -> received.data
        is UiState.Error -> return received
        UiState.Loading -> return UiState.Loading
    }
    return UiState.Content(
        all.data.filter { (senderId == null || it.senderId == senderId) && (ids == null || it.id in ids) },
    )
}

/**
 * Who [n] went to, for example "To: waiting list · Pine Lake", "To: 3 entrants · Pine Lake" or
 * "To: Maya · Pine Lake". [place] names the lottery, or is null if it can't be found.
 */
internal fun recipientsLine(
    n: Notification,
    place: String?,
    names: Map<Id, String>,
): String {
    val who = when {
        n.kind == NotificationKind.OWNER_MESSAGE -> "waiting list"
        n.recipientIds.size == 1 -> names[n.recipientIds.single()] ?: DELETED_USER
        n.recipientIds.isNotEmpty() -> "${n.recipientIds.size} entrants"
        n.kind == NotificationKind.RESERVATION_CONFIRMED -> "the entrant who booked"
        n.kind == NotificationKind.RESERVATION_CANCELLED -> "the entrant who cancelled"
        else -> "waiting list"
    }
    return "To: $who" + (place?.let { " · $it" } ?: "")
}

/** What kind of notification it is, in words. */
internal fun kindLabel(kind: NotificationKind): String =
    when (kind) {
        NotificationKind.OWNER_MESSAGE -> "Owner message"
        NotificationKind.RESERVATION_CONFIRMED -> "Reservation confirmed"
        NotificationKind.RESERVATION_CANCELLED -> "Reservation cancelled"
        NotificationKind.LOTTERY_RESULT -> "Lottery result"
    }
