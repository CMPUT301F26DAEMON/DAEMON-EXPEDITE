package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One notification: who sent it, the message, and when. Tapping it calls [onClick].
 *
 * Takes plain text rather than a model object so it can be reused anywhere; format the
 * timestamp before passing it in.
 *
 * Example:
 * ```
 * NotificationCard(
 *     sender = "Pine Creek Hideaway",
 *     message = "You won the lottery for Jun 12 - Jun 14.",
 *     timestamp = "2h ago",
 *     onClick = { /* open the reservation */ },
 * )
 * ```
 *
 * @param sender Shown in bold at the top, for example a campsite or owner name.
 * @param message The notification text. Wraps onto as many lines as it needs.
 * @param title Optional heading shown between the sender and the message.
 * @param isNew Shows a green dot next to the sender for notifications the user hasn't seen.
 * @param timestamp When it was sent, already formatted, for example "2h ago".
 * @param onClick Called when the card is tapped.
 * @param modifier Optional [Modifier] for the card.
 */
@Composable
fun NotificationCard(
    sender: String,
    message: String,
    timestamp: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    isNew: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.FillDeepTint)
            .border(
                width = 0.5.dp,
                color = AppColors.SurfaceDeep,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 13.dp,
                vertical = 11.dp
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNew) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary)
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = sender,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(5.dp))

        if (title != null) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
        }

        Text(
            text = message,
            color = AppColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = timestamp,
            color = AppColors.Secondary,
            fontSize = 11.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun NotificationCardPreview() {
    Column(Modifier.padding(16.dp)) {
        NotificationCard(
            sender = "Pine Creek Hideaway",
            message = "You won the lottery for Jun 12 - Jun 14. Accept your spot within 48 hours.",
            timestamp = "2h ago",
            onClick = {},
            title = "Lottery results",
            isNew = true,
        )
        Spacer(Modifier.height(10.dp))
        NotificationCard(
            sender = "Expedite",
            message = "Short message.",
            timestamp = "Yesterday",
            onClick = {},
        )
    }
}
