// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// Updated with Claude Code, "Update the lottery details page to match our Figma design", 2026-11-22.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsiteLinkCard
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
 * Built from the "Lottery Details · Owner" frame in the Figma file. Cancel (not in the design)
 * asks first. A lottery that hasn't opened yet is deleted instead of being kept as cancelled.
 *
 * For a co-owner ([LotteryDetails.isCollaborator]) it follows the "Lottery Details · Collaborator"
 * frame instead: a Collaborator pill and who invited them, and no editing or cancelling.
 *
 * @param state The lottery to show, or loading / error.
 * @param isCancelling True while cancelling; the cancel button ignores taps.
 * @param onCampsiteClick Called with the id of a tapped campsite.
 * @param onViewWaitlistClick Called when "View Waitlist" is tapped.
 * @param onEditClick Called when the pencil in the header is tapped (only shown before the draw).
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
    onShowQrCodeClick: () -> Unit = {},
    onCancelConfirmed: () -> Unit,
) {
    var askCancel by rememberSaveable { mutableStateOf(false) }
    val canEdit = (state as? UiState.Content)?.data?.canEdit == true

    DetailPageScaffold(
        "Lottery Details",
        state = state,
        onBackClick = onBackClick,
        onRetry = onRetry,
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderIconButton(AppIcons.QRCode, "Show QR code", onShowQrCodeClick)
                if (canEdit) {
                    HeaderIconButton(AppIcons.EditPencilOutlined, "Edit lottery", onEditClick)
                }
            }
        },
    ) { details ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(details.title, style = AppTextStyles.PageTitleMultiline, color = AppColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampsitePill(details.status, variant = details.statusVariant)
                if (details.isCollaborator) CampsitePill("Collaborator", variant = PillVariant.Outlined)
            }
            if (details.isCollaborator) details.invitedByText?.let { InvitedByText(it) }
        }

        LotteryInfoSections(details, onCampsiteClick)

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GreenButton(
                    onClick = onViewWaitlistClick,
                    icon = AppIcons.Waitlist,
                    buttonText = "View Waitlist",
                    buttonFontSize = 16,
                    customHeight = 58,
                    customWidth = width,
                )
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
                confirmText = "Yes, cancel lottery",
                dismissText = "Keep it",
                icon = AppIcons.CancelCircle,
                onConfirm = {
                    askCancel = false
                    onCancelConfirmed()
                },
                onDismiss = { askCancel = false },
            )
        }
    }
}

/**
 * The page where an owner answers an invite to add their campsites to another owner's lottery.
 * Built from the "Lottery Request · Approval Pending" frame in the Figma file: one approve card
 * per invited campsite, then the lottery's details. Invited campsites are listed as pending.
 *
 * @param state The lottery, formatted for the invited owner, or loading / error.
 * @param answering The campsite being approved or declined, or null. Buttons ignore taps meanwhile.
 * @param onAnswer Called with a campsite and true to approve it, false to decline it.
 */
@Composable
fun OwnerLotteryRequestScreen(
    state: UiState<LotteryDetails>,
    answering: Id?,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onCampsiteClick: (Id) -> Unit,
    onAnswer: (campsite: Id, approve: Boolean) -> Unit,
) {
    DetailPageScaffold("Lottery Request", state = state, onBackClick = onBackClick, onRetry = onRetry) { details ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(details.title, style = AppTextStyles.PageTitleMultiline, color = AppColors.TextPrimary)
            CampsitePill("Approval pending", variant = PillVariant.Outlined)
        }

        details.pendingCampsites.forEach { campsite ->
            ApproveInviteCard(
                campsiteName = campsite.name,
                stayRangeText = details.stayRangeText,
                onDecline = { if (answering == null) onAnswer(campsite.id, false) },
                onApprove = { if (answering == null) onAnswer(campsite.id, true) },
            )
        }

        LotteryInfoSections(details, onCampsiteClick)
    }
}

/**
 * Campsites (the viewer's pending ones dimmed at the end), Lottery Period, Stay Periods and
 * Draw: the sections every owner's lottery page shows, 24 dp apart.
 */
