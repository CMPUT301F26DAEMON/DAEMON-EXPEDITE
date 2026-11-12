
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons

/**
 * Notifications screen for the Expedite application.
 *
 * Displays notifications from lotteries, reservations,
 * cancellations, and campsite owners.
 *
 * TODO: Connect to the notification backend.
 * TODO: Connect notification clicks to navigation.
 */

data class NotificationEntry(
    val sender: String,
    val message: String,
    val timestamp: String
)


@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntry> = emptyList(),
    onBackClick: () -> Unit = {},
    onNotificationClick: (NotificationEntry) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(CircleShape)
                    .background(AppColors.FillSubtle)
                    .border(
                        width = 1.dp,
                        color = AppColors.OutlineSubtle,
                        shape = CircleShape
                    )
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Notifications",
                color = AppColors.TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(Modifier.height(24.dp))

        // Notification list
        notifications.forEach { notification ->
            NotificationCard(
                notification = notification,
                onClick = {
                    onNotificationClick(notification)
                }
            )

            Spacer(Modifier.height(5.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = AppColors.Divider
            )

            Spacer(Modifier.height(5.dp))
        }
    }
}

/**
 * Displays a notification with sender, message and time.
 */
@Composable
private fun NotificationCard(
    notification: NotificationEntry,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
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
        Text(
            text = notification.sender,
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = notification.message,
            color = AppColors.TextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = notification.timestamp,
            color = AppColors.Secondary,
            fontSize = 11.sp
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 393,
    heightDp = 852
)
@Composable
private fun NotificationsScreenPreview() {
    NotificationsScreen()
}
