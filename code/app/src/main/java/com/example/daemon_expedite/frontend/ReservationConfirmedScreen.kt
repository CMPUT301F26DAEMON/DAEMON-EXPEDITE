
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.GreenButton

/**
 * Displays a confirmation after a campsite reservation succeeds.
 *
 * Reservation data and navigation callbacks are provided by the caller.
 *
 * @param campsiteName Name of the reserved campsite.
 * @param stayDates Formatted dates of the reservation.
 * @param nights Number of nights reserved.
 * @param totalPrice Formatted total reservation price.
 * @param onViewReservationsClick Called when the user views their reservations.
 * @param onBackToExploreClick Called when the user returns to Explore.
 * @param modifier Optional modifier for the screen.
 */
@Composable
fun ReservationConfirmedScreen(
    campsiteName: String,
    stayDates: String,
    nights: Int,
    totalPrice: String,
    onViewReservationsClick: () -> Unit,
    onBackToExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(horizontal = 29.dp)
    ) {
        // Prevent the shared button from exceeding the available width.
        val buttonWidth = maxWidth.coerceAtMost(356.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.2f))

            // Confirmation icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(AppColors.PrimaryTint, CircleShape)
                    .border(
                        width = 1.dp,
                        color = AppColors.Primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Tick,
                    contentDescription = "Reservation confirmed",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Confirmation heading
            Text(
                text = "Reservation confirmed",
                style = AppTextStyles.PageTitle,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(7.dp))

            // Confirmation subtitle
            Text(
                text = "You're booked at $campsiteName.",
                style = AppTextStyles.BodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Reservation summary card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(89.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(AppColors.SurfaceDeep)
                    .padding(
                        horizontal = 15.dp,
                        vertical = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: reservation information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "YOUR STAY",
                        style = AppTextStyles.SectionLabel,
                        color = AppColors.Secondary
                    )

                    Text(
                        text = campsiteName,
                        style = AppTextStyles.TitleSmall,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$stayDates · $nights nights",
                        style = AppTextStyles.Caption,
                        color = AppColors.TextSecondary
                    )
                }

                // Right side: total price
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = totalPrice,
                        style = AppTextStyles.Amount,
                        color = AppColors.TextPrimary
                    )

                    Text(
                        text = "total",
                        style = AppTextStyles.Caption,
                        color = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.8f))

            // Primary action: existing team GreenButton
            GreenButton(
                onClick = onViewReservationsClick,
                buttonText = "View my reservations",
                customHeight = 57,
                customWidth = buttonWidth.value.toInt()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary action: outlined button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(57.dp)
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = AppColors.OutlineButton
                        ),
                        RoundedCornerShape(15.dp)
                    )
                    .clip(RoundedCornerShape(15.dp))
                    .clickable(onClick = onBackToExploreClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Back to explore",
                    style = AppTextStyles.BodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = AppColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(54.dp))
        }
    }
}

/**
 * Preview of the reservation confirmation screen using sample data.
 */
@Preview(
    name = "Reservation Confirmed",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun ReservationConfirmedScreenPreview() {
    ReservationConfirmedScreen(
        campsiteName = "Pine Creek Hideaway",
        stayDates = "16–18 Oct",
        nights = 2,
        totalPrice = "$70",
        onViewReservationsClick = {},
        onBackToExploreClick = {}
    )
}
