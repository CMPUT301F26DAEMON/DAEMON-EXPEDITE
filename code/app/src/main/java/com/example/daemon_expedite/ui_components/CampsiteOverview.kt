package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The top half of a campsite details page, shared by the entrant and owner views: photos,
 * name, rating, description, amenities, occupancy and price, and "View on map".
 *
 * Built from the "Campsite Detail" frames in the Figma file. Sections are 24 dp apart; the
 * name, rating and description sit closer together.
 *
 * @param name Campsite name.
 * @param imageUrls Photo download URLs for the carousel.
 * @param averageRating Average score, or null when there are no reviews.
 * @param reviewCount Number of reviews.
 * @param description Campsite description. Hidden when blank.
 * @param amenities Amenity tags. The section is hidden when empty.
 * @param maxOccupancy Maximum number of guests.
 * @param pricePerNight Already formatted price, for example "$45".
 * @param onViewMapClick Called when "View on map" is tapped.
 * @param modifier [Modifier] applied to the column.
 * @param pill Optional badge in the top-right corner of the photo, usually a [CampsitePill].
 */
@Composable
fun CampsiteOverview(
    name: String,
    imageUrls: List<String>,
    averageRating: Double?,
    reviewCount: Int,
    description: String,
    amenities: List<String>,
    maxOccupancy: Int,
    pricePerNight: String,
    onViewMapClick: () -> Unit,
    modifier: Modifier = Modifier,
    pill: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CampsiteImageCarousel(imageUrls = imageUrls, pill = pill)

        Column {
            Text(name, style = AppTextStyles.PageTitleMultiline, color = AppColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            RatingLabel(average = averageRating, reviewCount = reviewCount)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(description, style = AppTextStyles.BodySmallMultiline, color = AppColors.TextPrimary)
            }
        }

        if (amenities.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Amenities", style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
                AmenityChips(amenities)
            }
        }

        StayFactsCard(maxOccupancy = maxOccupancy, pricePerNight = pricePerNight)

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            GreenButton(
                onClick = onViewMapClick,
                icon = AppIcons.MapPin,
                buttonText = "View on map",
                customWidth = maxWidth.value.toInt(),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412, heightDp = 900)
@Composable
private fun CampsiteOverviewPreview() {
    CampsiteOverview(
        name = "Pine Creek Hideaway",
        imageUrls = emptyList(),
        averageRating = 4.9,
        reviewCount = 48,
        description = "This is the campsite description!",
        amenities = listOf("Has bathroom", "Water"),
        maxOccupancy = 6,
        pricePerNight = "$45",
        onViewMapClick = {},
        modifier = Modifier.padding(28.dp),
        pill = { CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = AppIcons.LotteryTicket) },
    )
}
