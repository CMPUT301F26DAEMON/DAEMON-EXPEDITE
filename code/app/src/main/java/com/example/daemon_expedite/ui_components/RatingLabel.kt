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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A star followed by the average rating and number of reviews, for example "★ 4.9 (48 reviews)".
 * With no reviews yet it shows an empty star and a dash.
 *
 * @param average Average score, or null when there are no reviews.
 * @param reviewCount Number of reviews.
 * @param modifier [Modifier] applied to the row.
 */
@Composable
fun RatingLabel(
    average: Double?,
    reviewCount: Int,
    modifier: Modifier = Modifier,
) {
    val hasReviews = average != null && reviewCount > 0
    val text = if (hasReviews) {
        "%.1f (%d %s)".format(average, reviewCount, if (reviewCount == 1) "review" else "reviews")
    } else {
        "–"
    }
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = if (hasReviews) "Rated $text" else "No reviews yet"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = if (hasReviews) AppIcons.Star else AppIcons.StarOutline,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(16.dp),
        )
        Text(text, style = AppTextStyles.TabLabel, color = AppColors.TextPrimary)
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
