
package com.example.daemon_expedite.frontend

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colors matching the Figma design
private val ownerBackground = Color(0xFF121514)
private val ownerCardBackground = Color(0xFF2C3029)
private val ownerSelectedGreen = Color(0xFF477C60)
private val ownerTextGray = Color(0xFFBABDB7)
private val ownerRed = Color(0xFFD56B65)
private val ownerGold = Color(0xFFF3B645)

/**
 * Reservation categories for campsite owners.
 */
enum class OwnerReservationTab(
    val label: String,
    val heading: String,
    val emptyMessage: String
) {
    ACTIVE(
        "Active",
        "Upcoming Reservations",
        "No upcoming reservations"
    ),
    PAST(
        "Past",
        "Past Reservations",
        "No past reservations"
    ),
    CANCELLED(
        "Cancelled",
        "Cancelled Reservations",
        "No cancelled reservations"
    )
}

/**
 * UI model for an owner's reservation.
 *
 * Replace with values loaded from the backend.
 */
data class OwnerReservationUi(
    val id: String,
    val campsiteName: String,
    val location: String,
    val status: OwnerReservationTab,
    val pricePerNight: Int? = null,
    val rating: Double? = null,
    val confirmed: Boolean = false,
    @DrawableRes val imageResId: Int? = null
)

/**
 * Owner Reservations screen.
 *
 * Displays active, past, and cancelled reservations.
 * No sample data is used in the actual application.
 *
 * @param reservations Reservations supplied by the backend.
 * @param onBackClick Navigates to the previous screen.
 * @param onRejectReservation Called after rejection confirmation.
 */
@Composable
fun OwnerReservationsScreen(
    reservations: List<OwnerReservationUi> = emptyList(),
    onBackClick: () -> Unit = {},
    onRejectReservation: (OwnerReservationUi) -> Unit = {}
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(OwnerReservationTab.ACTIVE)
    }

    var reservationToReject by remember {
        mutableStateOf<OwnerReservationUi?>(null)
    }

    val filteredReservations = reservations.filter {
        it.status == selectedTab
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ownerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        Spacer(Modifier.height(15.dp))

        // Top navigation and page title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
                    .size(38.dp)
                    .background(
                        Color(0xFF292E2B),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }

            Text(
                text = "My Reservations",
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Active / Past / Cancelled tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ownerCardBackground)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            OwnerReservationTab.values().forEach { tab ->

                val isSelected = selectedTab == tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) {
                                ownerSelectedGreen
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable {
                            selectedTab = tab
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = tab.label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Scrollable reservation list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Section heading changes with selected tab
            item {
                Text(
                    text = selectedTab.heading,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        bottom = 2.dp
                    )
                )
            }

            if (filteredReservations.isEmpty()) {

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedTab.emptyMessage,
                            color = ownerTextGray,
                            fontSize = 14.sp
                        )
                    }
                }

            } else {

                items(
                    items = filteredReservations,
                    key = { it.id }
                ) { reservation ->

                    OwnerReservationCard(
                        reservation = reservation,
                        onRejectClick = {
                            reservationToReject = reservation
                        }
                    )
                }
            }
        }
    }

    // Confirmation dialog before rejecting a reservation
    val pendingReservation = reservationToReject

    if (pendingReservation != null) {

        AlertDialog(
            onDismissRequest = {
                reservationToReject = null
            },
            containerColor = ownerCardBackground,
            title = {
                Text(
                    text = "Reject Reservation?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reject this reservation?",
                    color = ownerTextGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        reservationToReject = null
                        onRejectReservation(pendingReservation)
                    }
                ) {
                    Text(
                        text = "Reject",
                        color = ownerRed
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reservationToReject = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White
                    )
                }
            }
        )
    }
}

/**
 * Reusable reservation card.
 *
 * Accepts real reservation information.
 */
@Composable
private fun OwnerReservationCard(
    reservation: OwnerReservationUi,
    onRejectClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .border(
                width = 1.dp,
                color = Color(0xFF424A40),
                shape = RoundedCornerShape(13.dp)
            )
            .background(ownerCardBackground)
    ) {

        // Campsite image / empty image placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF38553F),
                            Color(0xFF17281F)
                        )
                    )
                )
        ) {

            if (reservation.imageResId != null) {

                Image(
                    painter = painterResource(
                        reservation.imageResId
                    ),
                    contentDescription = "Campsite image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Confirmed status for active reservations
            if (
                reservation.status == OwnerReservationTab.ACTIVE &&
                reservation.confirmed
            ) {

                Text(
                    text = "Confirmed",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF345B44))
                        .padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        )
                )
            }
        }

        // Campsite details
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp
            )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = reservation.campsiteName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (reservation.rating != null) {

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = ownerGold,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(Modifier.width(3.dp))

                    Text(
                        text = reservation.rating.toString(),
                        color = ownerTextGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = reservation.location,
                fontSize = 14.sp,
                color = ownerTextGray
            )

            if (reservation.pricePerNight != null) {

                Spacer(Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "$${reservation.pricePerNight}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = " / night",
                        color = ownerTextGray,
                        fontSize = 14.sp
                    )
                }
            }

            // Reject button only for active reservations
            if (reservation.status == OwnerReservationTab.ACTIVE) {

                Spacer(Modifier.height(12.dp))


                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = ownerRed
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF39352F),
                        contentColor = ownerRed
                    )
                ) {

                    // Square outline surrounding the X icon
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(
                                width = 1.7.dp,
                                color = ownerRed,
                                shape = RoundedCornerShape(5.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = ownerRed,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Reject reservation",
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ownerRed
                    )
                }
            }
        }
    }
}

/**
 * Preview data exists only for Android Studio Preview.
 * Production uses the list supplied to OwnerReservationsScreen.
 */
@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun OwnerReservationsScreenPreview() {

    val previewReservations = listOf(
        OwnerReservationUi(
            id = "preview-active",
            campsiteName = "Campsite name",
            location = "Campsite location",
            status = OwnerReservationTab.ACTIVE,
            pricePerNight = 45,
            rating = 4.9,
            confirmed = true
        ),
        OwnerReservationUi(
            id = "preview-past",
            campsiteName = "Campsite name",
            location = "Campsite location",
            status = OwnerReservationTab.PAST,
            pricePerNight = 45,
            rating = 4.9
        ),
        OwnerReservationUi(
            id = "preview-cancelled",
            campsiteName = "Campsite name",
            location = "Campsite location",
            status = OwnerReservationTab.CANCELLED,
            pricePerNight = 45,
            rating = 4.9
        )
    )

    MaterialTheme {
        OwnerReservationsScreen(
            reservations = previewReservations
        )
    }
}
