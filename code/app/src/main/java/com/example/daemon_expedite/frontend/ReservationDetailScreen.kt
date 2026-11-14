package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.ScreenHeader

/**
 * Detailed view of a reservation. Opened from "View Reservation Details" on Lottery Details,
 * similar to how "View on map" opens the map screen on Campsite Details.
 *
 * @param campsiteName Name of the reserved campsite.
 * @param stayDates Formatted dates of the reservation.
 * @param nights Number of nights reserved.
 * @param totalPrice Formatted total price.
 * @param onBackClick Called when the back button is clicked.
 * @param modifier [Modifier] applied to the screen layout.
 */
@Composable
fun ReservationDetailScreen(
    campsiteName: String,
    stayDates: String,
    nights: Int,
    totalPrice: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = "Reservation Details", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Confirmation icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(AppColors.PrimaryTint, CircleShape)
                    .border(
                        width = 1.dp,
                        color = AppColors.Primary,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Tick,
                    contentDescription = "Reservation confirmed",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Confirmed Booking",
                style = AppTextStyles.PageTitle,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You are booked at $campsiteName",
                style = AppTextStyles.BodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Details Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Surface, RoundedCornerShape(20.dp))
                    .border(1.dp, AppColors.OutlineStrong, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ReservationInfoRow(label = "CAMPSITE", value = campsiteName)
                ReservationInfoRow(label = "STAY DATES", value = stayDates)
                ReservationInfoRow(label = "DURATION", value = if (nights == 1) "1 night" else "$nights nights")
                ReservationInfoRow(label = "TOTAL PAID", value = totalPrice)
            }

            Spacer(modifier = Modifier.height(32.dp))

            GreenButton(
                onClick = onBackClick,
                buttonText = "Done",
                customHeight = 52,
                customWidth = 280,
            )
        }
    }
}

@Composable
private fun ReservationInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTextStyles.FieldLabel,
            color = AppColors.TextSecondary,
        )
        Text(
            text = value,
            style = AppTextStyles.ValueEmphasis,
            color = AppColors.TextOnPrimary,
        )
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun ReservationDetailScreenPreview() {
    ReservationDetailScreen(
        campsiteName = "Pine Creek Campground",
        stayDates = "16 Oct → 18 Oct",
        nights = 2,
        totalPrice = "$90",
        onBackClick = {},
    )
}
