package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppFonts
import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.ProfileImage
import com.example.daemon_expedite.ui_components.SearchBar

/**
* The main Entrant Home screen.
* Displays a search bar with a QR code scanner, filter and profile buttons
* Includes a list of recommended campsites.
* Includes a bottom navigation bar for accessing reservations, lotteries,
* and messages.
*
* The screen only displays what it is given; [HomeViewModel] does the loading and searching.
*
* @param profileImageUrl URL of the signed-in user's profile picture, or null to show the fallback.
* @param searchQuery Text currently in the search bar.
* @param onSearchQueryChange Called as the user types in the search bar.
* @param campsites The campsites to list, or a loading/error state.
* @param onRetry Called when the user taps the error message to load again.
* @param onCampsiteClick Called with the id of the tapped campsite.
* @param onProfileClick Called when the profile picture in the top bar is tapped.
* @param onReservationsClick Called when the Reservations tab is tapped.
* @param onLotteriesClick Called when the Lotteries tab is tapped.
* @param onMessagesClick Called when the Messages tab is tapped.
*/
@Composable
fun EntrantHomeScreen(
    profileImageUrl: String? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    campsites: UiState<List<CampsiteSummary>> = UiState.Content(emptyList()),
    onRetry: () -> Unit = {},
    onCampsiteClick: (Id) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onReservationsClick: () -> Unit = {},
    onLotteriesClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 15.dp,
                    end = 11.dp, // + the profile button's 4dp inner padding = 15dp, matching start
                    top =30.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Search BAr (no fixed height, so the Row can centre it with the icons)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholderText = "WHERE DO YOU WANT TO GO?",
                    showQrIcon = true,
                    onQRCodeClicked = {}
                )
            }
            // Smaller buttons than the 48dp default to tighten the filter/profile gap
            IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                Icon(  //Filter icon
                    imageVector = AppIcons.Filter,
                    contentDescription = "Filter",
                    tint = AppColors.TextOnPrimary
                )
            }
            IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) {
                // Profile picture (falls back to a default image when the user has none)
                ProfileImage(
                    imageUrl = profileImageUrl,
                    size = 39.dp,
                    contentDescription = "Profile"
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 25.dp,
                    end = 25.dp,
                    top = 65.dp
                )
        ) {
            // Heading
            Text(
                text = "Find your next hideaway",
                fontFamily = AppFonts.Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = AppColors.TextPrimary
            )
            Text(
                text = "Campsites for you",
                fontFamily = AppFonts.Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        // Campsite cards, or a spinner/error/empty message
        CampsiteList(
            state = campsites,
            emptyText = if (searchQuery.isBlank()) "No campsites to show yet"
                        else "No campsites match \"$searchQuery\"",
            onRetry = onRetry,
            onCampsiteClick = onCampsiteClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        BottomNavBar(
            labels = listOf(
                "Reservations",
                "Lotteries",
                "Messages"
            ),
            icons = listOf(
                AppIcons.Reservation,
                AppIcons.LotteryTicket,
                AppIcons.Chat
            ),
            onClicks = listOf(
                onReservationsClick,
                onLotteriesClick,
                onMessagesClick
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EntrantHomeScreenPreview() {
    EntrantHomeScreen()
}