// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the campsite details, policies and map screens from our Figma design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

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

    /** A lottery is open but its waiting list is full. Shows a disabled button. */
    FULL,

    /** A lottery has been set up but entries haven't opened yet. Shows a disabled button. */
    UPCOMING,

    /** Entries closed and the viewer joined; winners are being drawn. Shows a disabled button. */
    DRAWING,

    /** The viewer was drawn as a winner. Shows "Book your stay". */
    WON,

    /** The viewer joined but wasn't drawn. Shows a disabled button. */
    LOST,

    /** The viewer won and booked their stay. Shows what they booked. */
    BOOKED,

    /** The viewer won but turned it down. Shows a disabled button. */
    DECLINED,

    /** The viewer won but didn't book before the first night, so the win lapsed. */
    MISSED,

    /** No lottery is open for this campsite. Shows a disabled button. */
    NONE,
}

/**
 * Card on the campsite details page that shows the lottery state and the matching action.
 *
 * Built from the "Expedite/Card/Lottery" frames in the Figma file (join, joined and none),
 * plus full and upcoming states in the same style as "none".
 *
 * @param status Which state to show.
 * @param modifier [Modifier] applied to the card.
 * @param endsText When entries close, already formatted, for example "12 Oct 2026 · 11:59 PM MDT".
 *   Used for [LotteryStatus.OPEN], [LotteryStatus.JOINED] and [LotteryStatus.FULL].
 * @param opensText When entries open, for example "1 Oct". Used for [LotteryStatus.UPCOMING].
 * @param bookByText Booking deadline (the first stay night), for example "16 Oct". Used for
 *   [LotteryStatus.WON].
 * @param bookingText What the viewer booked, for example "Pine Creek · 16 Oct → 18 Oct · 2 nights".
 *   Used for [LotteryStatus.BOOKED].
 * @param onBookClick Called when "Book your stay" is tapped ([LotteryStatus.WON]).
 * @param busy True while joining or leaving: the button shows "…" and ignores taps.
 * @param onJoinClick Called when "Join Lottery!" is tapped ([LotteryStatus.OPEN]).
 * @param onLeaveClick Called when "Leave waiting list" is tapped ([LotteryStatus.JOINED]).
 */
@Composable
fun LotteryStatusCard(
    status: LotteryStatus,
    modifier: Modifier = Modifier,
    endsText: String? = null,
    opensText: String? = null,
    bookByText: String? = null,
    bookingText: String? = null,
    busy: Boolean = false,
    onJoinClick: () -> Unit = {},
    onLeaveClick: () -> Unit = {},
    onBookClick: () -> Unit = {},
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
            YellowButton(
                onClick = { if (!busy) onJoinClick() },
                icon = AppIcons.LotteryTicket,
                buttonText = if (busy) "Joining…" else "Join Lottery!",
                customWidth = width,
            )
        }

        LotteryStatus.JOINED -> CardFrame(
            border = AppColors.Danger,
            icon = AppIcons.CircleCheck,
            title = "You’re on the waiting list",
            subtitle = ends,
            modifier = modifier,
        ) { width ->
            RedButton(
                onClick = { if (!busy) onLeaveClick() },
                icon = AppIcons.LeaveList,
                buttonText = if (busy) "Leaving…" else "Leave waiting list",
                customWidth = width,
            )
        }

        LotteryStatus.FULL -> CardFrame(
            border = AppColors.OutlineStrong,
            icon = AppIcons.LotteryTicket,
            title = "The waiting list is full",
            subtitle = ends,
            modifier = modifier,
        ) {
            DisabledButton("Waiting List Full")
        }

        LotteryStatus.DRAWING -> CardFrame(
            border = AppColors.OutlineStrong,
            icon = AppIcons.Clock,
            title = "Drawing winners…",
            subtitle = "Entries are closed. Your result will show here soon.",
            modifier = modifier,
        ) {
            DisabledButton("Results Coming Soon")
        }

        LotteryStatus.WON -> CardFrame(
            border = AppColors.Accent,
            icon = AppIcons.Trophy,
            title = "You won!",
            subtitle = "Book your nights" + (bookByText?.let { " before $it" } ?: "") +
                ". It's first come, first served among winners.",
            modifier = modifier,
        ) { width ->
            YellowButton(onClick = onBookClick, icon = AppIcons.Calendar, buttonText = "Book your stay", customWidth = width)
        }

        LotteryStatus.LOST -> CardFrame(
            border = AppColors.OutlineStrong,
            icon = AppIcons.LotteryTicket,
            title = "Not selected this time",
            subtitle = "You weren't drawn in this lottery. Keep an eye out for the next one.",
            modifier = modifier,
        ) {
            DisabledButton("Not Selected")
        }

        LotteryStatus.BOOKED -> CardFrame(
            border = AppColors.Primary,
            icon = AppIcons.CircleCheck,
            title = "You're booked",
            subtitle = bookingText,
            modifier = modifier,
            button = null,
        )

        LotteryStatus.DECLINED -> CardFrame(
            border = AppColors.OutlineStrong,
            icon = AppIcons.CancelCircle,
            title = "You declined this win",
            subtitle = "Your spot was released for other winners.",
            modifier = modifier,
        ) {
            DisabledButton("Declined")
        }

        LotteryStatus.MISSED -> CardFrame(
            border = AppColors.OutlineStrong,
            icon = AppIcons.Clock,
            title = "Booking has closed",
            subtitle = "You didn't book before the first night, so your win was released.",
            modifier = modifier,
        ) {
            DisabledButton("Booking Closed")
        }

        LotteryStatus.UPCOMING -> CardFrame(
            border = AppColors.OutlineStrong,
            icon = AppIcons.Clock,
            title = "A lottery is coming",
            subtitle = opensText?.let { "Entries open $it." },
            modifier = modifier,
        ) {
            DisabledButton(opensText?.let { "Opens $it" } ?: "Opening Soon")
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
 * Shared layout of the cards: icon and title, a line of detail, then a full-width button.
 * [button] gets the width available inside the card, since the shared buttons take a width in dp.
 * Pass null for a card without a button.
 */
@Composable
private fun CardFrame(
    border: Color,
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier,
    button: (@Composable (width: Int) -> Unit)?,
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
        if (button != null) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                button(maxWidth.value.toInt())
            }
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

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380, heightDp = 1800)
@Composable
private fun LotteryStatusCardPreview() {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LotteryStatusCard(LotteryStatus.OPEN, endsText = "12 Oct 2026 · 6:00 PM PT")
        LotteryStatusCard(LotteryStatus.JOINED, endsText = "12 Oct 2026 · 6:00 PM PT")
        LotteryStatusCard(LotteryStatus.FULL, endsText = "12 Oct 2026 · 6:00 PM PT")
        LotteryStatusCard(LotteryStatus.UPCOMING, opensText = "1 Oct")
        LotteryStatusCard(LotteryStatus.WON, bookByText = "16 Oct")
        LotteryStatusCard(LotteryStatus.LOST)
        LotteryStatusCard(LotteryStatus.BOOKED, bookingText = "Pine Creek · 16 Oct → 18 Oct · 2 nights")
        LotteryStatusCard(LotteryStatus.NONE)
    }
}
