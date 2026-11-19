package com.example.daemon_expedite.frontend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Notification
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

/**
 * One entrant on a lottery's waiting list, as shown on [LotteryWaitingList].
 *
 * @property id The entrant's user id.
 * @property name The entrant's name, or "Unnamed entrant" if it is blank or couldn't be loaded.
 * @property imageUrl Download URL of the entrant's profile picture, or null to show the default.
 */
data class EntrantInWaitingList(val id: Id, val name: String, val imageUrl: String? = null)

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

    private val _isSending = MutableStateFlow(false)

    /** True while a notification is being sent. */
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    /**
     * Sends a notification to everyone on this lottery's waiting list (see
     * [Database.sendLotteryNotification]). Ignored while a send is already running.
     *
     * @param senderId The owner sending it.
     * @param onResult Called with the saved notification, or null if it couldn't be sent.
     */
    fun sendNotification(
        senderId: Id,
        title: String?,
        text: String,
        onResult: (Notification?) -> Unit,
    ) {
        val id = lotteryId ?: return onResult(null)
        if (_isSending.value) return
        _isSending.value = true
        viewModelScope.launch {
            val sent =
                try {
                    Database.sendLotteryNotification(id, senderId, title, text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("LotteryWaitingListVM", "Couldn't send notification", e)
                    null
                }
            _isSending.value = false
            onResult(sent)
        }
    }

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
                val user = quietly("Couldn't load entrant $entrantId") { Database.fetchUser(entrantId) }
                val name = user?.get("name")?.takeIf { it.isNotBlank() } ?: "Unnamed entrant"
                val imageUrl = user?.get("profileImage")?.takeIf { it.isNotBlank() }?.let { imageId ->
                    quietly("Couldn't load profile picture for entrant $entrantId") { profileImageUrl(imageId) }
                }
                EntrantInWaitingList(entrantId, name, imageUrl)
            }
        }.awaitAll()
    }

    /** Turns a user's profile image record id into a download URL an image loader can show. */
    private suspend fun profileImageUrl(imageId: String): String {
        val image = Database.fetchImage(Id(Uuid.parse(imageId)))
        return Database.getImage(image.path)
    }
}
