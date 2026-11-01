package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.DateRangeCalendar
import com.example.daemon_expedite.ui_components.YellowButton
import java.time.LocalDate

/**
 * Displays the campsite owner's Start Lottery screen.
 *
 * Allows an owner to configure lottery details, including campsite
 * selection, registration dates, booking dates, and draw settings.
 *
 * @param onBackClick Callback invoked when the back button is pressed.
 */
@Composable
fun StartLotteryScreen(
    onBackClick: () -> Unit = {}
) {
    var bookingStartDate by remember {
        mutableStateOf<LocalDate?>(null)
    }

    var bookingEndDate by remember {
        mutableStateOf<LocalDate?>(null)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onClick = onBackClick)

            Text(
                text = "Add Lottery",
                style = AppTextStyles.PageTitleLarge,
                color = AppColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Lottery Details",
            style = AppTextStyles.PageTitle,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select Campsites",
            style = AppTextStyles.HeadingMedium,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    color = AppColors.FillField,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.OutlineStrong,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.LotteryTicket, // Replace Later with CAMP icon
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "SELECT CAMPSITES",
                style = AppTextStyles.FieldContent,
                color = AppColors.TextSecondary,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select campsites",
                tint = AppColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Registration period",
            style = AppTextStyles.HeadingMedium,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    color = AppColors.FillField,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.OutlineStrong,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Calendar,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "OPEN DATE",
                style = AppTextStyles.FieldContent,
                color = AppColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(15.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    color = AppColors.FillField,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.OutlineStrong,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Calendar,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "CLOSE DATE",
                style = AppTextStyles.FieldContent,
                color = AppColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Booking period",
            style = AppTextStyles.HeadingMedium,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        DateRangeCalendar(
            startDate = bookingStartDate,
            endDate = bookingEndDate,
            onRangeChange = { start, end ->
                bookingStartDate = start
                bookingEndDate = end
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Draw settings",
            style = AppTextStyles.HeadingMedium,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    color = AppColors.FillField,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.OutlineStrong,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { }
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Trophy,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "NUMBER OF WINNERS",
                style = AppTextStyles.FieldContent,
                color = AppColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(15.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(
                    color = AppColors.FillField,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = AppColors.OutlineStrong,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { }
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Waitlist,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "WAITLIST CAPACITY",
                style = AppTextStyles.FieldContent,
                color = AppColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Share your campsite",
            style = AppTextStyles.HeadingMedium,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .border(
                    width = 1.dp,
                    color = AppColors.Primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    // TODO: Generate QR code
                }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.QRCode,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Generate QR Code",
                style = AppTextStyles.ButtonLabel,
                color = AppColors.Primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        YellowButton(
            onClick = {
                // TODO: Connect lottery creation logic
            },
            icon = AppIcons.Tick,
            buttonText = "Create Lottery"
        )
    }
}

/**
 * Preview of the Start Lottery screen.
 */
@Preview(showBackground = true,
    heightDp = 1500)
@Composable
private fun StartLotteryScreenPreview() {
    StartLotteryScreen()
}