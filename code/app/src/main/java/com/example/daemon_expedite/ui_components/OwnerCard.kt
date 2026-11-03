package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Card showing who owns a campsite, with an optional button to message them.
 *
 * Built from the "Expedite/Card/Owner" component in the Figma file.
 *
 * @param name Owner's name.
 * @param modifier [Modifier] applied to the card.
 * @param onMessageClick Called when the message button is tapped. Null hides the button.
 */
@Composable
fun OwnerCard(
    name: String,
    modifier: Modifier = Modifier,
    onMessageClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.FillField)
            .border(BorderStroke(1.dp, AppColors.OutlineField), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Profile, contentDescription = null, tint = AppColors.TextOnPrimary, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("CAMPSITE OWNER", style = AppTextStyles.Overline, color = AppColors.Secondary)
            Text(name, style = AppTextStyles.ValueEmphasis, color = AppColors.TextPrimary)
        }
        if (onMessageClick != null) {
            IconButton(onClick = onMessageClick) {
                Icon(AppIcons.Chat, contentDescription = "Message $name", tint = AppColors.TextPrimary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun OwnerCardPreview() {
    OwnerCard(name = "Maya", onMessageClick = {}, modifier = Modifier.padding(12.dp))
}
