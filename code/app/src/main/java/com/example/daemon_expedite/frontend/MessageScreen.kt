package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Message
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.MessageInputBar
import com.example.daemon_expedite.ui_components.ProfileImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

/**
 * ViewModel for [MessageScreen].
 *
 * Supports loading via an existing `chatId` or observing/creating a chat with `userId`.
 * If no chat exists yet, an empty chat is kept locally in memory and is only pushed to
 * Firebase once the first message is sent. Also loads the other user's name and profile photo.
 */
class MessageViewModel(
    savedState: SavedStateHandle,
) : ViewModel() {
    private val chatIdArg: String? = savedState.get<String>("chatId")
    private val userIdArg: String? = savedState.get<String>("userId")

    /**
     * True when the current user opened this chat as the campsite owner (e.g. from a lottery's
     * waiting list). Decides which side of the chat they are on when looking up or creating it.
     */
    private val isOwnerChat: Boolean = savedState.get<Boolean>(Routes.AS_OWNER_ARG) ?: false

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentUserId = MutableStateFlow<Id?>(null)
    val currentUserId: StateFlow<Id?> = _currentUserId.asStateFlow()

    private val _otherUserName = MutableStateFlow("Conversation")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()

    private val _otherUserPhotoUrl = MutableStateFlow<String?>(null)
    val otherUserPhotoUrl: StateFlow<String?> = _otherUserPhotoUrl.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var activeChatId: Id? = null
    private var targetUserId: Id? = null
    private var isNewLocalChat: Boolean = false

    init {
        loadChat()
    }

    private fun loadChat() {
        viewModelScope.launch {
            val user = quietly("Couldn't load user") { Database.fetchOwnUser() }
            val myId = user?.id ?: return@launch
            _currentUserId.value = myId

            val cId = chatIdArg?.toIdOrNull()
            val uId = userIdArg?.toIdOrNull()

            when {
                cId != null -> {
                    openExistingChat(cId, myId)
                }

                uId != null -> {
                    openChatWithUser(uId, myId)
                }

                else -> {}
            }
        }
    }

    private suspend fun loadOtherUser(targetId: Id) {
        val userMap = quietly("Couldn't load other user") { Database.fetchUser(targetId) }
        if (userMap != null) {
            _otherUserName.value = userMap["name"]?.takeIf { it.isNotBlank() } ?: "Conversation"
            val imageId = userMap["profileImage"]?.takeIf { it.isNotBlank() }
            if (imageId != null) {
                _otherUserPhotoUrl.value =
                    quietly("Couldn't load profile picture") {
                        val image = Database.fetchImage(Id(Uuid.parse(imageId)))
                        Database.getImage(image.path)
                    }
            }
        }
    }

    private suspend fun openExistingChat(
        chatId: Id,
        myId: Id,
    ) {
        activeChatId = chatId
        isNewLocalChat = false

        val chat = quietly("Couldn't fetch chat") { Database.fetchChat(chatId) }
        if (chat != null) {
            targetUserId = if (chat.userA == myId) chat.userB else chat.userA
            targetUserId?.let { loadOtherUser(it) }
            _messages.value = chat.messages.reversed()
        }
        observe(chatId)
    }

    private suspend fun openChatWithUser(
        userId: Id,
        myId: Id,
    ) {
        targetUserId = userId
        loadOtherUser(userId)

        val existingChat =
            try {
                // fetchChatWithUserId takes (entrant, owner), so swap when we are the owner.
                if (isOwnerChat) {
                    Database.fetchChatWithUserId(userId, myId)
                } else {
                    Database.fetchChatWithUserId(myId, userId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i(TAG, "No existing chat found with user $userId", e)
                null
            }

        if (existingChat == null) {
            activeChatId = Id()
            isNewLocalChat = true
            _messages.value = emptyList()
        } else {
            activeChatId = existingChat.id
            isNewLocalChat = false
            _messages.value = existingChat.messages.reversed()
            observe(existingChat.id)
        }
    }

    private suspend fun observe(chatId: Id) {
        Database
            .chatListener(chatId)
            .catch { e ->
                if (e is CancellationException) throw e
            }.collect { msgs -> _messages.value = msgs.reversed() }
    }

    fun sendMessage(text: String): Boolean {
        val chatId = activeChatId
        val myId = _currentUserId.value
        val targetId = targetUserId
        if (chatId == null || myId == null || targetId == null) return false
        if (text.isBlank() || _isSending.value) return false

        _isSending.value = true
        viewModelScope.launch {
            val newMessage = Message(Id(), text.trim(), myId)
            try {
                if (isNewLocalChat) {
                    Database.createChatAndUploadMessage(
                        chatId,
                        myId,
                        targetId,
                        newMessage,
                        isSenderOwner = isOwnerChat,
                    )
                    isNewLocalChat = false
                    launch { observe(chatId) }
                } else {
                    Database.uploadMessage(newMessage, chatId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sending message failed", e)
            } finally {
                _isSending.value = false
            }
        }
        return true
    }

    private companion object {
        const val TAG = "MessageViewModel"
    }
}

/**
 * Detailed conversation messaging screen.
 */
@Composable
fun MessageScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessageViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel(),
) {
    val messages by viewModel.messages.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    val otherUserPhotoUrl by viewModel.otherUserPhotoUrl.collectAsState()

    MessageScreenContent(
        messages = messages,
        currentUserId = currentUserId,
        otherUserName = otherUserName,
        otherUserPhotoUrl = otherUserPhotoUrl,
        onBackClick = onBackClick,
        onSend = viewModel::sendMessage,
        modifier = modifier,
    )
}

sealed interface MessageListItem {
    data class DateHeader(
        val dateString: String,
    ) : MessageListItem

    data class MessageItem(
        val message: Message,
    ) : MessageListItem
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatMessageTime(instant: Instant): String {
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    return TIME_FORMATTER.format(zonedDateTime)
}

private fun formatMessageDate(instant: Instant): String {
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val today = LocalDate.now(ZoneId.systemDefault())
    val date = zonedDateTime.toLocalDate()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> DATE_FORMATTER.format(zonedDateTime)
    }
}

private fun buildMessageListItems(messages: List<Message>): List<MessageListItem> {
    val items = mutableListOf<MessageListItem>()
    var lastDate: String? = null

    for (msg in messages) {
        val instant = msg.id.getTime()
        val dateStr = formatMessageDate(instant)
        if (dateStr != lastDate) {
            items.add(MessageListItem.DateHeader(dateStr))
            lastDate = dateStr
        }
        items.add(MessageListItem.MessageItem(msg))
    }
    return items
}

@Composable
fun MessageScreenContent(
    messages: List<Message>,
    currentUserId: Id?,
    otherUserName: String,
    otherUserPhotoUrl: String?,
    onBackClick: () -> Unit,
    onSend: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val listItems = remember(messages) { buildMessageListItems(messages) }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(imeVisible) {
        if (imeVisible && listItems.isNotEmpty()) {
            listState.animateScrollToItem(listItems.size - 1)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
    ) {
        MessageHeader(
            name = otherUserName,
            photoUrl = otherUserPhotoUrl,
            onBackClick = onBackClick,
        )

        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(listItems, key = { item ->
                when (item) {
                    is MessageListItem.DateHeader -> {
                        "date_${item.dateString}"
                    }

                    is MessageListItem.MessageItem -> {
                        item.message.id.value
                            .toString()
                    }
                }
            }) { item ->
                when (item) {
                    is MessageListItem.DateHeader -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.dateString,
                                style = AppTextStyles.Caption,
                                color = AppColors.TextMuted,
                                modifier =
                                    Modifier
                                        .background(AppColors.SurfaceDeep, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    is MessageListItem.MessageItem -> {
                        val msg = item.message
                        val isMe = msg.sender == currentUserId
                        val timeStr = formatMessageTime(msg.id.getTime())

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .widthIn(max = 280.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp,
                                            ),
                                        ).background(if (isMe) AppColors.Primary else AppColors.Surface)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = msg.message,
                                    style = AppTextStyles.Body,
                                    color = if (isMe) AppColors.TextOnPrimary else AppColors.TextPrimary,
                                )
                            }
                            Text(
                                text = timeStr,
                                style = AppTextStyles.Caption.copy(fontSize = 11.sp),
                                color = AppColors.TextMuted,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        MessageInputBar(
            message = inputText,
            onMessageChange = { inputText = it },
            onSendClick = { text ->
                if (onSend(text)) inputText = ""
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun MessageHeader(
    name: String,
    photoUrl: String?,
    onBackClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.CenterStart)) {
            BackButton(onClick = onBackClick)
        }
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileImage(
                imageUrl = photoUrl,
                size = 36.dp,
                contentDescription = name,
            )
            Text(
                text = name.ifBlank { "Conversation" },
                style = AppTextStyles.ScreenTitle,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------

private val previewMe = Id()
private val previewOther = Id()

private val previewMessages =
    listOf(
        Message(Id(), "Hi! Is the lakeside site still free this weekend?", previewMe),
        Message(Id(), "Hey! Yes, it's open from Friday to Sunday.", previewOther),
        Message(Id(), "Great, I'd like to enter the lottery for it.", previewMe),
        Message(
            Id(),
            "Perfect. Entries close Thursday at noon, and winners are drawn that evening.",
            previewOther,
        ),
        Message(Id(), "Thanks!", previewMe),
    )

@Preview(name = "Conversation", showBackground = true, showSystemUi = true)
@Composable
private fun MessageScreenPreview() {
    MessageScreenContent(
        messages = previewMessages,
        currentUserId = previewMe,
        otherUserName = "Maya Bennett",
        otherUserPhotoUrl = null,
        onBackClick = {},
        onSend = { true },
    )
}

@Preview(name = "Empty conversation", showBackground = true, showSystemUi = true)
@Composable
private fun MessageScreenEmptyPreview() {
    MessageScreenContent(
        messages = emptyList(),
        currentUserId = previewMe,
        otherUserName = "Amitej",
        otherUserPhotoUrl = null,
        onBackClick = {},
        onSend = { true },
    )
}
