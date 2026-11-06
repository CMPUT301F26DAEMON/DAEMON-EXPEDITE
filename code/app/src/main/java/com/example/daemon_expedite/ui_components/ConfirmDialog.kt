// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Add the owner's campsite details screen with edit and delete", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * "Are you sure?" dialog for destructive actions such as deleting or declining.
 *
 * Built from the "Decline Lottery · Dialog" frame in the Figma file: an icon in a red ring, a
 * title, a short message, a red confirm button and a dark button to back out.
 *
 * Show it while a flag is true, and clear the flag in both callbacks:
 * ```
 * if (askDelete) {
 *     ConfirmDialog(
 *         title = "Delete campsite?",
 *         message = "This can't be undone.",
 *         confirmText = "Yes, delete campsite",
 *         onConfirm = { askDelete = false; delete() },
 *         onDismiss = { askDelete = false },
 *     )
 * }
 * ```
 *
 * @param title Short question, for example "Decline this lottery?".
 * @param message What will happen if the user confirms.
 * @param confirmText Label of the red confirm button.
 * @param onConfirm Called when the confirm button is tapped.
 * @param onDismiss Called on the dismiss button, tapping outside, or back.
 * @param dismissText Label of the dark button that backs out.
 * @param icon Icon in the red ring at the top.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    icon: ImageVector = AppIcons.Dustbin,
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.SurfaceOverlay)
                .border(BorderStroke(1.dp, AppColors.OutlineStrong), shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AppColors.DangerTint)
                    .border(BorderStroke(1.dp, AppColors.Danger), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(28.dp))
            }
            Text(title, style = AppTextStyles.PageTitle, color = AppColors.TextOnPrimary, textAlign = TextAlign.Center)
            Text(message, style = AppTextStyles.Body, color = AppColors.TextSecondary, textAlign = TextAlign.Center)

            BoxWithConstraints(Modifier.padding(top = 8.dp)) {
                val width = maxWidth.value.toInt()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RedButton(onClick = onConfirm, icon = AppIcons.Cross, buttonText = confirmText, customWidth = width)
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AppColors.OutlineStrong),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AppColors.SurfaceDeep,
                            contentColor = AppColors.TextOnPrimary,
                        ),
                    ) {
                        Text(dismissText, style = AppTextStyles.ButtonLabel)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    ConfirmDialog(
        title = "Decline this lottery?",
        message = "This action can't be undone.",
        confirmText = "Yes, decline lottery",
        dismissText = "Keep my win",
        icon = AppIcons.CalendarCross,
        onConfirm = {},
        onDismiss = {},
    )
}
