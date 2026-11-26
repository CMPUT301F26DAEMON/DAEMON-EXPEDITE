package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.NamedUser
import com.example.daemon_expedite.backend.OwnerChatPartner
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.DropdownSelectField
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The admin's Messages tab: pick a campsite owner, then an entrant they've chatted with, to
 * read their conversation. The owner's messages are on the left, the entrant's on the right,
 * newest at the bottom. Scrolling to the top loads older messages.
 *
 * @param state What to show (see [AdminMessagesViewModel]).
 * @param onOwnerSelect Called when an owner is picked.
 * @param onEntrantSelect Called when an entrant is picked.
 * @param onRetryOwners Called when the owner list failed and the message is tapped.
 * @param onRetrySelection Called when the entrant list or the messages failed and the message is tapped.
 * @param onLoadOlder Called when the oldest loaded message scrolls into view.
 */
@Composable
fun AdminMessagesScreen(
    state: AdminMessagesState,
    onOwnerSelect: (NamedUser) -> Unit,
    onEntrantSelect: (OwnerChatPartner) -> Unit,
    onRetryOwners: () -> Unit,
    onRetrySelection: () -> Unit,
    onLoadOlder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val owners = (state.owners as? UiState.Content)?.data.orEmpty()
    val entrants = (state.entrants as? UiState.Content)?.data.orEmpty()

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
                selectedText = when (state.owners) {
                    UiState.Loading -> "Loading…"
                    is UiState.Error -> "Couldn't load"
                    is UiState.Content -> state.selectedOwner?.name
                        ?: if (owners.isEmpty()) "No owners" else "Select owner"
                },
                options = owners,
                optionLabel = { it.name },
                onSelect = onOwnerSelect,
            )
            DropdownSelectField(
                modifier = Modifier.weight(1f),
                label = "Entrant",
                selectedText = when (state.entrants) {
                    null -> "Select owner first"
                    UiState.Loading -> "Loading…"
                    is UiState.Error -> "Couldn't load"
                    is UiState.Content -> state.selectedEntrant?.entrant?.name
                        ?: if (entrants.isEmpty()) "No chats" else "Select entrant"
                },
                options = entrants,
                optionLabel = { it.entrant.name },
                onSelect = onEntrantSelect,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val messages = state.messages
            when {
                state.owners is UiState.Error -> ErrorText(state.owners.message, onRetryOwners)
                state.entrants is UiState.Error -> ErrorText(state.entrants.message, onRetrySelection)
                state.selectedOwner != null && state.entrants is UiState.Content && entrants.isEmpty() ->
                    CenteredText("${state.selectedOwner.name} has no conversations yet")
                messages == null ->
                    CenteredText("Select both a campsite owner and an entrant to see their messages")
                messages is UiState.Loading -> Centered { CircularProgressIndicator(color = AppColors.Primary) }
                messages is UiState.Error -> ErrorText(messages.message, onRetrySelection)
                messages is UiState.Content && messages.data.isEmpty() ->
                    CenteredText(
                        "No messages between ${state.selectedOwner?.name} and " +
                            "${state.selectedEntrant?.entrant?.name} yet",
                    )
                messages is UiState.Content ->
                    // Picking another pair starts a fresh list, scrolled to the newest message.
                    key(state.selectedEntrant?.chatId) {
                        MessageList(
                            messages = messages.data,
                            canLoadOlder = state.canLoadOlder,
                            loadingOlder = state.loadingOlder,
                            onLoadOlder = onLoadOlder,
                        )
                    }
            }
        }

        // The app draws the admin's bar over this tab.
        BottomNavBarSpacer()
    }
}

/**
 * The conversation, newest at the bottom.
 *
 * [messages] is newest first, and the list is drawn bottom-up ([LazyColumn]'s reverseLayout),
 * so it opens on the newest message and older pages are simply added to the end of the list.
 */
