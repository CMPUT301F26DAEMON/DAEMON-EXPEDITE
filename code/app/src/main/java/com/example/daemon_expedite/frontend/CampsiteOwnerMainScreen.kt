package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppFonts
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.ProfileImage
import com.example.daemon_expedite.ui_components.SearchBar
import java.time.LocalTime

/**
 * The main Campsite Owner screen.
 * Displays a search bar and profile button for accessing the owner's profile.
 * Includes a scrollable list of registered campsites and their details.
 * Provides a button for registering new campsites.
 * The bottom bar (reservations, messages, notifications and lotteries) is drawn over this
 * screen by AppNavigation, so it stays put while the owner moves between screens.
 *
 * The screen only displays what it is given; [OwnerViewModel] loads the campsites.
 * The search bar filters the already-loaded list, so it doesn't hit the database.
 *
 * @param profileImageUrl URL of the signed-in owner's profile picture, or null.
 * @param ownerName Name of the signed-in owner displayed in the header greeting.
 * @param searchQuery Current search text.
 * @param onSearchQueryChange Called when search query changes.
 * @param campsites Registered campsites list or loading/error state.
 * @param activeLotteryCount Count of currently active lotteries owned.
 * @param bookedStayCount Count of currently booked stays.
 * @param onRetry Called when the user retries loading.
 * @param onCampsiteClick Called with the ID of a selected campsite.
 * @param onProfileClick Called when the profile picture is tapped.
 * @param onRegisterCampsiteClick Called when "Add" / "Register a campsite" is tapped.
 */
@Composable
fun CampsiteOwnerMainPage(
    profileImageUrl: String? = null,
    ownerName: String = "",
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    campsites: UiState<List<CampsiteSummary>> = UiState.Content(emptyList()),
    activeLotteryCount: Int = 0,
    bookedStayCount: Int = 0,
    onRetry: () -> Unit = {},
    onCampsiteClick: (Id) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRegisterCampsiteClick: () -> Unit = {},
) {
    val campsitesList = (campsites as? UiState.Content)?.data.orEmpty()
    val campsiteCount = campsitesList.size
    val activeLotteries = if (activeLotteryCount > 0) activeLotteryCount else campsitesList.count { it.openLotteryId != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // Keeps the top bar clear of the status bar and camera cutout on every device
        Spacer(
            Modifier.windowInsetsTopHeight(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
        )

        // Top bar: time-based greeting title, profile picture, then the search row
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
                        text = ownerGreetingTitle(ownerName),
                        style = AppTextStyles.PageTitleSmall,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = ownerGreetingSubtitle(activeLotteries, bookedStayCount),
                        style = AppTextStyles.BodySmallMultiline,
                        color = AppColors.TextMuted,
                    )
                }
                // Profile picture (falls back to a default image when the user has none)
                ProfileImage(
                    imageUrl = profileImageUrl,
                    size = 44.dp,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick),
                )
            }

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholderText = "Search your campsites...",
                showQrIcon = false,
                onQRCodeClicked = {},
            )
        }

        // Section heading: My Campsites + Count Chip + Add Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "My Campsites",
                    style = AppTextStyles.HeadingMedium.copy(lineHeight = 25.sp),
                    color = AppColors.TextPrimary,
                )
                if (campsites is UiState.Content) {
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

            Button(
                onClick = onRegisterCampsiteClick,
                modifier = Modifier.height(36.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    contentColor = AppColors.TextOnPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.Plus,
                        contentDescription = "Add campsite",
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Add",
                        fontFamily = AppFonts.Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AppColors.TextOnPrimary,
                    )
                }
            }
        }

        // Scrollable list of the owner's registered campsites, filtered by the search text
        val filtered = when (campsites) {
            is UiState.Content -> UiState.Content(
                campsites.data.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) },
            )
            else -> campsites
        }

        CampsiteList(
            state = filtered,
            emptyText = if (searchQuery.isBlank()) "You haven't registered a campsite yet"
                        else "No campsites match \"$searchQuery\"",
            onRetry = onRetry,
            onCampsiteClick = onCampsiteClick,
            horizontalPadding = 28.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        // Room for the shared bottom bar AppNavigation draws on top.
        BottomNavBarSpacer()
    }
}

/**
 * Formats time-based owner greeting, e.g. "Good morning, Jamie".
 */
private fun ownerGreetingTitle(ownerName: String, now: LocalTime = LocalTime.now()): String {
    val timeOfDay = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (ownerName.isNotBlank()) "$timeOfDay, $ownerName" else timeOfDay
}

/**
 * Formats owner subtitle, e.g. "3 active lotteries · 12 booked stays".
 */
private fun ownerGreetingSubtitle(activeLotteries: Int, bookedStays: Int): String {
    val lotteriesText = when (activeLotteries) {
        1 -> "1 active lottery"
        else -> "$activeLotteries active lotteries"
    }
    val staysText = when (bookedStays) {
        1 -> "1 booked stay"
        else -> "$bookedStays booked stays"
    }
    return "$lotteriesText · $staysText"
}

@Preview(showBackground = true)
@Composable
fun CampsiteOwnerMainPagePreview() {
    CampsiteOwnerMainPage(ownerName = "Josh")
}
