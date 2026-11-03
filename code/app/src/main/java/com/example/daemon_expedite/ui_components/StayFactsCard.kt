package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Card showing a campsite's max occupancy on the left and nightly price on the right.
 *
 * Built from the "Stay facts" frame in the Figma file.
 *
 * @param maxOccupancy Maximum number of guests.
 * @param pricePerNight Already formatted price, for example "$45".
 * @param modifier [Modifier] applied to the card.
 */
@Composable
fun StayFactsCard(
    maxOccupancy: Int,
    pricePerNight: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Min height rather than fixed: the price and "per night" are taller than
            // 72dp minus 16dp padding, which cut off the bottom line.
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDeep)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(AppIcons.Guests, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("MAX OCCUPANCY", style = AppTextStyles.Overline, color = AppColors.Secondary)
                Text(
                    text = if (maxOccupancy == 1) "1 guest" else "$maxOccupancy guests",
                    style = AppTextStyles.ValueEmphasis,
                    color = AppColors.TextPrimary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(pricePerNight, style = AppTextStyles.Amount, color = AppColors.TextPrimary)
            Text("per night", style = AppTextStyles.Caption, color = AppColors.TextPrimary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun StayFactsCardPreview() {
    StayFactsCard(maxOccupancy = 6, pricePerNight = "$45", modifier = Modifier.padding(12.dp))
}
