package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Where the viewer stands with a campsite's lottery. Picks the look of [LotteryStatusCard]. */
enum class LotteryStatus {
    /** A lottery is open and the viewer hasn't joined. Shows "Join Lottery!". */
    OPEN,

    /** The viewer is on the waiting list. Shows "Leave waiting list". */
    JOINED,

    /** No lottery is open for this campsite. Shows a disabled button. */
    NONE,
}

/**
 * Card on the campsite details page that shows the lottery state and the matching action.
 *
 * Built from the "Expedite/Card/Lottery" frames in the Figma file (join, joined and none).
 *
 * @param status Which of the three states to show.
 * @param modifier [Modifier] applied to the card.
 * @param endsText When the lottery closes, already formatted, for example
 *   "12 Oct 2026 · 6:00 PM PT". Ignored for [LotteryStatus.NONE].
 * @param onJoinClick Called when "Join Lottery!" is tapped ([LotteryStatus.OPEN]).
 * @param onLeaveClick Called when "Leave waiting list" is tapped ([LotteryStatus.JOINED]).
 */
@Composable
fun LotteryStatusCard(
    status: LotteryStatus,
    modifier: Modifier = Modifier,
    endsText: String? = null,
    onJoinClick: () -> Unit = {},
    onLeaveClick: () -> Unit = {},
) {
    val ends = endsText?.let { "Lottery ends $it" }
    when (status) {
        LotteryStatus.OPEN -> CardFrame(
            border = AppColors.Accent,
            icon = AppIcons.LotteryTicket,
            title = "Your next adventure awaits",
            subtitle = ends,
            modifier = modifier,
        ) { width ->
            YellowButton(onClick = onJoinClick, icon = AppIcons.LotteryTicket, buttonText = "Join Lottery!", customWidth = width)
        }

        LotteryStatus.JOINED -> CardFrame(
            border = AppColors.Danger,
            icon = AppIcons.CircleCheck,
            title = "You’re on the waiting list",
            subtitle = ends,
            modifier = modifier,
        ) { width ->
            RedButton(onClick = onLeaveClick, icon = AppIcons.LeaveList, buttonText = "Leave waiting list", customWidth = width)
        }

        LotteryStatus.NONE -> CardFrame(
            border = AppColors.OutlineStrong,
            icon = AppIcons.Clock,
            title = "No lottery is open",
            subtitle = "The owner hasn't opened a lottery yet.",
            modifier = modifier,
        ) {
            DisabledButton("No Active Lottery Yet")
        }
    }
}

/**
 * Shared layout of the three cards: icon and title, a line of detail, then a full-width button.
 * [button] gets the width available inside the card, since the shared buttons take a width in dp.
 */
@Composable
private fun CardFrame(
    border: Color,
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier,
    button: @Composable (width: Int) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(BorderStroke(1.dp, border), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(20.dp))
            Text(title, style = AppTextStyles.CardTitle, color = AppColors.TextPrimary)
        }
        if (subtitle != null) {
            Text(subtitle, style = AppTextStyles.BodySmallMultiline, color = AppColors.Secondary)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            button(maxWidth.value.toInt())
        }
    }
}

@Composable
private fun DisabledButton(text: String) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(AppColors.SurfaceDisabled)
            .border(BorderStroke(1.dp, AppColors.OutlineStrong), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTextStyles.ButtonLabel, color = AppColors.Secondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun LotteryStatusCardPreview() {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LotteryStatusCard(LotteryStatus.OPEN, endsText = "12 Oct 2026 · 6:00 PM PT")
        LotteryStatusCard(LotteryStatus.JOINED, endsText = "12 Oct 2026 · 6:00 PM PT")
        LotteryStatusCard(LotteryStatus.NONE)
    }
}
