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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.LotteryCard
import com.example.daemon_expedite.ui_components.PillVariant
import com.example.daemon_expedite.ui_components.RedButton
import com.example.daemon_expedite.ui_components.ToggleButtonWithContent

/**
 * The entrant's lotteries, with In Progress and Results tabs.
 * Accessible through the Lotteries tab in the shared bottom navigation bar.
 *
 * Same layout as [OwnerLotteriesScreen], without the "Create New Lottery" button. In Progress
 * cards have a "Leave Waiting List" button that asks for confirmation first. Results cards show
 * how the entrant did in the pill ("Lost", "Won · Action Pending", ...). Cards themselves
 * aren't tappable.
 *
 * @param state The entrant's lotteries, or a loading/error state.
 * @param leavingId ID of the lottery currently being left, or null.
 * @param onRetry Called when the user taps an error message to retry loading.
 * @param onLeaveConfirmed Called with the lottery ID after confirming departure.
 */
@Composable
fun EntrantLotteriesScreen(
    state: UiState<EntrantLotteries>,
    leavingId: Id?,
    onRetry: () -> Unit,
    onLeaveConfirmed: (lotteryId: Id) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Id of the lottery the confirm dialog is about, as a String so it survives rotation.
    var askLeave by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
    ) {
        Text(
            text = "My Lotteries",
            style = AppTextStyles.PageTitle,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(
                start = 28.dp,
                top = 24.dp,
                bottom = 20.dp
            )
        )

        ToggleButtonWithContent(
            options = listOf("In Progress", "Results"),
            selectedIndex = tab,
            onOptionSelected = { tab = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp)
                .padding(vertical = 12.dp),
        ) { index ->
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
                is UiState.Content -> if (index == 0) {
                    LotteryList(
                        lotteries = state.data.inProgress,
                        emptyText = "Lotteries you're waiting on will show here. Join one from a campsite page.",
                        onLeaveClick = { id -> if (leavingId == null) askLeave = id.value.toString() },
                        leavingId = leavingId,
                    )
                } else {
                    LotteryList(
                        lotteries = state.data.results,
                        emptyText = "Once winners are drawn, your results will show here.",
                    )
                }
            }
        }

        // Room for the shared bottom bar AppNavigation draws on top.

        BottomNavBarSpacer()

    }

    val leaving = (state as? UiState.Content)?.data?.inProgress?.firstOrNull { it.id.value.toString() == askLeave }
    if (leaving != null) {
        ConfirmDialog(
            title = "Leave the waiting list?",
            message = "You won't be in the draw for ${leaving.title}. " +
                "You can join again while entries are open, if there's still room.",
            confirmText = "Yes, leave waiting list",
            dismissText = "Stay on the list",
            icon = AppIcons.LeaveList,
            onConfirm = {
                askLeave = null
                onLeaveConfirmed(leaving.id)
            },
            onDismiss = { askLeave = null },
        )
    }
}

/**
 * The cards for one tab. [onLeaveClick] adds a "Leave Waiting List" button to each card;
 * null shows none.
 */
@Composable
private fun LotteryList(
    lotteries: List<LotterySummary>,
    emptyText: String,
    onLeaveClick: ((Id) -> Unit)? = null,
    leavingId: Id? = null,
) {
    if (lotteries.isEmpty()) {
        Centered {
            Text(emptyText, style = AppTextStyles.BodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(lotteries, key = { it.id.value.toString() }) { lottery ->
            LotteryCard(
                title = lottery.title,
                status = lottery.status,
                statusVariant = lottery.statusVariant,
                entriesText = lottery.entriesText,
                staysText = lottery.staysText,
                countsText = lottery.countsText,
                onClick = null,
                footer = onLeaveClick?.let { onLeave ->
                    {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            RedButton(
                                onClick = { onLeave(lottery.id) },
                                icon = AppIcons.LeaveList,
                                buttonText = if (lottery.id == leavingId) "Leaving…" else "Leave Waiting List",
                                customHeight = 48,
                                customWidth = maxWidth.value.toInt(),
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun EntrantLotteriesScreenPreview() {
    EntrantLotteriesScreen(
        state = UiState.Content(
            EntrantLotteries(
                inProgress = listOf(
                    LotterySummary(
                        id = Id(),
                        title = "Pine Creek Hideaway + 2 more",
                        status = "Open",
                        statusVariant = PillVariant.Highlight,
                        entriesText = "Entries close 5 Oct",
                        staysText = "16 Oct – 25 Oct · 2 periods · 4 nights",
                        countsText = "12/20 joined · 4 winners",
                    ),
                ),
                results = emptyList(),
            ),
        ),
        leavingId = null,
        onRetry = {},
        onLeaveConfirmed = {},
    )
}
