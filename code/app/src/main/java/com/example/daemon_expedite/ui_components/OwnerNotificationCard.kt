package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A popup where an owner writes a notification for everyone on a lottery's waiting list.
 *
 * @param onDismiss Called when the owner taps outside the popup or presses back. Ignored
 *   while [isSending], so the popup can't be closed halfway through a send.
 * @param onSend Called with the title (null if left blank) and the message text.
 * @param isSending True while the notification is being saved. Disables the button.
 * @param modifier Optional [Modifier] applied to the popup card.
 */
@Composable
fun OwnerNotificationPopup(
    onDismiss: () -> Unit,
    onSend: (title: String?, text: String) -> Unit,
    isSending: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        OwnerNotificationCard(
            onSend = onSend,
            isSending = isSending,
            modifier = modifier.fillMaxWidth(0.86f),
        )
    }
}

/**
 * The content of [OwnerNotificationPopup]: an optional title field, a required message field
 * and a "Send Notification" button. Separate from the popup so it can be previewed without a
 * Dialog window.
 *
 * The button stays disabled until the message has some text.
 *
 * @param onSend Called with the trimmed title (null if blank) and the trimmed message.
 * @param isSending True while sending. Disables the button and changes its text.
 * @param modifier Optional [Modifier] applied to the card.
 */
@Composable
fun OwnerNotificationCard(
    onSend: (title: String?, text: String) -> Unit,
    isSending: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    val canSend = text.isNotBlank() && !isSending

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.SurfaceDeep,
        border = BorderStroke(1.dp, AppColors.OutlineSubtle),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Notify waiting list",
                style = AppTextStyles.CardTitle,
                color = AppColors.TextOnPrimary,
            )
            Spacer(Modifier.height(14.dp))

            NotificationInput(
                value = title,
                onValueChange = { title = it },
                placeholder = "Title (Optional)",
                singleLine = true,
                minHeight = 44.dp,
                textStyle = AppTextStyles.Caption,
            )
            Spacer(Modifier.height(10.dp))

            NotificationInput(
                value = text,
                onValueChange = { text = it },
                placeholder = "Write your notification…",
                singleLine = false,
                minHeight = 96.dp,
                textStyle = AppTextStyles.CaptionMultiline,
            )
            Spacer(Modifier.height(14.dp))

            // Full card width, like the other popups' buttons, without a hard-coded size.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                GreenButton(
                    onClick = { if (canSend) onSend(title.trim().ifEmpty { null }, text.trim()) },
                    icon = AppIcons.BellIcon,
                    buttonText = if (isSending) "Sending…" else "Send Notification",
                    customHeight = 44,
                    customWidth = maxWidth.value.toInt(),
                    enabled = canSend,
                )
            }
        }
    }
}

/** A rounded text field in the popups' style, with a grey [placeholder] while empty. */
@Composable
private fun NotificationInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Dp,
    textStyle: TextStyle,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        textStyle = textStyle.copy(color = AppColors.TextOnPrimary),
        cursorBrush = SolidColor(AppColors.Primary),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
                shape = RoundedCornerShape(11.dp),
                color = AppColors.FillField,
                border = BorderStroke(1.dp, AppColors.OutlineSubtle),
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = textStyle, color = AppColors.TextSecondary)
                    }
                    innerTextField()
                }
            }
        },
    )
}

/** Preview of the empty popup content. */
@Preview(showBackground = true)
@Composable
private fun OwnerNotificationCardPreview() {
    Surface(modifier = Modifier.fillMaxSize(), color = AppColors.Background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OwnerNotificationCard(onSend = { _, _ -> }, modifier = Modifier.fillMaxWidth(0.86f))
        }
    }
}
