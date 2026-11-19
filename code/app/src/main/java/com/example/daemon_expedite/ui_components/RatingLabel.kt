// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the campsite details, policies and map screens from our Figma design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A star followed by the average rating and number of reviews, for example "★ 4.9 · 48 reviews".
 * With no reviews yet it shows an empty star and "No reviews yet".
 *
 * @param average Average score, or null when there are no reviews.
 * @param reviewCount Number of reviews.
 * @param modifier [Modifier] applied to the row.
 * @param onClick Makes the row a link to the reviews, with a "›" at the end. Only used when
 *   there are reviews to show. Null for plain text.
 */
@Composable
fun RatingLabel(
    average: Double?,
    reviewCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val hasReviews = average != null && reviewCount > 0
    val reviews = if (hasReviews) "$reviewCount ${if (reviewCount == 1) "review" else "reviews"}" else ""
    // Only a link when there are reviews to open.
    val reviewsClick = onClick?.takeIf { hasReviews }
    val clickable = reviewsClick != null
    Row(
        modifier = modifier
            .then(
                if (reviewsClick != null) {
                    Modifier
                        .heightIn(min = 30.dp) // Figma "Reviews link" height, a comfier tap target
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClickLabel = "See reviews", onClick = reviewsClick)
                } else Modifier
            )
            .clearAndSetSemantics {
                contentDescription = if (hasReviews) "Rated %.1f, $reviews".format(average) else "No reviews yet"
                if (clickable) role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (hasReviews) AppIcons.Star else AppIcons.StarOutline,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(14.dp),
        )
        if (hasReviews) {
            Text("%.1f".format(average), style = AppTextStyles.TitleSmall, color = AppColors.TextPrimary)
            Text(
                text = "·  $reviews" + if (clickable) "  ›" else "",
                style = AppTextStyles.BodyMedium.copy(fontWeight = FontWeight.Medium),
                color = AppColors.TextSecondary,
            )
        } else {
            Text("No reviews yet", style = AppTextStyles.BodyMedium.copy(fontWeight = FontWeight.Medium), color = AppColors.TextSecondary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514)
@Composable
private fun RatingLabelPreview() {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RatingLabel(average = 4.9, reviewCount = 48)
        RatingLabel(average = null, reviewCount = 0)
    }
}
