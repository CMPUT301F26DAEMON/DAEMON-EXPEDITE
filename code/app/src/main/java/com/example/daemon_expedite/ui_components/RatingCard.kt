package com.example.daemon_expedite.ui_components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
/**
 * A popup dialog that lets the user rate a campsite from 1 to [maxStars]
 * and optionally write a review.
 *
 * @param title               The campsite name shown at the top of the popup.
 * @param onDismiss           Called when the user taps outside the popup or presses back.
 * @param onSubmit            Called with the number of stars selected.
 * @param maxStars            Total number of stars displayed. Defaults to 5.
 * @param initialRating       Number of stars selected when the popup opens. Defaults to 0.
 * @param modifier            Optional [Modifier] applied to the popup card.
 * @param onSubmitWithReview  Optional callback returning the rating and written review.
 */
@Composable
fun RatingPopup(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit,
    maxStars: Int = 5,
    initialRating: Int = 0,
    modifier: Modifier = Modifier,
    onSubmitWithReview: ((Int, String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        RatingCard(
            title = title,
            onSubmit = onSubmit,
            maxStars = maxStars,
            initialRating = initialRating,
            modifier = modifier.fillMaxWidth(0.86f),
            onSubmitWithReview = onSubmitWithReview
        )
    }
}
/**
 * The visual content of the rating popup (title, stay details, stars, review field, submit button).
 * Separate from [RatingPopup] so it can be previewed without a Dialog window.
 *
 * @param title               The campsite name shown at the top.
 * @param onSubmit            Called with the number of stars selected (1..[maxStars]).
 *                            Not called if no star is selected.
 * @param maxStars            Total number of stars displayed. Defaults to 5.
 * @param initialRating       Number of stars selected initially. Defaults to 0.
 * @param modifier            Optional [Modifier] applied to the card.
 * @param onSubmitWithReview  Optional callback returning the rating and written review.
 */
@Composable
fun RatingCard(
    title: String,
    onSubmit: (Int) -> Unit,
    maxStars: Int = 5,
    initialRating: Int = 0,
    modifier: Modifier = Modifier,
    onSubmitWithReview: ((Int, String) -> Unit)? = null
) {var rating by rememberSaveable(title, maxStars) {mutableIntStateOf(initialRating.coerceIn(0, maxStars))}

    var reviewText by rememberSaveable(title) {
        mutableStateOf("")
    }
    val selectedColor = AppColors.Accent
    val unselectedColor = AppColors.Secondary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.SurfaceDeep,
        border = BorderStroke(
            1.dp,
            AppColors.OutlineSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title,
                style = AppTextStyles.CardTitle,
                color = AppColors.TextOnPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(maxStars) { index ->
                    Icon(
                        imageVector = AppIcons.Star,
                        contentDescription = "Rate ${index + 1} star${if (index == 0) "" else "s"}",
                        tint = if (index < rating) selectedColor else unselectedColor,
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .size(26.dp)
                            .clickable(
                                interactionSource = remember {
                                    MutableInteractionSource()
                                },
                                indication = null
                            ) {
                                rating = index + 1
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BasicTextField(
                value = reviewText,
                onValueChange = { reviewText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                textStyle = AppTextStyles.CaptionMultiline.copy(color = AppColors.TextOnPrimary),
                cursorBrush = SolidColor(AppColors.Primary),
                decorationBox = { innerTextField ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(11.dp),
                        color = AppColors.FillField,
                        border = BorderStroke(
                            1.dp,
                            AppColors.OutlineSubtle
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            if (reviewText.isEmpty()) {
                                Text(
                                    text = "What was your stay like? (Optional)",
                                    style = AppTextStyles.CaptionMultiline,
                                    color = AppColors.TextSecondary
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            GreenButton(
                onClick = {
                    if (rating > 0) {
                        if (onSubmitWithReview != null) {
                            onSubmitWithReview(rating, reviewText)
                        } else {
                            onSubmit(rating)
                        }
                    }
                },
                icon = null,
                buttonText = "Submit review",
                customHeight = 40,
                customWidth = 356
            )
        }
    }
}

/**
 * Preview of the rating card with three stars selected.
 * Uses the existing app theme colours and components.
 */
@Preview(showBackground = true)
@Composable
private fun RatingCardPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.Background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            RatingCard(title = "Pine Creek Hideaway", onSubmit = {},initialRating = 3, modifier = Modifier.fillMaxWidth(0.86f)) } }
}