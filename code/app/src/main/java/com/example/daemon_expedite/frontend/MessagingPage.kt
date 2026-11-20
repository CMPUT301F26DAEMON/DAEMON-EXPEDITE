package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.MessageCardOwner
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.uuid.Uuid

/**
 * UI model representing a chat in the message inbox list with the other user's name, photo, and latest message.
 */
data class ChatUiModel(
    val chatId: Id,
    val otherUserName: String,
    val otherUserPhotoUrl: String?,
    val lastMessageText: String,
    val timestampStr: String,
    val isUnread: Boolean,
)

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

private fun formatMessageTime(instant: Instant): String {
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    return TIME_FORMATTER.format(zonedDateTime)
}

/**
 * ViewModel for the owner's messaging screen.
 * Fetches the latest owner record from the database and loads all associated chats.
 */
class OwnerMessagingViewModel : ViewModel() {
    private val _chats = MutableStateFlow<UiState<List<ChatUiModel>>>(UiState.Loading)
    val chats: StateFlow<UiState<List<ChatUiModel>>> = _chats.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (_chats.value !is UiState.Content) _chats.value = UiState.Loading
            _chats.value =
                loadAsUiState("Couldn't load messages") {
                    coroutineScope {
                        val owner = quietly("Couldn't load owner") { Database.getOwner() }
                        val user = quietly("Couldn't load user") { Database.fetchOwnUser() }
                        val ownerChats = owner?.ownerChats.orEmpty()
                        val userChats = user?.chatIds.orEmpty()
                        val chatIds = (ownerChats + userChats).distinct()

                        chatIds.map { chatId ->
                            async {
                                quietly("Couldn't load chat $chatId") {
                                    val chat = Database.fetchChat(chatId)
                                    val myId = owner?.id ?: user?.id
                                    val otherUserId = if (chat.userA == myId) chat.userB else chat.userA
                                    val userMap = quietly("Couldn't fetch user $otherUserId") { Database.fetchUser(otherUserId) }
                                    val name = userMap?.get("name")?.takeIf { it.isNotBlank() } ?: "Conversation"
                                    val imageId = userMap?.get("profileImage")?.takeIf { it.isNotBlank() }
                                    val photoUrl = imageId?.let { imgId ->
                                        quietly("Couldn't load image record") {
                                            val image = Database.fetchImage(Id(Uuid.parse(imgId)))
                                            Database.getImage(image.path)
                                        }
                                    }
                                    val lastMsg = chat.messages.lastOrNull()
                                    val lastMsgText = lastMsg?.message ?: "No messages"
                                    val timeStr = lastMsg?.let { formatMessageTime(it.id.getTime()) } ?: ""

                                    ChatUiModel(
                                        chatId = chat.id,
                                        otherUserName = name,
                                        otherUserPhotoUrl = photoUrl,
                                        lastMessageText = lastMsgText,
                                        timestampStr = timeStr,
                                        isUnread = false,
                                    )
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                }
        }
    }

    fun retry() {
        load()
    }
}

/**
 * ViewModel for the entrant's messaging screen.
 * Fetches the latest user record from the database and loads all associated chats.
 */
class EntrantMessagingViewModel : ViewModel() {
    private val _chats = MutableStateFlow<UiState<List<ChatUiModel>>>(UiState.Loading)
    val chats: StateFlow<UiState<List<ChatUiModel>>> = _chats.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (_chats.value !is UiState.Content) _chats.value = UiState.Loading
            _chats.value =
                loadAsUiState("Couldn't load messages") {
                    coroutineScope {
                        val user = quietly("Couldn't load user") { Database.fetchOwnUser() }
                        val chatIds = user?.chatIds.orEmpty()

                        chatIds.map { chatId ->
                            async {
                                quietly("Couldn't load chat $chatId") {
                                    val chat = Database.fetchChat(chatId)
                                    val myId = user?.id
                                    val otherUserId = if (chat.userA == myId) chat.userB else chat.userA
                                    val userMap = quietly("Couldn't fetch user $otherUserId") { Database.fetchUser(otherUserId) }
                                    val name = userMap?.get("name")?.takeIf { it.isNotBlank() } ?: "Conversation"
                                    val imageId = userMap?.get("profileImage")?.takeIf { it.isNotBlank() }
                                    val photoUrl = imageId?.let { imgId ->
                                        quietly("Couldn't load image record") {
                                            val image = Database.fetchImage(Id(Uuid.parse(imgId)))
                                            Database.getImage(image.path)
                                        }
                                    }
                                    val lastMsg = chat.messages.lastOrNull()
                                    val lastMsgText = lastMsg?.message ?: "No messages"
                                    val timeStr = lastMsg?.let { formatMessageTime(it.id.getTime()) } ?: ""

                                    ChatUiModel(
                                        chatId = chat.id,
                                        otherUserName = name,
                                        otherUserPhotoUrl = photoUrl,
                                        lastMessageText = lastMsgText,
                                        timestampStr = timeStr,
                                        isUnread = false,
                                    )
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                }
        }
    }

    fun retry() {
        load()
    }
}

/**
 * Screen showing the owner's messages.
 */
@Composable
fun OwnerMessagingPage(
    onMySitesClick: () -> Unit = {},
    onReservationsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLotteriesClick: () -> Unit = {},
    messagesContent: LazyListScope.() -> Unit = {
        // Displays message "No messages yet" if none to display
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No messages yet",
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextMuted,
                )
            }
        }
    },
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AppColors.Background),
    ) {
        Text(
            text = "Messages",
            style = AppTextStyles.PageTitleLarge,
            color = AppColors.TextOnPrimary,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = 28.dp,
                        end = 28.dp,
                        top = 26.dp,
                    ),
        )

        Spacer(modifier = Modifier.height(24.dp))
        // Messages are added here
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(horizontal = 28.dp),
            content = messagesContent,
        )
        // Reusing bottom nav bar
        BottomNavBar(
            labels =
                listOf(
                    "My sites",
                    "Reservations",
                    "Messages",
                    "Notifications",
                    "Lotteries",
                ),
            icons =
                listOf(
                    AppIcons.Home,
                    AppIcons.Reservation,
                    AppIcons.Chat,
                    AppIcons.BellIcon,
                    AppIcons.LotteryTicket,
                ),
            onClicks =
                listOf(
                    onMySitesClick,
                    onReservationsClick,
                    {},
                    onNotificationsClick,
                    onLotteriesClick,
                ),
            selectedIndex = 2,
        )
    }
}

