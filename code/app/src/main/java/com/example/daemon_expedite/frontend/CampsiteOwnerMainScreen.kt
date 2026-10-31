package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.daemon_expedite.ui_components.AppColors
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp



import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.SearchBar

/**
 * The main Campsite Owner screen.
 * Displays a search bar and profile button for accessing the owner's profile.
 * Includes a scrollable list of registered campsites and their details.
 * Provides a button for registering new campsites.
 * Includes a bottom navigation bar for accessing reservations, messages,
 * notifications, and lotteries.
 */
@Composable
fun CampsiteOwnerMainPage() {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {

        // Search bar and profile icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, top = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Search bar
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholderText = "SEARCH YOUR CAMPSITES",
                    showQrIcon = false,
                    onQRCodeClicked = {}
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    imageVector = AppIcons.Profile,
                    contentDescription = "Profile",
                    tint = AppColors.TextOnPrimary
                )
            }
        }
        // Registered campsites heading
        Text(
            text = "My Registered Campsites",
            style = AppTextStyles.PageTitle,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(
                start = 25.dp,
                end = 25.dp,
                top = 50.dp,
                bottom = 20.dp
            )
        )

        // Scrollable list of the owner's registered campsites
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Registered Campsites go here
        }
        // Register campsite button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            GreenButton(
                onClick = {},
                buttonText = "Register a campsite"
            )
        }
        // Bottom navigation
        BottomNavBar(
            labels = listOf(
                "Reservations",
                "Messages",
                "Notifications",
                "Lotteries"
            ),
            icons = listOf(
                AppIcons.Reservation,
                AppIcons.Chat,
                AppIcons.BellIcon,
                AppIcons.LotteryTicket
            ),
            onClicks = listOf(
                {}, //Reservations
                {}, //Messages
                {}, //Notifications
                {}, //Lotteries
            )


        )
    }
}


@Preview(showBackground = true)
@Composable
fun CampsiteOwnerMainPagePreview() {
    CampsiteOwnerMainPage()
}