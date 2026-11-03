package com.example.daemon_expedite.ui_components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * "Are you sure?" dialog in the app's colours, for destructive actions such as deleting.
 *
 * Show it while a flag is true, and clear the flag in both callbacks:
 * ```
 * if (askDelete) {
 *     ConfirmDialog(
 *         title = "Delete campsite?",
 *         message = "This can't be undone.",
 *         confirmText = "Delete",
 *         onConfirm = { askDelete = false; delete() },
 *         onDismiss = { askDelete = false },
 *     )
 * }
 * ```
 *
 * @param title Short question, for example "Delete campsite?".
 * @param message What will happen if the user confirms.
 * @param confirmText Label of the confirm button, shown in the danger colour.
 * @param onConfirm Called when the confirm button is tapped.
 * @param onDismiss Called on Cancel, tapping outside, or back.
 * @param dismissText Label of the cancel button.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = AppColors.Danger) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText, color = AppColors.TextPrimary) }
        },
        containerColor = AppColors.Surface,
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextSecondary,
    )
}

@Preview
@Composable
private fun ConfirmDialogPreview() {
    ConfirmDialog(
        title = "Delete campsite?",
        message = "Pine Creek Hideaway and its photos will be deleted. This can't be undone.",
        confirmText = "Delete",
        onConfirm = {},
        onDismiss = {},
    )
}
