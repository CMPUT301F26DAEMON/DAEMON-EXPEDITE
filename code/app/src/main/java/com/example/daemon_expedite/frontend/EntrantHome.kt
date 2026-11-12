package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.ProfileImage
import com.example.daemon_expedite.ui_components.SearchBar
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable

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
    val openLotteryCount = (campsites as? UiState.Content)?.data
        ?.mapNotNull { it.openLotteryId }?.distinct()?.size ?: 0

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
        // Top bar: greeting, profile picture, then the search row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Find your next hideaway",
                        style = AppTextStyles.PageTitle.copy(fontSize = 22.sp, lineHeight = 28.sp),
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = greeting(userName, openLotteryCount),
                        style = AppTextStyles.BodySmall.copy(lineHeight = 18.sp),
                        color = AppColors.TextMuted,
                    )
                }
                // Profile picture (falls back to a default image when the user has none)
                ProfileImage(
                    imageUrl = profileImageUrl,
                    size = 42.dp,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholderText = "Where do you want to go?",
                        showQrIcon = true,
                        onQRCodeClicked = {},
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.FillField)
                        .border(1.dp, AppColors.OutlineField, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Filter") { showFilterSheet = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Filter,
                        contentDescription = "Filter",
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Results heading with the number of campsites
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Campsites for you",
                style = AppTextStyles.HeadingMedium.copy(lineHeight = 25.sp),
                color = AppColors.TextPrimary,
            )
            val campsiteCount = (campsites as? UiState.Content)?.data?.size
            if (campsiteCount != null) {
                Text(
                    text = campsiteCount.toString(),
                    style = AppTextStyles.Caption.copy(fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
                    color = AppColors.TextSecondary,
                    modifier = Modifier
                        .background(AppColors.FillChip, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
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
            horizontalPadding = 28.dp,
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

/**
 * "Good morning, Jamie · 1 lottery open near you". The time of day follows the phone's clock,
 * and the lottery part is left out when none of the shown campsites has an open lottery.
 */
private fun greeting(userName: String, openLotteryCount: Int, now: LocalTime = LocalTime.now()): String {
    val timeOfDay = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val name = if (userName.isNotBlank()) ", $userName" else ""
    val lotteries = when (openLotteryCount) {
        0 -> ""
        1 -> " · 1 lottery open near you"
        else -> " · $openLotteryCount lotteries open near you"
    }
    return timeOfDay + name + lotteries
}

@Preview(showBackground = true)
@Composable
private fun EntrantHomeScreenPreview() {
    EntrantHomeScreen()
}
