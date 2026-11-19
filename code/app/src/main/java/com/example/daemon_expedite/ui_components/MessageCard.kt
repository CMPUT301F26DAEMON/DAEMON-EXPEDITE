
package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable message bubble.
 * Incoming messages are dark grey.
 * Outgoing messages are green.
 */
@Composable
fun MessageCard(
    message: String,
    time: String,
    isSentByMe: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isSentByMe) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 248.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (isSentByMe) {
                        AppColors.Primary
                    } else {
                        AppColors.FillField
                    }
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                )
        ) {
            Text(
                text = message,
                color = AppColors.TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = time,
            color = AppColors.TextSecondary,
            fontSize = 10.sp
        )
    }
}
