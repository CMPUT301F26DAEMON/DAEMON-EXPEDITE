
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.NotificationCard

/**
 * Notifications screen for the Expedite application.
 *
 * Displays notifications from lotteries, reservations,
 * cancellations, and campsite owners.
 *
 * Opened from the Notifications tab of the entrant, owner and admin bottom bars
 * ([Routes.ENTRANT_NOTIFICATIONS], [Routes.OWNER_NOTIFICATIONS], [Routes.ADMIN_NOTIFICATIONS]).
 * The bar is drawn on top by AppNavigation. Shows "No notifications yet" when
 * [notifications] is empty.
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
    // Null hides the back button: the entrant opens this as a tab, like Reservations.
    onBackClick: (() -> Unit)? = {},
    onNotificationClick: (NotificationEntry) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
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
            if (onBackClick != null) {
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
                        .clickable(onClick = onBackClick),
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
            }

            Text(
                text = "Notifications",
                style = AppTextStyles.PageTitle,
                color = AppColors.TextPrimary
            )
        }

        Spacer(Modifier.height(24.dp))

        if (notifications.isEmpty()) {
            Text(
                text = "No notifications yet",
                style = AppTextStyles.BodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                textAlign = TextAlign.Center
            )
        }

        // Notification list
        notifications.forEach { notification ->
            NotificationCard(
                sender = notification.sender,
                message = notification.message,
                timestamp = notification.timestamp,
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

        // Room for the shared bottom bar AppNavigation draws on top.
        BottomNavBarSpacer()
    }
}

@Preview(
    showBackground = true,
    widthDp = 393,
    heightDp = 852
)
@Composable
private fun NotificationsScreenPreview() {
    NotificationsScreen(
        notifications = listOf(
            NotificationEntry(
                sender = "Pine Creek Hideaway",
                message = "You won the lottery for Jun 12 - Jun 14. Accept your spot within 48 hours.",
                timestamp = "2h ago"
            ),
            NotificationEntry(
                sender = "Maple Hollow",
                message = "The lottery you joined closes tomorrow at midnight.",
                timestamp = "5h ago"
            ),
            NotificationEntry(
                sender = "Lakeside Retreat",
                message = "Your reservation for Jul 3 - Jul 5 was cancelled by the owner.",
                timestamp = "Yesterday"
            ),
            NotificationEntry(
                sender = "Expedite",
                message = "Welcome! Turn on notifications to hear about lottery results.",
                timestamp = "Nov 2"
            )
        )
    )
}
