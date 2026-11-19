
package com.example.daemon_expedite.frontend

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.MessageInputBar
import com.example.daemon_expedite.ui_components.MessageCard

/**
 * UI model for a message.
 */
data class UserChatMessageUi(
    val id: String,
    val text: String,
    val time: String,
    val isSentByMe: Boolean
)

@Composable
fun UserMessagesScreen(
    contactName: String = "User Name",
    messages: List<UserChatMessageUi> = emptyList(),
    dateLabel: String = "",
    @DrawableRes avatarResId: Int? = null,
    onBack: () -> Unit = {},
    onSendMessage: (String) -> Unit = {}
) {
    var draft by rememberSaveable(contactName) {
        mutableStateOf("")
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {

        // Header: back button, avatar and name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 20.dp,
                    top = 14.dp,
                    bottom = 20.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BackButton(onClick = onBack)

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceDisabled),
                contentAlignment = Alignment.Center
            ) {

                if (avatarResId != null) {
                    Image(
                        painter = painterResource(avatarResId),
                        contentDescription =
                            "$contactName profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = AppIcons.Profile,
                        contentDescription = null,
                        tint = AppColors.Secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = contactName,
                color = AppColors.TextOnPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Scrollable messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 16.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            // Conversation date
            if (dateLabel.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dateLabel,
                            color = AppColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Reusable message cards
            items(
                items = messages,
                key = { it.id }
            ) { item ->

                MessageCard(
                    message = item.text,
                    time = item.time,
                    isSentByMe = item.isSentByMe
                )
            }
        }

        // Existing MessageBar UI component
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                )
        ) {

            MessageInputBar(
                message = draft,
                onMessageChange = { draft = it },
                onSendClick = { text ->
                    onSendMessage(text)
                    draft = ""
                }
            )
        }
    }
}

/**
 * Interactive Preview
 * Sample messages are only used in Android Studio.
 */
@Preview(
    name = "User Messages - Interactive",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun PreviewUserMessagesScreen() {

    val previewMessages = remember {
        mutableStateListOf(
            UserChatMessageUi(
                id = "1",
                text = "Hi Maya! Your reservation is confirmed for 16–18 Oct. Looking forward to welcoming you to Pine Creek.",
                time = "9:30 AM",
                isSentByMe = false
            ),
            UserChatMessageUi(
                id = "2",
                text = "Thanks, Oliver! We're two guests. What time can we arrive?",
                time = "9:34 AM",
                isSentByMe = true
            ),
            UserChatMessageUi(
                id = "3",
                text = "Check-in is after 2 PM. Quiet hours are 10 PM–7 AM. Use the firepit only and leave no trace.",
                time = "9:38 AM",
                isSentByMe = false
            ),
            UserChatMessageUi(
                id = "4",
                text = "Perfect, we'll arrive around 3 PM. See you soon!",
                time = "9:41 AM",
                isSentByMe = true
            )
        )
    }

    MaterialTheme {
        UserMessagesScreen(
            contactName = "Oliver",
            dateLabel = "12 Oct 2026",
            messages = previewMessages,
            onSendMessage = { newText ->
                previewMessages.add(
                    UserChatMessageUi(
                        id = "preview-${previewMessages.size}",
                        text = newText,
                        time = "Now",
                        isSentByMe = true
                    )
                )
            }
        )
    }
}
