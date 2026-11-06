
package com.example.daemon_expedite.ui_components

import android.R.attr.name
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
import androidx.compose.ui.unit.sp

/**
 * Card showing an entrant, with a button to message them
 **
 * @param name Entrant's name.
 * @param modifier [Modifier] applied to the card.
 * @param onMessageClick Called when the message button is tapped. Null hides the button.
 * @param imageUrl Entrant profile picture or default if null
 */
@Composable
fun EntrantCard(
    name: String,
    modifier: Modifier = Modifier,
    onMessageClick: (() -> Unit)? = null,
    imageUrl: String? = null
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.FillSubtle)
            .border(BorderStroke(1.dp, AppColors.OutlineSubtle), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        ProfileImage(imageUrl, size = 40.dp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(name, style = AppTextStyles.ValueEmphasis, color = AppColors.TextPrimary, fontSize = 18.sp)
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
private fun EntrantCardPreview() {
    EntrantCard(name = "Keya Malhotra", onMessageClick = {}, modifier = Modifier.padding(12.dp))
}
