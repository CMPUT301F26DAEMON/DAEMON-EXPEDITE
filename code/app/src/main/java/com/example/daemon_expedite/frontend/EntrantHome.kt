package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppFonts
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.ProfileImage
import com.example.daemon_expedite.ui_components.SearchBar
import java.time.LocalDate

/**
* The main Entrant Home screen.
* Displays a search bar with a QR code scanner, filter and profile buttons
* Includes a list of recommended campsites.
* Includes a bottom navigation bar for accessing reservations, lotteries,
* and messages.
*
* The screen only displays what it is given; [HomeViewModel] does the loading and searching.
*
 * @param profileImageUrl URL of the signed-in user's profile picture,
 * or null to display the default image.
 * @param userName Name of the signed-in user displayed in the greeting.
 * @param searchQuery Current text entered in the search bar.
 * @param onSearchQueryChange Called when the search query changes.
 * @param campsites Campsites to display, or a loading/error state.
 * @param onRetry Called when the user retries loading campsites.
 * @param onCampsiteClick Called with the ID of the selected campsite.
 * @param onFilterClick Called when the user applies or resets filters,
 * with the location, stay dates, and guest count.
 * @param onProfileClick Called when the profile picture is tapped.
 * @param onReservationsClick Called when the Reservations tab is tapped.
 * @param onLotteriesClick Called when the Lotteries tab is tapped.
 * @param onMessagesClick Called when the Messages tab is tapped.
 * @param onExploreClick Called when the Explore tab is tapped.
*/
@Composable
fun EntrantHomeScreen(
    profileImageUrl: String? = null,
    userName: String = "",
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    campsites: UiState<List<CampsiteSummary>> = UiState.Content(emptyList()),
    onRetry: () -> Unit = {},
    onCampsiteClick: (Id) -> Unit = {},
    onFilterClick: (location: String, startDate: LocalDate?, endDate: LocalDate?, guestCount: Int) -> Unit = { _, _, _, _ -> },
    onProfileClick: () -> Unit = {},
    onReservationsClick: () -> Unit = {},
    onLotteriesClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
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
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Find your next hideaway",
                        style = AppTextStyles.PageTitle,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Good evening${if (userName.isNotBlank()) ", $userName" else ""} · X lotteries open near you", // TODO: Replace X with the actual number of open nearby lotteries from the backend.
                        style = AppTextStyles.BodySmall,
                        color = AppColors.TextMuted
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
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 15.dp,
                        end = 11.dp, // + the profile button's 4dp inner padding = 15dp, matching start
                        top = 12.dp,
                    ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search BAr (no fixed height, so the Row can centre it with the icons)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholderText = "Where do you want to go?",
                    showQrIcon = true,
                    onQRCodeClicked = {},
                )
            }
            // Smaller buttons than the 48dp default to tighten the filter/profile gap
            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .size(30.dp)
                    .background(AppColors.FillField, CircleShape)
                    .border(1.dp, AppColors.OutlineField, CircleShape)

            ) {
                Icon( // Filter icon
                    imageVector = AppIcons.Filter,
                    contentDescription = "Filter",
                    tint = AppColors.TextOnPrimary,
                    modifier = Modifier.size(23.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Campsites for you",
                    style = AppTextStyles.HeadingMedium,
                    color = AppColors.TextPrimary
                )

                val campsiteCount = (campsites as? UiState.Content)?.data?.size

                if (campsiteCount != null) {
                    Text(
                        text = campsiteCount.toString(),
                        style = AppTextStyles.Caption,
                        color = AppColors.TextSecondary,
                        modifier = Modifier
                            .background(AppColors.SurfaceDeep, CircleShape)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
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
        BottomNavBar(
            labels =
                listOf(
                    "Explore",
                    "Reservations",
                    "Lotteries",
                    "Messages",
                ),
            icons =
                listOf(
                    AppIcons.Home,
                    AppIcons.Reservation,
                    AppIcons.LotteryTicket,
                    AppIcons.Chat,
                ),
            onClicks =
                listOf(
                    onExploreClick,
                    onReservationsClick,
                    onLotteriesClick,
                    onMessagesClick,
                ),
            selectedIndex = 0
        )
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
private fun EntrantHomeScreenPreview() {
    EntrantHomeScreen()
}