@Composable
private fun MessageList(
    messages: List<AdminMessageUi>,
    canLoadOlder: Boolean,
    loadingOlder: Boolean,
    onLoadOlder: () -> Unit,
) {
    val listState = rememberLazyListState()
    val latestOnLoadOlder by rememberUpdatedState(onLoadOlder)

    // Asks for older messages when one of the oldest few comes into view (at the top).
    LaunchedEffect(listState, canLoadOlder) {
        if (!canLoadOlder) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_OLDER_THRESHOLD
        }.distinctUntilChanged()
            .filter { it }
            .collect { latestOnLoadOlder() }
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 25.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(items = messages, key = { _, message -> message.id }) { index, message ->
            Column {
                // A date heading above the first (oldest) message of each day. The list is
                // newest first, so the next item is the message just before this one.
                if (index == messages.lastIndex || messages[index + 1].date != message.date) {
                    MessageDateLabel(message.date)
                    Spacer(modifier = Modifier.height(13.dp))
                }
                AdminMessageCard(message = message)
            }
        }
        if (loadingOlder) {
            item(key = "loading_older") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** How close to the oldest loaded message (in items) the next page starts loading. */
private const val LOAD_OLDER_THRESHOLD = 3

@Composable
private fun MessageDateLabel(date: LocalDate) {
    Text(
        text = date.format(
            DateTimeFormatter.ofPattern(
                "d MMM yyyy",
                Locale.ENGLISH
            )
        ).uppercase(Locale.ENGLISH),
        style = AppTextStyles.FieldLabel,
        color = AppColors.Secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AdminMessageCard( //Card for message can be used again
    message: AdminMessageUi
) {val bubbleColor = if (message.fromOwner) {
        AppColors.FillField
    } else {
        AppColors.Primary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromOwner) {
            Arrangement.Start
        } else {
            Arrangement.End
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .border(
                    1.dp,
                    AppColors.OutlineSubtle,
                    RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                )
        ) {

            Text(
                text = message.text,
                style = AppTextStyles.BodyMedium,
                color = if (message.fromOwner) AppColors.TextPrimary else AppColors.TextOnPrimary
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun CenteredText(text: String) = Centered {
    Text(
        text = text,
        style = AppTextStyles.BodyMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ErrorText(message: String, onRetry: () -> Unit) = Centered {
    Text(
        text = "$message. Tap to try again.",
        style = AppTextStyles.BodyMedium,
        color = AppColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.clickable(onClick = onRetry).padding(12.dp),
    )
}

/**
 * Preview sample messages:
 * Owner: Hi
 * Entrant: HI
 */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AdminMessagesScreenPreview() {
    val sampleDate = LocalDate.of(2026, 10, 12)
    val owner = NamedUser(Id(), "Oliver Hayes")
    val entrant = OwnerChatPartner(Id(), NamedUser(Id(), "Maya"))
    AdminMessagesScreen(
        state = AdminMessagesState(
            owners = UiState.Content(listOf(owner, NamedUser(Id(), "Elena Park"))),
            selectedOwner = owner,
            entrants = UiState.Content(listOf(entrant, OwnerChatPartner(Id(), NamedUser(Id(), "Amitej")))),
            selectedEntrant = entrant,
            // Newest first, as the view model gives them.
            messages = UiState.Content(
                listOf(
                    AdminMessageUi(id = "2", text = "HI", date = sampleDate, fromOwner = false),
                    AdminMessageUi(id = "1", text = "Hi", date = sampleDate, fromOwner = true),
                ),
            ),
        ),
        onOwnerSelect = {},
        onEntrantSelect = {},
        onRetryOwners = {},
        onRetrySelection = {},
        onLoadOlder = {},
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AdminMessagesScreenEmptyPreview() {
    AdminMessagesScreen(
        state = AdminMessagesState(owners = UiState.Content(listOf(NamedUser(Id(), "Oliver Hayes")))),
        onOwnerSelect = {},
        onEntrantSelect = {},
        onRetryOwners = {},
        onRetrySelection = {},
        onLoadOlder = {},
    )
}
