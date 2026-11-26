package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Message
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.DropdownSelectField

/**
 * The admin's Messages tab: pick a campsite owner and an entrant to read their conversation,
 * shown the same way as the chat screen ([ChatMessageList]) with the entrant on the right.
 *
 * @param owners Every owner, for the owner dropdown.
 * @param users Every user, for the entrant dropdown (the picked owner is left out).
 * @param selectedOwner The picked owner, or null.
 * @param selectedEntrant The picked entrant, or null.
 * @param messages The pair's messages, oldest first.
 */
@Composable
fun AdminMessagesScreen(
    owners: List<AdminChatUser>,
    users: List<AdminChatUser>,
    selectedOwner: AdminChatUser?,
    selectedEntrant: AdminChatUser?,
    messages: List<Message>,
    onOwnerSelect: (AdminChatUser) -> Unit,
    onEntrantSelect: (AdminChatUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // Same header as the owner and entrant Messages tabs.
        Text(
            text = "Messages",
            style = AppTextStyles.PageTitleLarge,
            color = AppColors.TextOnPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 26.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 21.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DropdownSelectField(
                modifier = Modifier.weight(1f),
                label = "Campsite owner",
                selectedText = selectedOwner?.name ?: "Select owner",
                options = owners,
                optionLabel = { it.name },
                onSelect = onOwnerSelect,
            )
            DropdownSelectField(
                modifier = Modifier.weight(1f),
                label = "Entrant",
                selectedText = selectedEntrant?.name ?: "Select entrant",
                options = users.filter { it.id != selectedOwner?.id },
                optionLabel = { it.name },
                onSelect = onEntrantSelect,
            )
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (selectedOwner == null || selectedEntrant == null) {
                        "Select both a campsite owner and an entrant to see their messages"
                    } else {
                        "No messages yet"
                    },
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            ChatMessageList(
                messages = messages,
                currentUserId = selectedEntrant?.id, // entrant on the right, owner on the left
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }

        // The app draws the admin's bar over this tab.
        BottomNavBarSpacer()
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AdminMessagesScreenPreview() {
    val owner = AdminChatUser(Id(), "Oliver Hayes")
    val entrant = AdminChatUser(Id(), "Maya")
    AdminMessagesScreen(
        owners = listOf(owner),
        users = listOf(owner, entrant),
        selectedOwner = owner,
        selectedEntrant = entrant,
        messages = listOf(
            Message(Id(), "Hi! Is the lakeside site still free this weekend?", entrant.id),
            Message(Id(), "Hey! Yes, it's open from Friday to Sunday.", owner.id),
        ),
        onOwnerSelect = {},
        onEntrantSelect = {},
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AdminMessagesScreenEmptyPreview() {
    AdminMessagesScreen(
        owners = emptyList(),
        users = emptyList(),
        selectedOwner = null,
        selectedEntrant = null,
        messages = emptyList(),
        onOwnerSelect = {},
        onEntrantSelect = {},
    )
}
