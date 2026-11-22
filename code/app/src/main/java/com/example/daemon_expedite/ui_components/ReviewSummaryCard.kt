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
 * an optional date, and the written review (if there is one).
 *
 * @param rating The [Rating] being shown; its score fills the stars and its description is the review text.
 * @param authorName The reviewer's display name.
 * @param modifier Modifier applied to the card.
 * @param dateText Optional pre-formatted date shown after the stars (e.g. "Nov 2, 2026"). Hidden when null or blank.
 * @param profileImageUrl Optional URL of the reviewer's profile picture.
 * @param fallbackImageRes Drawable shown when [profileImageUrl] is null or fails to load.
 */
@Composable
fun ReviewCard(
    rating: Rating,
    authorName: String,
    modifier: Modifier = Modifier,
    dateText: String? = null,
    profileImageUrl: String? = null,
    @DrawableRes fallbackImageRes: Int = R.drawable.mock_profile){
    ReviewCardContent(
        authorName = authorName,
        score = rating.score,
        description = rating.description,
        dateText = dateText,
        profileImageUrl = profileImageUrl,
        fallbackImageRes = fallbackImageRes,
        modifier = modifier,
    )
}

/**
 * Stateless layout behind [ReviewCard]. Takes plain values instead of a [Rating]
 * so previews can render it without building backend objects.
 *
 * @param authorName The reviewer's display name.
 * @param score Star score; clamped to 0..[Rating.MAX_RATING].
 * @param description The written review. Hidden when null or blank.
 * @param dateText Optional date shown after the stars. Hidden when null or blank.
 * @param profileImageUrl Optional URL of the reviewer's profile picture.
 * @param fallbackImageRes Drawable shown when there is no profile picture.
 * @param modifier Modifier applied to the card.
 */
@Composable
private fun ReviewCardContent(
    authorName: String,
    score: Int,
    description: String?,
    dateText: String?,
    profileImageUrl: String? = null,
    @DrawableRes fallbackImageRes: Int = R.drawable.mock_profile,
    modifier: Modifier = Modifier){Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.SurfaceSheet,
        border = BorderStroke(
            width = 1.dp,
            color = AppColors.OutlineCard)){Column(
            modifier = Modifier.padding(16.dp))
{Row(verticalAlignment = Alignment.CenterVertically)
{ProfileImage( //Reusing ProfileImage from component
                    imageUrl = profileImageUrl,
                    size = 44.dp,
                    showBorder = false,
                    fallbackRes = fallbackImageRes)
                Spacer(
                    modifier = Modifier.width(12.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp))
                {
                    Text(  //Name
                        text = authorName,
                        style = AppTextStyles.ValueEmphasis,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row( // Stars
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            repeat(Rating.MAX_RATING) { index ->
                                val starsFilled = index < score.coerceIn(
                                    0,
                                    Rating.MAX_RATING
                                )
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
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                        if (!dateText.isNullOrBlank()) {
                            Text( // Placeholder for date
                                text = "  ·  $dateText",
                                style = AppTextStyles.Caption,
                                color = AppColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (!description.isNullOrBlank()) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )
                Text(
                    text = description,
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary,
                )
            }
        }
    }
}
/** Preview: a 5-star review with a written description. */
@Preview(
    name = "Maya Patel - 5 Stars",
    showBackground = true,
    widthDp = 390)
@Composable
private fun MayaReviewPreview() {
    ReviewPreviewBackground {
        ReviewCardContent(
            authorName = "Maya Patel",
            score = 5,
            dateText = "Nov 2, 2026",
            description = "A peaceful spot by the creek. " +
                    "The site was clean, easy to find, " +
                    "and perfect for a quiet weekend.",
        )
    }
}
/** Preview: a 5-star review with no description, so only the header row shows. */
@Preview(name="Alex Morgan - 5 Stars",showBackground = true,widthDp = 390)
@Composable
private fun AlexReviewPreview(){
    ReviewPreviewBackground{
        ReviewCardContent(
            authorName = "Alex Morgan",
            score = 5,
            dateText = "Oct 24, 2026",
            description = null,
        )
    }
}
/** Preview: a 4-star review, showing an outlined (unfilled) star. */
@Preview(name = "Jordan Lee - 4 Stars",showBackground= true,widthDp = 390)
@Composable
private fun JordanReviewPreview() {
    ReviewPreviewBackground {
        ReviewCardContent(
            authorName = "Jordan Lee",
            score = 4,
            dateText = "Oct 11, 2026",
            description = "Beautiful trees and plenty of space. " +"Bring an extra layer, the evenings "+"get chilly by the water.",
        )
    }
}
/** Preview: another 5-star review with a description. */
@Preview(name= "Sam Rivera - 5 Stars",showBackground=true,widthDp = 390)
@Composable
private fun SamReviewPreview(){
    ReviewPreviewBackground{
        ReviewCardContent(
            authorName = "Sam Rivera",
            score = 5,
            dateText = "Sep 29, 2026",
            description = "Loved waking up to the sound of the creek. " +"We’ll definitely come back for another stay."
        )
    }
}
/**
 * Wraps preview content in the app background with padding so the card is shown as it looks on screen.
 *
 * @param content The preview content to show.
 */
@Composable
private fun ReviewPreviewBackground(
    content: @Composable () -> Unit,
){Surface(modifier =Modifier.fillMaxWidth(),color = AppColors.Background){Box(modifier =Modifier.padding(16.dp)){content()}}
}