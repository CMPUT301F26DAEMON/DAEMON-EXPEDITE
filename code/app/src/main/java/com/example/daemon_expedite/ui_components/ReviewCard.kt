package com.example.daemon_expedite.ui_components
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.R
import com.example.daemon_expedite.backend.Rating

/**
 * A reusable card showing one entrant's review of a campsite.
 *
 * Displays the reviewer's profile picture and name, a row of stars for the score,
 * and the written review (if there is one).
 *
 * @param name The reviewer's display name.
 * @param score Star score; clamped to 0..[Rating.MAX_RATING].
 * @param review The written review. Hidden when null or blank.
 * @param imageUrl URL of the reviewer's profile picture. When null (or it fails to load), [fallbackImageRes] is shown.
 * @param modifier Modifier applied to the card.
 * @param fallbackImageRes Drawable shown when there is no profile picture.
 */
@Composable
fun ReviewCard(
    name: String,
    score: Int,
    review: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackImageRes: Int = R.drawable.mock_profile,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.SurfaceSheet,
        border = BorderStroke(width = 1.dp, color = AppColors.OutlineCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileImage( // Reusing ProfileImage from components
                    imageUrl = imageUrl,
                    size = 44.dp,
                    showBorder = false,
                    fallbackRes = fallbackImageRes,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text( // Name
                        text = name,
                        style = AppTextStyles.ValueEmphasis,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) { // Stars
                        repeat(Rating.MAX_RATING) { index ->
                            val starFilled = index < score.coerceIn(0, Rating.MAX_RATING)
                            Icon(
                                imageVector = if (starFilled) AppIcons.Star else AppIcons.StarOutline,
                                contentDescription = null,
                                tint = if (starFilled) AppColors.Accent else AppColors.TextMuted,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
            if (!review.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = review,
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary,
                )
            }
        }
    }
}

/** Preview: a 5-star review with a written description. */
@Preview(name = "Maya Patel - 5 Stars", showBackground = true, widthDp = 390)
@Composable
private fun MayaReviewPreview() {
    ReviewPreviewBackground {
        ReviewCard(
            name = "Maya Patel",
            score = 5,
            review = "A peaceful spot by the creek. " +
                "The site was clean, easy to find, " +
                "and perfect for a quiet weekend.",
            imageUrl = null,
        )
    }
}

/** Preview: a 5-star review with no description, so only the header row shows. */
@Preview(name = "Alex Morgan - 5 Stars", showBackground = true, widthDp = 390)
@Composable
private fun AlexReviewPreview() {
    ReviewPreviewBackground {
        ReviewCard(name = "Alex Morgan", score = 5, review = null, imageUrl = null)
    }
}

/** Preview: a 4-star review, showing an outlined (unfilled) star. */
@Preview(name = "Jordan Lee - 4 Stars", showBackground = true, widthDp = 390)
@Composable
private fun JordanReviewPreview() {
    ReviewPreviewBackground {
        ReviewCard(
            name = "Jordan Lee",
            score = 4,
            review = "Beautiful trees and plenty of space. " +
                "Bring an extra layer, the evenings get chilly by the water.",
            imageUrl = null,
        )
    }
}

/** Preview: another 5-star review with a description. */
@Preview(name = "Sam Rivera - 5 Stars", showBackground = true, widthDp = 390)
@Composable
private fun SamReviewPreview() {
    ReviewPreviewBackground {
        ReviewCard(
            name = "Sam Rivera",
            score = 5,
            review = "Loved waking up to the sound of the creek. " +
                "We’ll definitely come back for another stay.",
            imageUrl = null,
        )
    }
}

/**
 * Wraps preview content in the app background with padding so the card is shown as it looks on screen.
 *
 * @param content The preview content to show.
 */
@Composable
private fun ReviewPreviewBackground(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = AppColors.Background) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}
