package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview

/**
 * A reusable circular button for incrementing or decrementing a value.
 *
 * The composable is stateless: it displays the provided [icon] and reports
 * clicks through [onClick]. The parent manages the value being changed
 * and determines whether the button is enabled.
 *
 * Usage:
 * ```
 * StepperButton(
 *     icon = AppIcons.Minus,
 *     description = "Fewer guests",
 *     enabled = guestCount > 1,
 *     onClick = { onGuestCountChange(-1) }
 * )
 * ```
 *
 * @param icon Icon displayed inside the button.
 * @param description Accessibility description of the button.
 * @param enabled Whether the button can be clicked. Defaults to true.
 * @param onClick Called when the button is clicked.
 */
@Composable
fun StepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AppColors.FillSubtle)
            .border(
                1.dp,
                AppColors.OutlineStrong,
                CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) AppColors.TextPrimary
            else AppColors.TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(
    name = "Stepper Buttons",
    showBackground = true
)
@Composable
private fun StepperButtonPreview() {
    Row(
        modifier = Modifier
            .background(AppColors.SurfaceOverlay)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            icon = AppIcons.Minus,
            description = "Fewer guests",
            enabled = true,
            onClick = {}
        )

        StepperButton(
            icon = AppIcons.Plus,
            description = "More guests",
            enabled = true,
            onClick = {}
        )
    }
}