@Composable
private fun LotteryInfoSections(details: LotteryDetails, onCampsiteClick: (Id) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Section("Campsites") {
            details.campsites.forEach { campsite ->
                CampsiteLinkCard(campsite.name, onClick = { onCampsiteClick(campsite.id) })
            }
            details.pendingCampsites.forEach { campsite ->
                CampsiteLinkCard(campsite.name, onClick = { onCampsiteClick(campsite.id) }, pendingText = "Pending approval")
            }
        }

        Section("Lottery Period") {
                Text(
                    details.entryText,
                    style = AppTextStyles.BodyMedium.copy(lineHeight = 21.sp),
                    color = AppColors.TextPrimary,
                )
                Text(
                    details.phaseNote,
                    style = AppTextStyles.BodySmall.copy(lineHeight = 19.sp),
                    color = AppColors.TextMuted,
                )
            }

            Section("Stay Periods") {
                DateRangeCalendar(
                    startDate = null,
                    endDate = null,
                    onRangeChange = null, // read-only
                    initialMonth = details.stayPeriods.firstOrNull()?.start?.let(YearMonth::from) ?: YearMonth.now(),
                    addedRanges = details.stayPeriods.map { it.start..it.end },
                    addedRangesColor = AppColors.Primary,
                    footer = if (details.stayTexts.isEmpty()) {
                        null
                    } else {
                        {
                            details.stayTexts.forEach {
                                Text("Stay nights: $it", style = AppTextStyles.CaptionMultiline, color = AppColors.Secondary)
                            }
                        }
                    },
                )
            }

            Section("Draw") {
                DrawFactsCard(joinedText = details.joinedText, winnerCount = details.winnerCount)
                details.drawResultText?.let {
                    Text(it, style = AppTextStyles.BodySmall, color = AppColors.TextMuted)
                }
            }
    }
}

/** "Invited by Alex Chen", under the pills of a lottery another owner created. */
@Composable
private fun InvitedByText(text: String) {
    Text(text, style = AppTextStyles.BodySmall.copy(lineHeight = 19.sp), color = AppColors.TextMuted)
}

/**
 * "Approve Maple Campsite?" with Decline and Approve buttons (the "Expedite/Card/Lottery
 * Request · Approval Pending" component in Figma).
 */
@Composable
private fun ApproveInviteCard(
    campsiteName: String,
    stayRangeText: String,
    onDecline: () -> Unit,
    onApprove: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AppColors.OutlineButton, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Approve $campsiteName?", style = AppTextStyles.TitleMedium, color = AppColors.TextPrimary)
        Text(
            "You’ve been invited to add your campsite to this lottery for $stayRangeText.",
            style = AppTextStyles.BodySmallMultiline,
            color = AppColors.TextMuted,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 10
            val totalWidth = maxWidth.value.toInt()
            val declineWidth = 162.coerceAtMost((totalWidth - gap) / 2)
            Row(horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
                RedButton(
                    onClick = onDecline,
                    buttonText = "Decline",
                    customHeight = 40,
                    customWidth = declineWidth,
                    buttonFontSize = 15,
                )
                GreenButton(
                    onClick = onApprove,
                    buttonText = "Approve",
                    buttonFontSize = 15,
                    customHeight = 40,
                    customWidth = totalWidth - declineWidth - gap,
                )
            }
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

/** Round 40 dp header button, styled like the back button (the "Edit action" in Figma). */
@Composable
private fun HeaderIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AppColors.FillSubtle)
            .border(1.dp, AppColors.OutlineSubtle, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = AppColors.TextPrimary, modifier = Modifier.size(20.dp))
    }
}

/** Waiting list size on the left, number of winners on the right (the "Draw facts" frame). */
@Composable
private fun DrawFactsCard(joinedText: String, winnerCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDeep)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(AppIcons.Guests, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("WAITING LIST", style = AppTextStyles.Overline, color = AppColors.Secondary)
                Text(joinedText, style = AppTextStyles.ValueEmphasis, color = AppColors.TextPrimary)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(winnerCount.toString(), style = AppTextStyles.Amount, color = AppColors.TextPrimary)
            Text(
                if (winnerCount == 1) "winner" else "winners",
                style = AppTextStyles.Caption,
                color = AppColors.TextPrimary,
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 1500)
@Composable
private fun OwnerLotteryDetailScreenPreview() {
    val stays = listOf(
        DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18)),
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
                phaseNote = "Entrants can join while entries are open. Winners are drawn at the end of 5 Oct.",
                stayPeriods = stays,
                stayTexts = listOf("16 Oct → 18 Oct · 2 nights"),
                drawText = "12/20 joined · 4 winners to draw",
                joinedText = "12 / 20 joined",
                winnerCount = 4,
                drawResultText = null,
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
