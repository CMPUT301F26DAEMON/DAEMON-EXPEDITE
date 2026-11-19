package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

//Reusable message input bar for messaging

@Composable
fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    // Prevent empty messages from being sent.
    val sendMessage = {
        val text = message.trim()
        if (text.isNotEmpty()) {
            onSendClick(text)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .background(AppColors.FillField, shape)
            .border(
                1.dp,
                AppColors.OutlineStrong,
                shape
            )
            .padding(
                start = 14.dp,
                end = 4.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Message text field.
        BasicTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            textStyle = AppTextStyles.Body.copy(
                color = AppColors.TextPrimary
            ),
            cursorBrush = SolidColor(AppColors.Secondary),
            minLines = 1,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { sendMessage() }
            ),
            decorationBox = { innerTextField ->

                Box(
                    contentAlignment = Alignment.CenterStart
                ) {

                    // Placeholder displayed when no message is entered.
                    if (message.isEmpty()) {
                        Text(
                            text = "Message...",
                            style = AppTextStyles.Body,
                            color = AppColors.TextMuted
                        )
                    }

                    innerTextField()
                }
            }
        )
        // Circular send button.
        IconButton(
            onClick = sendMessage,
            modifier = Modifier
                .size(50.dp)
                .background(
                    AppColors.Primary,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Send message",
                tint = AppColors.TextOnPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Preview showing only the message input bar.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514
)
@Composable
fun MessageInputBarPreview() {
    var message by remember { mutableStateOf("")}
    MessageInputBar(
        message = message,
        onMessageChange = { message = it },
        onSendClick = { message =""},
        modifier = Modifier.padding(8.dp)
    )
}
