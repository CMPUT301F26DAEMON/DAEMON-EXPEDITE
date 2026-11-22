
package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.backend.Rating

/**
 * A reusable card summarizing all the ratings for a campsite.
 *
 * The left side shows the average score in large text, a row of stars (average rounded
 * to the nearest whole star) and the total review count. The right side shows one bar
 * per star level, filled in proportion to how many reviews gave that score.
 *
 * @param scores Every individual star score for the campsite (1..[Rating.MAX_RATING]).
 * @param modifier Modifier applied to the card.
 */
@Composable
fun RatingSummaryCard(scores: List<Int>, modifier: Modifier = Modifier) {
    RatingSummaryCardContent(
        scores = scores,
        averageRating = if (scores.isEmpty()) 0.0 else scores.average(),
        modifier = modifier,
    )
}

/**
 * Layout behind [RatingSummaryCard], given the scores and their average.
 *
 * @param scores Every individual star score (1..[Rating.MAX_RATING]).
 * @param averageRating The average of [scores], shown to one decimal place.
 * @param modifier Modifier applied to the card.
 */
@Composable
private fun RatingSummaryCardContent(
    scores: List<Int>,
    averageRating: Double,
    modifier: Modifier = Modifier,
) {val reviewCount = scores.size

    // Count number of reviews
    val starCounts = (1..Rating.MAX_RATING).associateWith { star ->
        scores.count{it == star}}
    //Count average rating
    val filledStars = (averageRating+0.5).toInt()
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.SurfaceSheet)
            .border(
                width = 1.dp,
                color = AppColors.OutlineCard,
                shape = shape,
            )
            .padding(
                horizontal = 20.dp,
                vertical = 20.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Bigger font rating
        Column(
            modifier = Modifier.weight(0.38f),
            verticalArrangement = Arrangement.Center,
        ) {Text(text = "%.1f".format(averageRating),
                style = AppTextStyles.PageTitle.copy(
                    fontSize = 49.sp,
                    lineHeight = 54.sp,
                    color = AppColors.TextOnPrimary,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(  //Stars
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                repeat(Rating.MAX_RATING) { index ->
                    val starsFilled = index <filledStars

                    Icon(
                        imageVector = if (starsFilled) {
                            AppIcons.Star
                        } else {
                            AppIcons.StarOutline
                        },
                        contentDescription = null,
                        tint = if (starsFilled) {
                            AppColors.Accent
                        } else {
                            AppColors.TextMuted
                        },
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            // Review Count
            Text(
                text = "$reviewCount ${if (reviewCount == 1) "review" else "reviews"}",
                style = AppTextStyles.BodySmall.copy(
                    color = AppColors.TextSecondary,
                ),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(0.62f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (star in Rating.MAX_RATING downTo 1) {

                val count = starCounts[star] ?: 0
                val fraction = if (reviewCount == 0) {  // Rating bar
                    0f} else {
                    count.toFloat() / reviewCount
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$star",
                        style = AppTextStyles.Caption.copy(
                            color = AppColors.TextSecondary,
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(10.dp),
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(AppColors.FillChip),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(
                                    AppColors.Accent,
                                    CircleShape,
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = "$count",
                        style = AppTextStyles.Caption.copy(
                            color = AppColors.TextMuted),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(18.dp))
                }
            }
        }
    }
}
/**
 * Preview for testing the rating summary card
 */
@Preview(showBackground = true,backgroundColor = 0xFF121514,widthDp = 390)
@Composable
private fun RatingSummaryCardPreview() {
    val exampleScores = List(21){5}+ List(3) {4}
    Box(
        modifier = Modifier
            .background(AppColors.Background)
            .padding(16.dp),
    ){RatingSummaryCardContent(scores = exampleScores,averageRating =exampleScores.average())}}
