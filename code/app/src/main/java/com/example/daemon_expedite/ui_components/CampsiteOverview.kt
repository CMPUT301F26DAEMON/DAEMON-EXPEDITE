// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Add the owner's campsite details screen with edit and delete", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
 * @param onViewMapClick Called when the "View on map" row is tapped.
 * @param modifier [Modifier] applied to the column.
 * @param pill Optional badge in the top-right corner of the photo, usually a [CampsitePill].
 * @param onReviewsClick Makes the rating line a link to the reviews. Null leaves it as plain text.
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
    onReviewsClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CampsiteImageCarousel(imageUrls = imageUrls, pill = pill)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, style = AppTextStyles.PageTitleMultiline, color = AppColors.TextPrimary)
            RatingLabel(average = averageRating, reviewCount = reviewCount, onClick = onReviewsClick)
        }

        if (description.isNotBlank()) {
            Text(
                description,
                style = AppTextStyles.BodyMedium.copy(lineHeight = 21.sp),
                color = AppColors.TextSecondary,
            )
        }

        if (amenities.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Amenities", style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
                AmenityChips(amenities)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StayFactsCard(maxOccupancy = maxOccupancy, pricePerNight = pricePerNight)
            ViewOnMapRow(onClick = onViewMapClick)
        }
    }
}

/** Figma "View on map": an outlined row with a pin, the label and a chevron. */
@Composable
private fun ViewOnMapRow(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AppColors.OutlineButton, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(AppIcons.MapPin, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(19.dp))
        Text("View on map", style = AppTextStyles.TitleSmall, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
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
