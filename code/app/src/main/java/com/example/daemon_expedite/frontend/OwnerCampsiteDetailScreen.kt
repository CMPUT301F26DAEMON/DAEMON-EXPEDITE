// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Add the owner's campsite details screen with edit and delete", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.CampsiteOverview
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.LotteryStatus
import com.example.daemon_expedite.ui_components.RedButton

/**
 * An owner's view of one of their own campsites. Opened by tapping a campsite on Owner Home.
 *
 * Built from the "Campsite Detail: owner view" frame in the Figma file, plus Edit and Delete
 * buttons (not in the design). Delete asks for confirmation first.
 *
 * @param state The campsite to show, or loading / error.
 * @param isDeleting True while the campsite is being deleted; the delete button is disabled.
 * @param onBackClick Called when the back button is tapped.
 * @param onRetry Called when the error message is tapped.
 * @param onViewMapClick Called when "View on map" is tapped.
 * @param onViewWaitlistClick Called when "View Waitlist" is tapped.
 * @param onEditClick Called when "Edit campsite" is tapped.
 * @param onShowQrCodeClick Called when "Show QR code" is tapped.
 * @param onDeleteConfirmed Called after the owner confirms the delete dialog.
 */
@Composable
fun OwnerCampsiteDetailScreen(
    state: UiState<CampsiteDetails>,
    isDeleting: Boolean,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onViewMapClick: () -> Unit,
    onViewWaitlistClick: () -> Unit,
    onEditClick: () -> Unit,
    onShowQrCodeClick: () -> Unit = {},
    onDeleteConfirmed: () -> Unit,
) {
    var askDelete by rememberSaveable { mutableStateOf(false) }

    DetailPageScaffold("Campsite Details", state = state, onBackClick = onBackClick, onRetry = onRetry) { details ->
        CampsiteOverview(
            name = details.name,
            imageUrls = details.imageUrls,
            averageRating = details.averageRating,
            reviewCount = details.reviewCount,
            description = details.description,
            amenities = details.amenities,
            maxOccupancy = details.maxOccupancy,
            pricePerNight = details.pricePerNight,
            onViewMapClick = onViewMapClick,
            pill = { LotteryPill(details.lotteryStatus) },
        )

        CalendarSection(title = "Booked Dates", ranges = details.bookedNights)

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GreenButton(onClick = onViewWaitlistClick, icon = AppIcons.Waitlist, buttonText = "View Waitlist", customWidth = width)
                GreenButton(onClick = onEditClick, icon = AppIcons.EditPencilOutlined, buttonText = "Edit campsite", customWidth = width)
                GreenButton(onClick = onShowQrCodeClick, icon = AppIcons.QRCode, buttonText = "Show QR code", customWidth = width)
                RedButton(
                    onClick = { if (!isDeleting) askDelete = true },
                    icon = AppIcons.Dustbin,
                    buttonText = if (isDeleting) "Deleting…" else "Delete campsite",
                    customWidth = width,
                )
            }
        }

        if (askDelete) {
            ConfirmDialog(
                title = "Delete campsite?",
                message = "${details.name} and its photos will be deleted. This can't be undone.",
                confirmText = "Yes, delete campsite",
                dismissText = "Keep it",
                onConfirm = {
                    askDelete = false
                    onDeleteConfirmed()
                },
                onDismiss = { askDelete = false },
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 1500)
@Composable
private fun OwnerCampsiteDetailScreenPreview() {
    OwnerCampsiteDetailScreen(
        state = UiState.Content(previewCampsiteDetails(LotteryStatus.OPEN)),
        isDeleting = false,
        onBackClick = {},
        onRetry = {},
        onViewMapClick = {},
        onViewWaitlistClick = {},
        onEditClick = {},
        onDeleteConfirmed = {},
    )
}
