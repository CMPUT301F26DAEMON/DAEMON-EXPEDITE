package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * A popup dialog that lets the user rate something (e.g. a campsite) from 1 to [maxStars].
 *
 * @param title          The name shown at the top of the popup.
 * @param onDismiss      Called when the user taps outside the popup or presses back.
 * @param onSubmit       Called with the number of stars selected when "Submit Rating" is pressed.
 * @param maxStars       Total number of stars displayed. Defaults to 5.
 * @param initialRating  Number of stars selected when the popup first opens. Defaults to 0.
 * @param modifier       Optional [Modifier] applied to the popup card.
 */
@Composable
fun RatingPopup(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit,
    maxStars: Int = 5,
    initialRating: Int = 0,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        RatingCard(
            title = title,
            onSubmit = onSubmit,
            maxStars = maxStars,
            initialRating = initialRating,
            modifier = modifier
        )
    }
}

/**
 * The visual content of the rating popup (title, stars, submit button).
 * Separate from [RatingPopup] so it can be previewed without a Dialog window.
 *
 * @param title          The name shown at the top.
 * @param onSubmit       Called with the number of stars selected (1..[maxStars]).
 *                       Not called if no star is selected.
 * @param maxStars       Total number of stars displayed. Defaults to 5.
 * @param initialRating  Number of stars selected initially. Defaults to 0.
 * @param modifier       Optional [Modifier] applied to the card.
 */
@Composable
fun RatingCard(
    title: String,
    onSubmit: (Int) -> Unit,
    maxStars: Int = 5,
    initialRating: Int = 0,
    modifier: Modifier = Modifier
) {
    var rating by rememberSaveable { mutableIntStateOf(initialRating) }

    val selectedColor = AppColors.Accent
    val unselectedColor = AppColors.Secondary

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF181D1A),
        border = BorderStroke(1.dp, AppColors.OutlineSubtle)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = title,
                color = AppColors.TextOnPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(maxStars) { index ->
                    Icon(
                        imageVector = AppIcons.Star,
                        contentDescription = "Rate ${index + 1} star${if (index == 0) "" else "s"}",
                        tint = if (index < rating) selectedColor else unselectedColor,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { rating = index + 1 } // this star + all before it
                    )
                }
            }

            GreenButton(
                onClick = { if (rating > 0) onSubmit(rating) },
                icon = AppIcons.Tick,
                buttonText = "Submit Rating"
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RatingCardPreview() {
    RatingCard(
        title = "Pine Creek Hideaway",
        onSubmit = {},
        initialRating = 0
    )
}