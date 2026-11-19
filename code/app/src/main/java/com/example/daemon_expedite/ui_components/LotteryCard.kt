// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A compact, text-only card summarising one lottery, for the owner's My Lotteries list.
 *
 * Every line is passed in already formatted, so the card has no date or plural logic:
 * ```
 * LotteryCard(
 *     title = "Pine Creek + 2 more",
 *     status = "Open", statusVariant = PillVariant.Highlight,
 *     entriesText = "Entries close 5 Oct",
 *     staysText = "16 Oct – 25 Oct · 5 nights",
 *     countsText = "12 joined · 4 winners",
 *     onClick = { openLottery(id) },
 * )
 * ```
 *
 * @param title Campsite names, for example "Pine Creek + 2 more".
 * @param status Short phase label shown in the pill, for example "Open".
 * @param entriesText When entries open or close.
 * @param staysText The stay dates and number of nights.
 * @param countsText How many joined and how many winners.
 * @param onClick Called when the card is tapped. Null makes the card not tappable and hides the arrow.
 * @param modifier [Modifier] applied to the card.
 * @param statusVariant Look of the status pill. See [PillVariant].
 * @param footer Optional content under the text, for example a button. Null shows nothing.
 */
@Composable
fun LotteryCard(
    title: String,
    status: String,
    entriesText: String,
    staysText: String,
    countsText: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    statusVariant: PillVariant = PillVariant.Outlined,
    footer: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(BorderStroke(1.dp, AppColors.OutlineAccent), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(AppIcons.LotteryTicket, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = AppTextStyles.CardTitle,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CampsitePill(status, variant = statusVariant)
        }
        Text(entriesText, style = AppTextStyles.BodySmall, color = AppColors.TextSecondary)
        Text(staysText, style = AppTextStyles.BodySmall, color = AppColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = countsText,
                style = AppTextStyles.BodySmall,
                color = AppColors.Secondary,
                modifier = Modifier.weight(1f),
            )
            if (onClick != null) {
                Icon(AppIcons.ArrowForward, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
        if (footer != null) {
            Box(Modifier.padding(top = 6.dp)) { footer() }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun LotteryCardPreview() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LotteryCard(
            title = "Pine Creek Hideaway + 2 more",
            status = "Open",
            statusVariant = PillVariant.Highlight,
            entriesText = "Entries close 5 Oct",
            staysText = "16 Oct – 25 Oct · 5 nights",
            countsText = "12 joined · 4 winners",
            onClick = {},
        )
        LotteryCard(
            title = "Maple Campsite",
            status = "Cancelled",
            statusVariant = PillVariant.Muted,
            entriesText = "Entries closed 5 Oct",
            staysText = "16 Oct – 18 Oct · 2 nights",
            countsText = "3 joined · 2 winners",
            onClick = {},
        )
    }
}
