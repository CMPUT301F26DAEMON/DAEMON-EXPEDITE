package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.Notification
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Data for [NotificationsScreen]: the signed-in user's notifications, newest first.
 *
 * Call [load] every time the page is opened. It shows the notifications with a green dot on
 * the ones that were unread, then marks them all read in the database. The dots stay for this
 * visit and are gone the next time the page is opened.
 */
class NotificationsViewModel : ViewModel() {

    private val _notifications = MutableStateFlow<UiState<List<NotificationEntry>>>(UiState.Loading)
    val notifications: StateFlow<UiState<List<NotificationEntry>>> = _notifications.asStateFlow()

    /** Loads [userId]'s notifications, then marks the unread ones as read. */
    fun load(userId: Id) {
        viewModelScope.launch {
            if (_notifications.value !is UiState.Content) _notifications.value = UiState.Loading
            var unread: List<Id> = emptyList()
            val fresh = loadAsUiState("Couldn't load your notifications") {
                val loaded = Database.fetchReceivedNotifications(userId)
                unread = loaded.filterNot { it.isRead }.map { it.id }
                toEntries(loaded)
            }
            // Keep what's on screen if a reload fails.
            if (fresh is UiState.Content || _notifications.value !is UiState.Content) _notifications.value = fresh

            if (unread.isNotEmpty()) {
                quietly("Couldn't mark notifications as read") { Database.markNotificationsRead(userId, unread) }
            }
        }
    }

    /**
     * Formats [notifications] for display. The heading is the lottery's campsites, for example
     * "Pine Creek Hideaway and 2 more". Each lottery and campsite is looked up once. If a
     * lottery's campsites can't be found (deleted, or failed to load), the owner's name is
     * shown instead.
     */
    private suspend fun toEntries(notifications: List<Notification>): List<NotificationEntry> = coroutineScope {
        val lotteries: Map<Id, Lottery> =
            notifications
                .map { it.receiversOfLottery }
                .distinct()
                .map { id -> async { id to quietly("Couldn't load lottery $id") { Database.fetchLottery(id) } } }
                .awaitAll()
                .mapNotNull { (id, lottery) -> lottery?.let { id to it } }
                .toMap()

        val campsiteNames: Map<Id, String> =
            lotteries.values
                .flatMap { it.campsites }
                .distinct()
                .map { id -> async { id to quietly("Couldn't load campsite $id") { Database.fetchCampsite(id).name } } }
                .awaitAll()
                .mapNotNull { (id, name) -> name?.takeIf { it.isNotBlank() }?.let { id to it } }
                .toMap()

        fun headingFor(n: Notification): String? =
            lotteries[n.receiversOfLottery]
                ?.campsites
                ?.mapNotNull { campsiteNames[it] }
                ?.let(::campsitesHeading)

        // Only look up owner names for notifications that need the fallback.
        val ownerNames: Map<Id, String> =
            notifications
                .filter { headingFor(it) == null }
                .map { it.senderId }
                .distinct()
                .map { id -> async { id to quietly("Couldn't load sender $id") { Database.fetchUser(id)["name"] } } }
                .awaitAll()
                .mapNotNull { (id, name) -> name?.takeIf { it.isNotBlank() }?.let { id to it } }
                .toMap()

        val now = Instant.now()
        notifications.map { n ->
            NotificationEntry(
                sender = headingFor(n) ?: ownerNames[n.senderId] ?: "Campsite owner",
                message = n.text,
                timestamp = relativeTime(n.id.getTime(), now),
                id = n.id,
                title = n.title,
                isNew = !n.isRead,
            )
        }
    }
}

/**
 * "Pine Creek" for one campsite, "Pine Creek and 2 more" for three, or null for none.
 */
internal fun campsitesHeading(names: List<String>): String? =
    when (names.size) {
        0 -> null
        1 -> names.single()
        else -> "${names.first()} and ${names.size - 1} more"
    }

private val DATE = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/** "Just now", "5m ago", "3h ago", "2d ago", then a date like "Nov 2". */
internal fun relativeTime(time: Instant, now: Instant): String {
    val elapsed = Duration.between(time, now)
    return when {
        elapsed.toMinutes() < 1 -> "Just now"
        elapsed.toHours() < 1 -> "${elapsed.toMinutes()}m ago"
        elapsed.toDays() < 1 -> "${elapsed.toHours()}h ago"
        elapsed.toDays() < 7 -> "${elapsed.toDays()}d ago"
        else -> DATE.format(time.atZone(ZoneId.systemDefault()))
    }
}
