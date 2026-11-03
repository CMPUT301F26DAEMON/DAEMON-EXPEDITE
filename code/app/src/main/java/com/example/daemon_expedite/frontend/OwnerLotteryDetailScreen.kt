package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsitePill
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.DateRangeCalendar
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.PillVariant
import com.example.daemon_expedite.ui_components.RedButton
import java.time.LocalDate
import java.time.YearMonth

/**
 * The owner's view of one lottery. Opened by tapping a card on My Lotteries.
 *
 * No Figma frame exists; this follows the campsite details pages. Cancel asks first. A lottery
 * that hasn't opened yet is deleted instead of being kept as cancelled.
 *
 * @param state The lottery to show, or loading / error.
 * @param isCancelling True while cancelling; the cancel button ignores taps.
 * @param onCampsiteClick Called with the id of a tapped campsite.
 * @param onViewWaitlistClick Called when "View Waitlist" is tapped.
 * @param onEditClick Called when "Edit lottery" is tapped (only shown before the draw).
 * @param onCancelConfirmed Called after the owner confirms the cancel dialog.
 */
@Composable
fun OwnerLotteryDetailScreen(
    state: UiState<LotteryDetails>,
    isCancelling: Boolean,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onCampsiteClick: (Id) -> Unit,
    onViewWaitlistClick: () -> Unit,
    onEditClick: () -> Unit,
    onCancelConfirmed: () -> Unit,
) {
    var askCancel by rememberSaveable { mutableStateOf(false) }

    DetailPageScaffold("Lottery Details", state = state, onBackClick = onBackClick, onRetry = onRetry) { details ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = details.title,
                style = AppTextStyles.PageTitleMultiline,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            CampsitePill(details.status, variant = details.statusVariant)
        }

        Section("Campsites") {
            details.campsites.forEach { campsite ->
                CampsiteRow(campsite.name, onClick = { onCampsiteClick(campsite.id) })
            }
        }

        Section("Lottery Period") {
            Text(details.entryText, style = AppTextStyles.ValueMedium, color = AppColors.TextPrimary)
            Text(details.phaseNote, style = AppTextStyles.BodySmallMultiline, color = AppColors.TextSecondary)
        }

        Section("Stay Periods") {
            DateRangeCalendar(
                startDate = null,
                endDate = null,
                onRangeChange = null, // read-only
                initialMonth = details.stayPeriods.firstOrNull()?.start?.let(YearMonth::from) ?: YearMonth.now(),
                addedRanges = details.stayPeriods.map { it.start..it.end },
            )
            details.stayTexts.forEach {
                Text(it, style = AppTextStyles.BodySmall, color = AppColors.TextPrimary)
            }
        }

        Section("Draw") {
            Text(details.drawText, style = AppTextStyles.ValueMedium, color = AppColors.TextPrimary)
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GreenButton(onClick = onViewWaitlistClick, icon = AppIcons.Waitlist, buttonText = "View Waitlist", customWidth = width)
                if (details.canEdit) {
                    GreenButton(onClick = onEditClick, icon = AppIcons.EditPencilOutlined, buttonText = "Edit lottery", customWidth = width)
                }
                if (details.canCancel) {
                    RedButton(
                        onClick = { if (!isCancelling) askCancel = true },
                        icon = AppIcons.Cross,
                        buttonText = if (isCancelling) "Cancelling…" else "Cancel lottery",
                        customWidth = width,
                    )
                }
            }
        }

        if (askCancel) {
            ConfirmDialog(
                title = "Cancel lottery?",
                message = if (details.deletesOnCancel) {
                    "It hasn't opened yet, so it will be deleted. Its campsites will be free for a new lottery."
                } else {
                    "Entrants will see it as cancelled and its campsites will be free for a new lottery. " +
                        "This can't be undone."
                },
                confirmText = "Cancel lottery",
                dismissText = "Keep it",
                onConfirm = {
                    askCancel = false
                    onCancelConfirmed()
                },
                onDismiss = { askCancel = false },
            )
        }
    }
}

/** A heading with its content underneath, 12 dp apart. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
        content()
    }
}

/** A campsite in the lottery; tapping it opens the campsite's page. */
@Composable
private fun CampsiteRow(name: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.SurfaceDeep)
            .border(1.dp, AppColors.OutlineSubtle, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.Campsite, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(20.dp))
        Text(
            text = name,
            style = AppTextStyles.ValueEmphasis,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(AppIcons.ArrowForward, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Preview(widthDp = 412, heightDp = 1500)
@Composable
private fun OwnerLotteryDetailScreenPreview() {
    val stays = listOf(
        DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18)),
        DatePeriod(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 25)),
    )
    OwnerLotteryDetailScreen(
        state = UiState.Content(
            LotteryDetails(
                id = Id(),
                title = "Pine Creek Hideaway + 1 more",
                status = "Open",
                statusVariant = PillVariant.Highlight,
                campsites = listOf(CampsiteOption(Id(), "Pine Creek Hideaway"), CampsiteOption(Id(), "Maple Campsite")),
                entryText = "Opens 1 Oct · Closes end of 5 Oct",
                phaseNote = "Entrants can join now. Winners are drawn at the end of 5 Oct.",
                stayPeriods = stays,
                stayTexts = listOf("16 Oct → 18 Oct · 2 nights", "23 Oct → 25 Oct · 2 nights"),
                drawText = "12/20 joined · 4 winners to draw",
                canEdit = true,
                canCancel = true,
                deletesOnCancel = false,
            )
        ),
        isCancelling = false,
        onBackClick = {},
        onRetry = {},
        onCampsiteClick = {},
        onViewWaitlistClick = {},
        onEditClick = {},
        onCancelConfirmed = {},
    )
}
