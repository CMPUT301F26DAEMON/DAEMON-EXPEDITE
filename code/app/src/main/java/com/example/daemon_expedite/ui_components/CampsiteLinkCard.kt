package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An outlined row naming a campsite, with a map pin on the left and a chevron on the right.
 * Tapping it opens that campsite.
 *
 * Built from the "Campsite row" frame on the Lottery Details page in the Figma file.
 *
 * @param name The campsite's name. Long names are cut off with an ellipsis.
 * @param onClick Called when the card is tapped.
 * @param modifier [Modifier] applied to the card.
 */
@Composable
fun CampsiteLinkCard(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AppColors.OutlineButton, shape)
            .clickable(role = Role.Button, onClickLabel = "Open $name", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(AppIcons.MapPin, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(19.dp))
        Text(
            text = name,
            style = AppTextStyles.TitleSmall.copy(lineHeight = 20.sp),
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(16.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun CampsiteLinkCardPreview() {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CampsiteLinkCard(name = "Pine Creek Hideaway", onClick = {})
        CampsiteLinkCard(name = "Maple Campsite", onClick = {})
    }
}
