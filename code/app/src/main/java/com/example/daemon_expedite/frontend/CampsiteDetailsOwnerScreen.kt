
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.GreenButton

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Displays campsite information for a campsite owner.
 *
 * TODO: Connect campsite details and bookings to Firestore.
 * TODO: Connect map and waitlist navigation.
 */
@Composable
fun CampsiteDetailsOwnerScreen(
    campsiteName: String = "Pine Creek Hideaway",
    description: String = "This is the campsite description!",
    rating: String = "4.9",
    reviewCount: Int = 48,
    maxOccupancy: Int = 6,
    pricePerNight: Int = 45,
    amenities: List<String> = listOf("Has bathroom", "Water"),
    photoResIds: List<Int> = emptyList(),
    bookedDates: Set<LocalDate> = setOf(
        LocalDate.of(2026, 12, 16),
        LocalDate.of(2026, 12, 17),
        LocalDate.of(2026, 12, 18)
    ),
    onBackClick: () -> Unit = {},
    onViewMapClick: () -> Unit = {},
    onViewWaitlistClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Component 1: Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onClick = onBackClick)

            Spacer(Modifier.weight(1f))

            Text(
                text = "Campsite Details",
                style = AppTextStyles.HeadingSmall,
                color = AppColors.TextOnPrimary
            )
        }

        Spacer(Modifier.height(22.dp))

        // Component 2: Campsite photo carousel
        CampsitePhotoCarousel(
            photoResIds = photoResIds
        )

        Spacer(Modifier.height(24.dp))

        // Component 3: Name and rating
        Text(
            text = campsiteName,
            style = AppTextStyles.PageTitle,
            color = AppColors.TextOnPrimary
        )

        Spacer(Modifier.height(7.dp))

        Text(
            text = "$rating ($reviewCount reviews)",
            style = AppTextStyles.BodySmall,
            color = AppColors.TextPrimary
        )

        Spacer(Modifier.height(19.dp))

        // Component 4: Description

        Text(
            text = description,
            style = AppTextStyles.BodySmallMultiline,
            color = AppColors.TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        // Component 5: Amenities
        Text(
            text = "Amenities",
            style = AppTextStyles.HeadingSmall,
            color = AppColors.TextOnPrimary
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            amenities.forEach { amenity ->
                OwnerAmenityChip(label = amenity)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Component 6: Occupancy and price
        OccupancyPriceCard(
            maxOccupancy = maxOccupancy,
            pricePerNight = pricePerNight
        )

        Spacer(Modifier.height(20.dp))

        // Component 7: View on map
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            GreenButton(
                onClick = onViewMapClick,
                icon = Icons.Default.LocationOn,
                buttonText = "View on map",
                customWidth = maxWidth.value.toInt(),
                customHeight = 56
            )
        }

        Spacer(Modifier.height(26.dp))

        // Component 8: Booked dates
        Text(
            text = "Booked Dates",
            style = AppTextStyles.HeadingMedium,
            color = AppColors.TextOnPrimary
        )

        Spacer(Modifier.height(14.dp))

        BookedDatesCalendar(
            bookedDates = bookedDates
        )

        Spacer(Modifier.height(16.dp))

        // Component 9: View Waitlist
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            GreenButton(
                onClick = onViewWaitlistClick,
                icon = Icons.Default.Assignment,
                buttonText = "View Waitlist",
                customWidth = maxWidth.value.toInt(),
                customHeight = 56
            )
        }

        Spacer(Modifier.height(26.dp))
    }
}

/**
 * Displays campsite images with navigation controls.
 * Uses a placeholder when no image resources are provided.
 */
@Composable
private fun CampsitePhotoCarousel(
    photoResIds: List<Int>
) {
    var currentPhoto by rememberSaveable(photoResIds.size) {
        mutableIntStateOf(0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceDeep)
    ) {
        // Image or placeholder
        if (photoResIds.isNotEmpty()) {
            Image(
                painter = painterResource(
                    photoResIds[currentPhoto % photoResIds.size]
                ),
                contentDescription = "Campsite photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Landscape,
                    contentDescription = null,
                    tint = AppColors.Secondary,
                    modifier = Modifier.size(42.dp)
                )

                Text(
                    text = "Campsite photo",
                    style = AppTextStyles.Caption,
                    color = AppColors.TextSecondary
                )
            }
        }

        // Lottery badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(
                    AppColors.SurfaceDeep,
                    RoundedCornerShape(20.dp)
                )
                .border(
                    1.dp,
                    AppColors.Accent,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = AppIcons.LotteryTicket,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = "Lottery · Active",
                style = AppTextStyles.Badge,
                color = AppColors.TextOnPrimary
            )
        }

        // Previous and next photo buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    currentPhoto =
                        (currentPhoto - 1 + photoResIds.size) %
                                photoResIds.size
                },
                enabled = photoResIds.size > 1,
                modifier = Modifier.background(
                    AppColors.SurfaceDeep,
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous photo",
                    tint = AppColors.TextOnPrimary
                )
            }

            IconButton(
                onClick = {
                    currentPhoto =
                        (currentPhoto + 1) % photoResIds.size
                },
                enabled = photoResIds.size > 1,
                modifier = Modifier.background(
                    AppColors.SurfaceDeep,
                    CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next photo",
                    tint = AppColors.TextOnPrimary
                )
            }
        }

        // Photo position indicators
        if (photoResIds.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                photoResIds.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .width(
                                if (index == currentPhoto) 20.dp
                                else 6.dp
                            )
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPhoto)
                                    AppColors.TextOnPrimary
                                else AppColors.TextSecondary
                            )
                    )
                }
            }
        }
    }
}

