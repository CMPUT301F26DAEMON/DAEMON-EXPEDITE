package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A read-only green tag for one amenity, such as "Has bathroom".
 *
 * Built from the "Amenity tag" frame in the Figma file.
 *
 * @param text Amenity name.
 * @param modifier [Modifier] applied to the chip.
 */
@Composable
fun AmenityChip(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(AppColors.Primary)
            .border(BorderStroke(1.dp, AppColors.Primary), CircleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(AppIcons.Tick, contentDescription = null, tint = AppColors.TextOnPrimary, modifier = Modifier.size(15.dp))
        Text(text = text, style = AppTextStyles.TabLabel, color = AppColors.TextOnPrimary)
    }
}

/**
 * All of a campsite's amenities as [AmenityChip]s, wrapping onto new lines when needed.
 *
 * @param amenities Amenity names. Nothing is drawn when the list is empty.
 * @param modifier [Modifier] applied to the group.
 */
@Composable
fun AmenityChips(amenities: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        amenities.forEach { AmenityChip(it) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun AmenityChipsPreview() {
    AmenityChips(listOf("Has bathroom", "Water", "Fire pit", "Pet friendly"), Modifier.padding(12.dp))
}
