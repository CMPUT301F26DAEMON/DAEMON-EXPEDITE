package com.example.daemon_expedite.frontend

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.OwnerReservationCard
import com.example.daemon_expedite.ui_components.OwnerReservationCardStatus

/** The three tabs in the owner's Reservations screen. */
enum class OwnerReservationTab(val label: String, val emptyMessage: String) {
    ACTIVE("Active", "No active reservations"),
    PAST("Past", "No past reservations"),
    CANCELLED("Cancelled", "No cancelled reservations")
}

/** Navigation destinations displayed in the Figma bottom bar. */
enum class OwnerReservationNavTab(val label: String) {
    MY_SITES("My sites"),
    RESERVATIONS("Reservations"),
    MESSAGES("Messages"),
    NOTIFICATIONS("Notifications"),
    LOTTERIES("Lotteries")
}

/**
 * UI-only reservation model.
 * Optional fields are added at the end to preserve existing call sites.
 */
data class OwnerReservationUi(
    val id: String,
    val campsiteName: String,
    val location: String,
    val status: OwnerReservationTab,
    val pricePerNight: Int? = null,
    val rating: Double? = null,
    val confirmed: Boolean = false,
    @DrawableRes val imageResId: Int? = null,
    val guestName: String? = null,
    val guestCount: Int? = null,
    val dateRange: String? = null,
    val nights: Int? = null,
    val cancelledByGuest: Boolean = false
)

/**
 *
 * @param reservations The live reservations supplied by the caller.
 * @param onRejectReservation Triggered only after the user confirms rejection.
 * @param onBottomNavigationSelected Connect this to the app's navigation controller.
 * @param showBottomNavigation Set false if the parent already draws the bottom bar.
 * @param initialTab Useful for navigation and the three Android Studio previews.
 * @param onBackClick Retained for compatibility with older call sites; the Figma
 *   design has no back button.
 */
@Composable
fun OwnerReservationsScreen(
    reservations: List<OwnerReservationUi> = emptyList(),
    onBackClick: () -> Unit = {},
    onRejectReservation: (OwnerReservationUi) -> Unit = {},
    onBottomNavigationSelected: (OwnerReservationNavTab) -> Unit = {},
    showBottomNavigation: Boolean = true,
    initialTab: OwnerReservationTab = OwnerReservationTab.ACTIVE
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var reservationToReject by remember { mutableStateOf<OwnerReservationUi?>(null) }
    val filteredReservations = reservations.filter { it.status == selectedTab }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            text = "Reservations",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 22.dp, bottom = 24.dp),
            color = AppColors.TextOnPrimary,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceDeep)
                .border(1.dp, AppColors.OutlineSubtle, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            OwnerReservationTab.values().forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) AppColors.Primary else AppColors.SurfaceDeep)
                        .clickable { selectedTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        color = if (selected) AppColors.TextOnPrimary else AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (filteredReservations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedTab.emptyMessage,
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredReservations, key = { it.id }) { reservation ->
                    OwnerReservationCard(
                        campsiteName = reservation.campsiteName,
                        location = reservation.location,
                        status = when (reservation.status) {
                            OwnerReservationTab.ACTIVE -> OwnerReservationCardStatus.ACTIVE
                            OwnerReservationTab.PAST -> OwnerReservationCardStatus.PAST
                            OwnerReservationTab.CANCELLED -> OwnerReservationCardStatus.CANCELLED
                        },
                        pricePerNight = reservation.pricePerNight,
                        rating = reservation.rating,
                        confirmed = reservation.confirmed,
                        imageResId = reservation.imageResId,
                        guestName = reservation.guestName,
                        guestCount = reservation.guestCount,
                        dateRange = reservation.dateRange,
                        nights = reservation.nights,
                        cancelledByGuest = reservation.cancelledByGuest,
                        onRejectClick = { reservationToReject = reservation }
                    )
                }
            }
        }

        if (showBottomNavigation) {
            OwnerReservationsBottomBar(onBottomNavigationSelected)
        }
    }

    val pending = reservationToReject
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { reservationToReject = null },
            containerColor = AppColors.Surface,
            title = {
                Text(
                    "Reject reservation?",
                    color = AppColors.TextOnPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to reject this reservation?",
                    color = AppColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    reservationToReject = null
                    onRejectReservation(pending)
                }) {
                    Text("Reject", color = AppColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToReject = null }) {
                    Text("Cancel", color = AppColors.TextOnPrimary)
                }
            }
        )
    }
}

/** Bottom bar from the owner Figma screens, using only existing AppIcons. */
@Composable
private fun OwnerReservationsBottomBar(
    onSelected: (OwnerReservationNavTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceDeep)
            .border(1.dp, AppColors.OutlineSubtle)
            .height(65.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerReservationNavTab.values().forEach { tab ->
            val selected = tab == OwnerReservationNavTab.RESERVATIONS
            val icon = when (tab) {
                OwnerReservationNavTab.MY_SITES -> AppIcons.Campsite
                OwnerReservationNavTab.RESERVATIONS -> AppIcons.Reservation
                OwnerReservationNavTab.MESSAGES -> AppIcons.Chat
                OwnerReservationNavTab.NOTIFICATIONS -> AppIcons.BellIcon
                OwnerReservationNavTab.LOTTERIES -> AppIcons.LotteryTicket
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) AppColors.SurfaceDisabled else AppColors.SurfaceDeep)
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tab.label,
                    tint = if (selected) AppColors.TextOnPrimary else AppColors.Secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    tab.label,
                    color = if (selected) AppColors.TextOnPrimary else AppColors.Secondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
@Preview(name = "Owner reservations - Active", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewOwnerReservationsActive() {
    val previewReservations = listOf(
        OwnerReservationUi(
            id = "preview-active",
            campsiteName = "Campsite Name",
            location = "Campsite Location",
            status = OwnerReservationTab.ACTIVE,
            guestName = "Guest Name",
            guestCount = 2,
            dateRange = "16 – 18 Oct",
            nights = 2,
            pricePerNight = 45,
            rating = 4.9,
            confirmed = true
        ),
        OwnerReservationUi(
            id = "preview-past",
            campsiteName = "Campsite Name",
            location = "Campsite Location",
            status = OwnerReservationTab.PAST,
            guestName = "Guest Name",
            guestCount = 2,
            dateRange = "2 – 4 Oct",
            nights = 2,
            pricePerNight = 45,
            rating = 4.9
        ),
        OwnerReservationUi(
            id = "preview-cancelled",
            campsiteName = "Campsite Name",
            location = "Campsite Location",
            status = OwnerReservationTab.CANCELLED,
            guestName = "Guest Name",
            guestCount = 2,
            dateRange = "19 – 29 Oct",
            nights = 10,
            pricePerNight = 45,
            rating = 4.9,
            cancelledByGuest = true
        )
    )

    MaterialTheme {
        OwnerReservationsScreen(reservations = previewReservations)
    }
}
