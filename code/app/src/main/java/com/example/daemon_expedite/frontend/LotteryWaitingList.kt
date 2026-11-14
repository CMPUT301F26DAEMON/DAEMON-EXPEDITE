package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.EntrantCard
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.OwnerNotificationPopup
import com.example.daemon_expedite.ui_components.ScreenHeader

/**
 * The owner's view of a lottery's waiting list. Opened by tapping "View Waitlist" on the
 * owner's lottery details page.
 *
 * Shows a spinner while loading, a tappable error message if loading fails, a placeholder
 * if no one has joined yet, and otherwise one [EntrantCard] per entrant.
 *
 * @param state The entrants, in join order, or loading / error.
 * @param onBackClick Called when the header's back button is tapped.
 * @param onRetry Called when the error message is tapped.
 * @param onMessageClick Called when the message button on any entrant's card is tapped.
 * @param isSendingNotification True while a notification is being sent. Disables the popup's button.
 * @param onSendNotification Called when the owner sends from the popup, with the title (null if
 *   blank), the message, and a callback to report back: pass true to close the popup, false
 *   to keep it open (with the owner's text) so they can try again. The "Send Notification"
 *   button that opens the popup is only shown when the waiting list has at least one entrant.
 */
@Composable
fun LotteryWaitingList(
    state: UiState<List<EntrantInWaitingList>>,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onMessageClick: () -> Unit = {},
    isSendingNotification: Boolean = false,
    onSendNotification: (title: String?, text: String, onResult: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
){
    var showNotificationPopup by rememberSaveable { mutableStateOf(false) }
    if (showNotificationPopup) {
        OwnerNotificationPopup(
            onDismiss = { showNotificationPopup = false },
            isSending = isSendingNotification,
            onSend = { title, text ->
                onSendNotification(title, text) { sent -> if (sent) showNotificationPopup = false }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ){
        ScreenHeader(title = "Waiting List", onBackClick = onBackClick)
        when (state) {
            UiState.Loading -> Centered { CircularProgressIndicator(color = AppColors.Primary) }

            is UiState.Error -> Centered {
                Text(
                    text = "${state.message}. Tap to try again.",
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(onClick = onRetry).padding(12.dp),
                )
            }

            is UiState.Content -> if (state.data.isEmpty()) {
                Centered {
                    Text(
                        text = "No one has joined yet",
                        style = AppTextStyles.BodyMedium,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // weight(1f) lets the list take only the space left over after the button,
                // instead of fillMaxSize() which would push the button off-screen.
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.data, key = { it.id.value.toString() }) { entrant ->
                        EntrantCard(
                            name = entrant.name,
                            imageUrl = entrant.imageUrl,
                            onMessageClick = onMessageClick,
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    GreenButton(
                        onClick = { showNotificationPopup = true },
                        icon = AppIcons.BellIcon,
                        buttonText = "Send Notification",
                        customWidth = maxWidth.value.toInt(),
                    )
                }
            }

        }
    }

}

/** Centers [content] in the space below the header, for the loading, error and empty states. */
@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/** Preview of the waiting list with two entrants. */
@Preview
@Composable
fun PreviewLotteryWaitingList(){
    LotteryWaitingList(
        state = UiState.Content(
            listOf(
                EntrantInWaitingList(Id(), "Keya Malhotra"),
                EntrantInWaitingList(Id(), "John Doe")
            )
        ),
        onBackClick = {},
        onRetry = {},
    )
}
