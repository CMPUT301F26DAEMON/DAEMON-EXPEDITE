package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A lottery another owner invited your campsites to, on the Requests tab of My Lotteries.
 * Tapping it opens the request, where each campsite can be approved or declined.
 *
 * Built from the "Pine Creek collaboration request" frame in the Figma file.
 *
 * @param title The lottery's campsites, e.g. "Pine Creek Hideaway + 1 more".
 * @param invitedByText e.g. "Invited by Alex Chen".
 * @param staysText e.g. "16 Oct – 18 Oct · 2 nights".
 * @param awaitingText e.g. "1 of your campsites awaiting approval".
 * @param onClick Called when the card is tapped.
 */
@Composable
fun CollaborationRequestCard(
    title: String,
    invitedByText: String,
    staysText: String,
    awaitingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.OutlineRequest, shape)
            .clickable(role = Role.Button, onClickLabel = "Review request", onClick = onClick)
            .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 23.dp),
    ) {
        Text(
            title,
            style = AppTextStyles.HeadingSmall,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(7.dp))
        Text(invitedByText, style = AppTextStyles.Caption.copy(lineHeight = 17.4.sp), color = AppColors.TextSecondary)
        Spacer(Modifier.height(12.dp))
        Text(staysText, style = AppTextStyles.BodyMedium.copy(lineHeight = 20.3.sp), color = AppColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(awaitingText, style = AppTextStyles.BodySmall.copy(lineHeight = 18.85.sp), color = AppColors.TextSecondary)
        Spacer(Modifier.height(24.dp))
        Text("Review request   →", style = AppTextStyles.BodyMedium.copy(lineHeight = 20.3.sp), color = AppColors.Accent)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun CollaborationRequestCardPreview() {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CollaborationRequestCard(
            title = "Pine Creek Hideaway + 1 more",
            invitedByText = "Invited by Alex Chen",
            staysText = "16 Oct – 18 Oct · 2 nights",
            awaitingText = "1 of your campsites awaiting approval",
            onClick = {},
        )
    }
}
