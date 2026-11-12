package com.example.daemon_expedite.frontend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.daemon_expedite.ui_components.DateRangeCalendar
import com.example.daemon_expedite.ui_components.FormTextField
import java.time.LocalDate
import java.time.YearMonth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.StepperButton

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * "Filter campsites" popup, based on the Figma frame for filtering campsite listings.
 *
 * Displays location, date range, and guest count filters. Only displays the current
 * filter values and reports changes; the parent manages the values and applies
 * or resets the filters.
 *
 * @param location Current location filter.
 * @param onLocationChange Called when the user types in the location field.
 * @param startDate Selected start date, or null if none is selected.
 * @param endDate Selected end date, or null if none is selected.
 * @param today Earliest selectable date in the calendar.
 * @param onRangeChange Called when the user selects a date range.
 * @param guestCount Current number of guests.
 * @param onGuestCountChange Called with +1 or −1 from the guest buttons.
 * @param onApplyFilters Called when the user taps Apply filters.
 * @param onResetFilters Called when the user taps Reset.
 * @param onDismiss Called when the popup is closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterCampsitesSheet(
    location: String,
    onLocationChange: (String) -> Unit,
    startDate: LocalDate?,
    endDate: LocalDate?,
    today: LocalDate,
    onRangeChange: (LocalDate?, LocalDate?) -> Unit,
    guestCount: Int,
    onGuestCountChange: (Int) -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceDeep,
        scrimColor = AppColors.Scrim,
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp,
                    end = 20.dp,
                    bottom = 24.dp)
        ) {

            // Filter Campsites header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Filter Campsites",
                    style = AppTextStyles.PageTitle,
                    color = AppColors.TextPrimary
                )

                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = AppIcons.Cross,
                        contentDescription = "Close filters",
                        tint = AppColors.TextPrimary
                    )
                }
            }
            // Location section
            Text(
                text = "Location",
                style = AppTextStyles.HeadingMedium,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(top = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Location input
            FormTextField(
                label = "Enter location",
                value = location,
                onValueChange = onLocationChange,
                icon = AppIcons.MapPin
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dates section
            Text(
                text = "Dates",
                style = AppTextStyles.HeadingMedium,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            DateRangeCalendar(
                startDate = startDate,
                endDate = endDate,
                onRangeChange = onRangeChange,
                minDate = today
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Guests section

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(
                        AppColors.FillField,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        AppColors.OutlineField,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Guest icon
                Icon(
                    imageVector = AppIcons.Guests,
                    contentDescription = null,
                    tint = AppColors.TextOnPrimary,
                    modifier = Modifier.size(20.dp)
                )

                // Guest information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "GUESTS",
                        style = AppTextStyles.FieldLabel,
                        color = AppColors.Secondary
                    )

                    Text(
                        text = "Number of guests",
                        style = AppTextStyles.ValueMedium,
                        color = AppColors.TextOnPrimary
                    )
                }

                // Decrease guests
                StepperButton(
                    icon = AppIcons.Minus,
                    description = "Fewer guests",
                    enabled = guestCount > 1
                ) {
                    onGuestCountChange(-1)
                }

                // Current guest count
                Text(
                    text = guestCount.toString(),
                    style = AppTextStyles.CounterValue,
                    color = AppColors.TextOnPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(20.dp)
                )

                // Increase guests
                StepperButton(
                    icon = AppIcons.Plus,
                    description = "More guests"
                ) {
                    onGuestCountChange(1)
                }

            }
            Spacer(modifier = Modifier.height(24.dp))

            // Apply filters button
            GreenButton(
                onClick = onApplyFilters,
                buttonText = "Apply filters",
                customHeight = 58,
                customWidth = 372
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Reset filters button
            OutlinedButton(
                onClick = onResetFilters,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    AppColors.OutlineButton
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = AppColors.TextPrimary
                )
            ) {
                Text(
                    text = "Reset",
                    style = AppTextStyles.ValueMedium
                )
            }
        }
    }
}
@Preview(
    name = "Filter Campsites Popup",
    showBackground = true,
    widthDp = 412,
    heightDp = 1000
)
@Composable
private fun FilterCampsitesPreview() {
    val guestCount = 2
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Scrim)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(900.dp),
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp
            ),
            color = AppColors.SurfaceDeep
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp,
                        end = 20.dp,
                        bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filter Campsites",
                        style = AppTextStyles.PageTitle,
                        color = AppColors.TextPrimary
                    )

                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = AppIcons.Cross,
                            contentDescription = "Close filters",
                            tint = AppColors.TextPrimary
                        )
                    }
                }
                // Location section
                Text(
                    text = "Location",
                    style = AppTextStyles.HeadingMedium,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                FormTextField(
                    label = "Enter location",
                    value = "",
                    onValueChange = {},
                    icon = AppIcons.MapPin
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Dates section
                Text(
                    text = "Dates",
                    style = AppTextStyles.HeadingMedium,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                DateRangeCalendar(
                    startDate = LocalDate.of(2026, 10, 16),
                    endDate = LocalDate.of(2026, 10, 18),
                    onRangeChange = null,
                    initialMonth = YearMonth.of(2026, 10)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(
                            AppColors.FillField,
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            AppColors.OutlineField,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Guests,
                        contentDescription = null,
                        tint = AppColors.TextOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "GUESTS",
                            style = AppTextStyles.FieldLabel,
                            color = AppColors.Secondary
                        )

                        Text(
                            text = "Number of guests",
                            style = AppTextStyles.ValueMedium,
                            color = AppColors.TextOnPrimary
                        )
                    }

                    StepperButton(
                        icon = AppIcons.Minus,
                        description = "Fewer guests",
                        onClick = {}
                    )

                    Text(
                        text = guestCount.toString(),
                        style = AppTextStyles.CounterValue,
                        color = AppColors.TextOnPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(20.dp)
                    )

                    StepperButton(
                        icon = AppIcons.Plus,
                        description = "More guests",
                        onClick = {}
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                GreenButton(
                    onClick = {},
                    buttonText = "Apply filters",
                    customHeight = 58,
                    customWidth = 372
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        AppColors.OutlineButton
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = AppColors.TextPrimary
                    )
                ) {
                    Text(
                        text = "Reset",
                        style = AppTextStyles.ValueMedium
                    )
                }
            }
        }
    }
}