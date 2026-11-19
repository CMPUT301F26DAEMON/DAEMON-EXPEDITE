package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.ProfileImage
import com.example.daemon_expedite.ui_components.SearchBar
import java.time.LocalDate

/**
 * The main Admin Home screen.
 * Displays a search bar with a QR code scanner, filter and profile buttons.
 * Includes a list of campsites.
 * The bottom bar (messages, the profile list, notifications and images) is drawn over this
 * screen by AppNavigation, so it stays put while the admin moves between screens.
 *
 * Same layout as [EntrantHomeScreen]; only the bottom navigation tabs differ.
 * The screen only displays what it is given; [HomeViewModel] does the loading and searching.
 *
 * @param profileImageUrl URL of the signed-in user's profile picture, or null to show the fallback.
 * @param searchQuery Text currently in the search bar.
 * @param onSearchQueryChange Called as the user types in the search bar.
 * @param campsites The campsites to list, or a loading/error state.
 * @param onRetry Called when the user taps the error message to load again.
 * @param onCampsiteClick Called with the id of the tapped campsite.
 * @param onFilterClick Called when the filter button is tapped.
 * @param onProfileClick Called when the profile picture in the top bar is tapped.
 */
@Composable
fun AdminHomeScreen(
    profileImageUrl: String? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    campsites: UiState<List<CampsiteSummary>> = UiState.Content(emptyList()),
    onRetry: () -> Unit = {},
    onCampsiteClick: (Id) -> Unit = {},
    onFilterClick: (location: String, startDate: LocalDate?, endDate: LocalDate?, guestCount: Int) -> Unit = { _, _, _, _ -> },
    onProfileClick: () -> Unit = {},
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterLocation by remember { mutableStateOf("") }
    var filterStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterGuestCount by remember { mutableStateOf(1) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AppColors.Background),
    ) {
        // Keeps the top bar clear of the status bar and camera cutout on every device
        Spacer(
            Modifier.windowInsetsTopHeight(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 15.dp,
                        end = 11.dp, // + the profile button's 4dp inner padding = 15dp, matching start
                        top = 12.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search BAr (no fixed height, so the Row can centre it with the icons)
            Box(
                modifier = Modifier.weight(1f),
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholderText = "WHERE DO YOU WANT TO GO?",
                    showQrIcon = true,
                    onQRCodeClicked = {},
                )
            }
            // Smaller buttons than the 48dp default to tighten the filter/profile gap
            IconButton(
                onClick = {
                    showFilterSheet = true
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon( // Filter icon
                    imageVector = AppIcons.Filter,
                    contentDescription = "Filter",
                    tint = AppColors.TextOnPrimary,
                )
            }
            IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) {
                // Profile picture (falls back to a default image when the user has none)
                ProfileImage(
                    imageUrl = profileImageUrl,
                    size = 39.dp,
                    contentDescription = "Profile",
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 25.dp,
                        end = 25.dp,
                        top = 32.dp,
                    ),
        ) {
            // Heading
            Text(
                text = "Find your next hideaway",
                style = AppTextStyles.PageTitle,
                color = AppColors.TextPrimary,
            )
            Text(
                text = "Campsites for you",
                style = AppTextStyles.ValueEmphasis,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        // Campsite cards, or a spinner/error/empty message
        CampsiteList(
            state = campsites,
            emptyText =
                if (searchQuery.isBlank()) {
                    "No campsites to show yet"
                } else {
                    "No campsites match \"$searchQuery\""
                },
            onRetry = onRetry,
            onCampsiteClick = onCampsiteClick,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        )
        // Room for the shared bottom bar AppNavigation draws on top.
        BottomNavBarSpacer()
    }

    if (showFilterSheet) {
        FilterCampsitesSheet(
            location = filterLocation,
            onLocationChange = { filterLocation = it },
            startDate = filterStartDate,
            endDate = filterEndDate,
            today = remember { LocalDate.now() },
            onRangeChange = { start, end ->
                filterStartDate = start
                filterEndDate = end
            },
            guestCount = filterGuestCount,
            onGuestCountChange = { delta ->
                filterGuestCount = (filterGuestCount + delta).coerceAtLeast(1)
            },
            onApplyFilters = {
                onFilterClick(filterLocation, filterStartDate, filterEndDate, filterGuestCount)
                showFilterSheet = false
            },
            onResetFilters = {
                filterLocation = ""
                filterStartDate = null
                filterEndDate = null
                filterGuestCount = 1
                onSearchQueryChange("")
                onFilterClick("", null, null, 1)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminHomeScreenPreview() {
    AdminHomeScreen()
}
