package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles

import com.example.daemon_expedite.ui_components.BottomNavBar
// Screen showing owner's messages
// empty lazy column for now
@Composable
fun OwnerMessagingPage(
    onMySitesClick: () -> Unit = {},
    onReservationsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLotteriesClick: () -> Unit = {}
){

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Text(
            text = "Messages",
            style = AppTextStyles.PageTitleLarge,
            color = AppColors.TextOnPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = 26.dp
                )
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Messages are added here
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                horizontal = 28.dp
            )
        ){

            // Displays message "NO messages yet" if none to display

            item{Box(modifier = Modifier.fillParentMaxSize(),contentAlignment = Alignment.Center){
                    Text(
                        text = "No messages yet",
                        style = AppTextStyles.BodyMedium,
                        color = AppColors.TextMuted
                    )
                }
            }

        }
        // Reusing bottom nav bar
        BottomNavBar(
            labels = listOf(
                "My sites",
                "Reservations",
                "Messages",
                "Notifications",
                "Lotteries"
            ),
            icons = listOf(
                AppIcons.Home,
                AppIcons.Reservation,
                AppIcons.Chat,
                AppIcons.BellIcon,
                AppIcons.LotteryTicket
            ),
            onClicks = listOf(
                onMySitesClick,
                onReservationsClick, {},
                onNotificationsClick,
                onLotteriesClick
            ),
            selectedIndex = 2
        )
    }
}
/**
 * Preview showing the owner's Messaging screen
 * Displays the screen heading and bottom navigation bar.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 405,
    heightDp = 850,
    showSystemUi = true
)
@Composable
private fun OwnerMessagingPagePreview(){OwnerMessagingPage()}