/**
 * Read-only amenity badge.
 */
@Composable
private fun OwnerAmenityChip(
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Primary)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = AppIcons.Tick,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = label,
            style = AppTextStyles.Badge,
            color = Color.White
        )
    }
}

/**
 * Read-only campsite occupancy and nightly price.
 */
@Composable
private fun OccupancyPriceCard(
    maxOccupancy: Int,
    pricePerNight: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceDeep)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = AppColors.TextPrimary,
            modifier = Modifier.size(26.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "MAX OCCUPANCY",
                style = AppTextStyles.SectionLabel,
                color = AppColors.TextSecondary
            )

            Text(
                text = "$maxOccupancy guests",
                style = AppTextStyles.ValueEmphasis,
                color = AppColors.TextPrimary
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$$pricePerNight",
                style = AppTextStyles.Amount,
                color = AppColors.TextOnPrimary
            )

            Text(
                text = "per night",
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Calendar displaying booked dates.
 *
 * Defaults to December 2026 to match the Figma sample.
 * Month arrows work in both directions.
 */
@Composable
private fun BookedDatesCalendar(
    bookedDates: Set<LocalDate>
) {
    var monthOffset by rememberSaveable {
        mutableIntStateOf(0)
    }

    val initialMonth = YearMonth.of(2026, 12)
    val displayedMonth =
        initialMonth.plusMonths(monthOffset.toLong())

    val formatter = remember {
        DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            Locale.ENGLISH
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AppColors.OutlineStrong
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { monthOffset-- }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = AppColors.TextPrimary
                    )
                }

                Text(
                    text = displayedMonth.format(formatter),
                    style = AppTextStyles.TabLabel,
                    color = AppColors.TextPrimary
                )

                IconButton(
                    onClick = { monthOffset++ }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = AppColors.TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Weekday labels, Monday first
            val weekdays = listOf(
                "M", "T", "W", "T", "F", "S", "S"
            )

            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = AppTextStyles.Caption,
                            color = AppColors.Secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Monday = 1, Sunday = 7
            val leadingBlanks =
                displayedMonth.atDay(1).dayOfWeek.value - 1

            val daysInMonth =
                displayedMonth.lengthOfMonth()

            val cellCount =
                ((leadingBlanks + daysInMonth + 6) / 7) * 7

            repeat(cellCount / 7) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(7) { weekday ->
                        val dayNumber =
                            week * 7 + weekday -
                                    leadingBlanks + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                val date =
                                    displayedMonth.atDay(dayNumber)

                                val isBooked =
                                    date in bookedDates

                                val joinsPrevious =
                                    date.minusDays(1) in bookedDates &&
                                            weekday > 0

                                val joinsNext =
                                    date.plusDays(1) in bookedDates &&
                                            weekday < 6

                                val shape = RoundedCornerShape(
                                    topStart = if (joinsPrevious)
                                        0.dp else 18.dp,
                                    bottomStart = if (joinsPrevious)
                                        0.dp else 18.dp,
                                    topEnd = if (joinsNext)
                                        0.dp else 18.dp,
                                    bottomEnd = if (joinsNext)
                                        0.dp else 18.dp
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isBooked)
                                                AppColors.Primary
                                            else Color.Transparent,
                                            if (isBooked)
                                                shape
                                            else RoundedCornerShape(0.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = if (isBooked)
                                            AppTextStyles.CalendarDateSelected
                                        else AppTextStyles.BodySmall,
                                        color = if (isBooked)
                                            Color.White
                                        else AppColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Android Studio Compose Preview.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514,

)
@Composable
private fun CampsiteDetailsOwnerPreview() {
    CampsiteDetailsOwnerScreen()
}
