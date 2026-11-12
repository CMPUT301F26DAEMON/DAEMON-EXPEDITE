
package com.example.daemon_expedite.ui_components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Card states, independent of the frontend screen's tabs. */
enum class OwnerReservationCardStatus {
    ACTIVE,
    PAST,
    CANCELLED
}

/**
 * Shared reservation card for the owner's Active, Past, and Cancelled tabs.
 *
 * This component has no dependency on frontend models or navigation.
 * Data is passed in by the caller and the Reject button delegates
 * through a callback.
 */
@Composable
fun OwnerReservationCard(
    campsiteName: String,
    location: String,
    status: OwnerReservationCardStatus,
    onRejectClick: () -> Unit,
    modifier: Modifier = Modifier,
    pricePerNight: Int? = null,
    rating: Double? = null,
    confirmed: Boolean = false,
    @DrawableRes imageResId: Int? = null,
    guestName: String? = null,
    guestCount: Int? = null,
    dateRange: String? = null,
    nights: Int? = null,
    cancelledByGuest: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .border(
                1.dp,
                AppColors.OutlineStrong,
                RoundedCornerShape(14.dp)
            )
    ) {

        // Campsite image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AppColors.Primary,
                            AppColors.SurfaceDeep
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            if (imageResId != null) {
                Image(
                    painter = painterResource(imageResId),
                    contentDescription = "$campsiteName campsite",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder until an image is supplied
                Icon(
                    imageVector = AppIcons.Campsite,
                    contentDescription = null,
                    tint = AppColors.Secondary,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Confirmed badge
            if (
                status == OwnerReservationCardStatus.ACTIVE &&
                confirmed
            ) {
                ReservationStatusBadge(
                    label = "Confirmed",
                    isCancelled = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                )
            }

            // Cancelled badge
            else if (
                status == OwnerReservationCardStatus.CANCELLED
            ) {
                ReservationStatusBadge(
                    label = if (cancelledByGuest) {
                        "Cancelled by guest"
                    } else {
                        "Cancelled"
                    },
                    isCancelled = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                )
            }
        }

        // Campsite details
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                top = 10.dp,
                bottom = if (
                    status == OwnerReservationCardStatus.ACTIVE
                ) 8.dp else 14.dp
            )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = campsiteName,
                    modifier = Modifier.weight(1f),
                    color = AppColors.TextOnPrimary,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Rating
                if (rating != null) {

                    Spacer(Modifier.width(6.dp))

                    Icon(
                        imageVector = AppIcons.Star,
                        contentDescription = "Rating",
                        tint = AppColors.Accent,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(Modifier.width(3.dp))

                    Text(
                        text = rating.toString(),
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }


            Spacer(Modifier.height(1.dp))

            // Guest details
            val isCancelled =
                status == OwnerReservationCardStatus.CANCELLED

            val guestDescription = if (isCancelled) {
                // Cancelled: Guest Name · 19 – 29 Oct
                listOfNotNull(guestName, dateRange)
                    .joinToString(" · ")
                    .ifEmpty { location }
            } else {
                // Active/Past: Guest Name · 2 guests
                if (guestName != null) {
                    if (guestCount != null) {
                        "$guestName · $guestCount guests"
                    } else {
                        guestName
                    }
                } else {
                    location
                }
            }

            Text(
                text = guestDescription,
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )

            // Show dates on a separate line only for Active/Past
            if (!isCancelled && dateRange != null) {

                Spacer(Modifier.height(1.dp))

                val duration = nights?.let {
                    " · $it ${if (it == 1) "night" else "nights"}"
                }.orEmpty()

                Text(
                    text = "$dateRange$duration",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Price per night
            if (pricePerNight != null) {


                Spacer(Modifier.height(1.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "$${pricePerNight}",
                        color = AppColors.TextOnPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    Text(
                        text = " / night",
                        color = AppColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Reject button for active reservations
            if (status == OwnerReservationCardStatus.ACTIVE) {

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRejectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        AppColors.Danger
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppColors.DangerTint,
                        contentColor = AppColors.Danger
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp
                    )
                ) {

                    Icon(
                        imageVector = AppIcons.CancelCircle,
                        contentDescription = null,
                        tint = AppColors.Danger,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Reject reservation",
                        color = AppColors.Danger,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun ReservationStatusBadge(
    label: String,
    isCancelled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.SurfaceDeep)
            .border(
                width = 1.dp,
                color = if (isCancelled) {
                    AppColors.Danger
                } else {
                    AppColors.OutlineAccent
                },
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        if (!isCancelled) {
            Icon(
                imageVector = AppIcons.CircleCheck,
                contentDescription = null,
                tint = AppColors.TextOnPrimary,
                modifier = Modifier.size(11.dp)
            )
        }

        Text(
            text = label,
            color = AppColors.TextOnPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
