// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Admin notification log with sent by / received by filters", 2026-11-26.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.DropdownSelectField
import com.example.daemon_expedite.ui_components.NotificationCard

/**
 * The admin's Notifications tab: a log of every notification sent, newest first, which can be
 * narrowed by who sent it and who received it. Each dropdown starts at "All".
 *
 * @param state The filtered log, or loading / error.
 * @param owners Every owner, for the Sent-by dropdown.
 * @param users Every user, for the Received-by dropdown.
 * @param selectedSender The picked sender, or null for all.
 * @param selectedRecipient The picked recipient, or null for all.
 * @param onSenderSelect Called with the picked sender, or null for All.
 * @param onRecipientSelect Called with the picked recipient, or null for All.
 * @param onRetry Called when the error message is tapped.
 */
@Composable
fun AdminNotificationsScreen(
    state: UiState<List<AdminNotificationEntry>>,
    owners: List<AdminChatUser>,
    users: List<AdminChatUser>,
    selectedSender: AdminChatUser?,
    selectedRecipient: AdminChatUser?,
    onSenderSelect: (AdminChatUser?) -> Unit,
    onRecipientSelect: (AdminChatUser?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // Same header as the admin Messages tab.
        Text(
            text = "Notifications",
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
            // null is the "All" option.
            DropdownSelectField(
                modifier = Modifier.weight(1f),
                label = "Sent by",
                selectedText = selectedSender?.name ?: ALL,
                options = listOf(null) + owners,
                optionLabel = { it?.name ?: ALL },
                onSelect = onSenderSelect,
            )
            DropdownSelectField(
                modifier = Modifier.weight(1f),
                label = "Received by",
                selectedText = selectedRecipient?.name ?: ALL,
                options = listOf(null) + users,
                optionLabel = { it?.name ?: ALL },
                onSelect = onRecipientSelect,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (state) {
                UiState.Loading -> CircularProgressIndicator(
                    color = AppColors.Primary,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                )

                is UiState.Error -> CenteredMessage(
                    text = "${state.message}. Tap to try again.",
                    modifier = Modifier.clickable(onClick = onRetry),
                )

                is UiState.Content ->
                    if (state.data.isEmpty()) {
                        CenteredMessage(
                            text = if (selectedSender == null && selectedRecipient == null) {
                                "No notifications have been sent yet"
                            } else {
                                "No notifications match these filters"
                            },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 22.dp),
                        ) {
                            items(state.data, key = { it.id.toString() }) { entry ->
                                NotificationCard(
                                    sender = entry.sender,
                                    title = entry.title,
                                    message = entry.message,
                                    footer = entry.details,
                                    onClick = null, // a log: nothing to open
                                )
                                Spacer(Modifier.height(5.dp))
                                HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
                                Spacer(Modifier.height(5.dp))
                            }
                        }
                    }
            }
        }

        // The app draws the admin's bar over this tab.
        BottomNavBarSpacer()
    }
}

private const val ALL = "All"

@Composable
private fun CenteredMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = AppTextStyles.BodyMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AdminNotificationsScreenPreview() {
    val owner = AdminChatUser(Id(), "Oliver Hayes")
    val entrant = AdminChatUser(Id(), "Maya")
    AdminNotificationsScreen(
        state = UiState.Content(
            listOf(
                AdminNotificationEntry(
                    id = Id(),
                    senderId = owner.id,
                    sender = owner.name,
                    title = "You won the lottery!",
                    message = "You were selected for Pine Lake. Open Expedite to book your stay.",
                    details = "To: 3 entrants · Pine Lake\nLottery result · Nov 26, 2026, 12:01 AM",
                ),
                AdminNotificationEntry(
                    id = Id(),
                    senderId = owner.id,
                    sender = owner.name,
                    title = null,
                    message = "The lake trail is closed this weekend.",
                    details = "To: waiting list · Pine Lake\nOwner message · Nov 24, 2026, 4:12 PM",
                ),
            ),
        ),
        owners = listOf(owner),
        users = listOf(owner, entrant),
        selectedSender = null,
        selectedRecipient = null,
        onSenderSelect = {},
        onRecipientSelect = {},
        onRetry = {},
    )
}
