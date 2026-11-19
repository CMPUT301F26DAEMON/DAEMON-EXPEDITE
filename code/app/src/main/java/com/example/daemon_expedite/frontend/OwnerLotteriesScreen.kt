// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

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
import com.example.daemon_expedite.ui_components.LotteryCard
import com.example.daemon_expedite.ui_components.PillVariant
import com.example.daemon_expedite.ui_components.ToggleButtonWithContent
import com.example.daemon_expedite.ui_components.YellowButton

/**
 * The owner's lotteries, with Active and Past tabs and a "Create New Lottery" button.
 * Opened from the Lotteries tab of the owner's bottom bar, which AppNavigation draws on top.
 *
 * Based on the "Lotteries · Active" owner frame in the Figma file, with lottery cards in place
 * of the placeholder campsite cards and a Past tab for finished and cancelled lotteries.
 *
 * @param state The lotteries, or loading / error.
 * @param onRetry Called when the error message is tapped.
 * @param onLotteryClick Called with the id of the tapped lottery.
 * @param onCreateClick Called when "Create New Lottery" is tapped.
 */
@Composable
fun OwnerLotteriesScreen(
    state: UiState<OwnerLotteries>,
    onRetry: () -> Unit,
    onLotteryClick: (Id) -> Unit,
    onCreateClick: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding(),
    ) {
        // Top-left title, matching the other bottom bar tabs (e.g. the entrant's My Lotteries).
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
            options = listOf("Active", "Past"),
            selectedIndex = tab,
            onOptionSelected = { tab = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp)
                .padding(top = 12.dp),
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
                is UiState.Content -> LotteryList(
                    lotteries = if (index == 0) state.data.active else state.data.past,
                    emptyText = if (index == 0) {
                        "No active lotteries. Create one to let entrants join."
                    } else {
                        "Finished and cancelled lotteries will show here."
                    },
                    onLotteryClick = onLotteryClick,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp),
        ) {
            YellowButton(
                onClick = onCreateClick,
                icon = AppIcons.AddCircle,
                buttonText = "Create New Lottery",
                customHeight = 58,
                customWidth = maxWidth.value.toInt(),
            )
        }

        // Room for the shared bottom bar AppNavigation draws on top.
        BottomNavBarSpacer()
    }
}

@Composable
private fun LotteryList(
    lotteries: List<LotterySummary>,
    emptyText: String,
    onLotteryClick: (Id) -> Unit,
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
                onClick = { onLotteryClick(lottery.id) },
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
private fun OwnerLotteriesScreenPreview() {
    OwnerLotteriesScreen(
        state = UiState.Content(
            OwnerLotteries(
                active = listOf(
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
                past = emptyList(),
            )
        ),
        onRetry = {},
        onLotteryClick = {},
        onCreateClick = {},
    )
}