/**
 * Screen showing the entrant's messages.
 */
@Composable
fun EntrantMessagingPage(
    onExploreClick: () -> Unit = {},
    onReservationsClick: () -> Unit = {},
    onLotteriesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    messagesContent: LazyListScope.() -> Unit = {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No messages yet",
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextMuted,
                )
            }
        }
    },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        Text(
            text = "Messages",
            style = AppTextStyles.PageTitleLarge,
            color = AppColors.TextOnPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = 26.dp,
                ),
        )

        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 28.dp),
            content = messagesContent,
        )
        BottomNavBar(
            labels = listOf(
                "Explore",
                "Reservations",
                "Lotteries",
                "Messages",
                "Notifications",
            ),
            icons = listOf(
                AppIcons.Home,
                AppIcons.Reservation,
                AppIcons.LotteryTicket,
                AppIcons.Chat,
                AppIcons.BellIcon,
            ),
            onClicks = listOf(
                onExploreClick,
                onReservationsClick,
                onLotteriesClick,
                {},
                onNotificationsClick,
            ),
            selectedIndex = 3,
        )
    }
}

/**
 * Preview showing the owner's Messaging screen
 * Displays the screen heading and bottom navigation bar.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 405,
    heightDp = 850,
    showSystemUi = true,
)
@Composable
private fun OwnerMessagingPagePreview() {
    OwnerMessagingPage(
        messagesContent = {
            item {
                MessageCardOwner(
                    senderName = "Maya Bennett",
                    lastMessage = "Perfect, we’ll arrive around 3 PM. See you soon!",
                    timestamp = "9:41 AM",
                    isUnread = true,
                    onClick = {},
                )
            }

            item {
                MessageCardOwner(
                    senderName = "Amitej",
                    lastMessage = "Hello, where is the campsite?",
                    isUnread = true,
                    timestamp = "Yesterday",
                    onClick = {},
                )
            }

            item {
                MessageCardOwner(
                    senderName = "Keya",
                    lastMessage = "Hello, does the campsite have a firepit?",
                    isUnread = false,
                    timestamp = "11 Oct",
                    showDivider = false,
                    onClick = {},
                )
            }
        },
    )
}
