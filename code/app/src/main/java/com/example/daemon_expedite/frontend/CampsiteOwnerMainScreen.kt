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
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp



import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.ProfileImage
import androidx.compose.foundation.layout.size
import com.example.daemon_expedite.ui_components.SearchBar

/**
 * The main Campsite Owner screen.
 * Displays a search bar and profile button for accessing the owner's profile.
 * Includes a scrollable list of registered campsites and their details.
 * Provides a button for registering new campsites.
 * Includes a bottom navigation bar for accessing reservations, messages,
 * notifications, and lotteries.
 *
 * The screen only displays what it is given; [OwnerViewModel] loads the campsites.
 * The search bar filters the already-loaded list, so it doesn't hit the database.
 */
@Composable
fun CampsiteOwnerMainPage(
    profileImageUrl: String? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    campsites: UiState<List<CampsiteSummary>> = UiState.Content(emptyList()),
    onRetry: () -> Unit = {},
    onCampsiteClick: (Id) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRegisterCampsiteClick: () -> Unit = {},
    onReservationsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLotteriesClick: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {

        // Search bar and profile icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 15.dp,
                    end = 11.dp, // + the profile button's inner padding = 15dp, matching start
                    top = 30.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Search bar
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholderText = "SEARCH YOUR CAMPSITES",
                    showQrIcon = false,
                    onQRCodeClicked = {}
                )
            }

            // Same size as on the entrant home page so the two top bars match.
            IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) {
                // Profile picture (falls back to a default image when the user has none)
                ProfileImage(
                    imageUrl = profileImageUrl,
                    size = 39.dp,
                    contentDescription = "Profile"
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

        // Scrollable list of the owner's registered campsites, filtered by the search text
        val filtered = when (campsites) {
            is UiState.Content -> UiState.Content(
                campsites.data.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
            )
            else -> campsites
        }
        CampsiteList(
            state = filtered,
            emptyText = if (searchQuery.isBlank()) "You haven't registered a campsite yet"
                        else "No campsites match \"$searchQuery\"",
            onRetry = onRetry,
            onCampsiteClick = onCampsiteClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        // Register campsite button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            GreenButton(
                onClick = onRegisterCampsiteClick,
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
                onReservationsClick,
                onMessagesClick,
                onNotificationsClick,
                onLotteriesClick,
            )


        )
    }
}


@Preview(showBackground = true)
@Composable
fun CampsiteOwnerMainPagePreview() {
    CampsiteOwnerMainPage()
}