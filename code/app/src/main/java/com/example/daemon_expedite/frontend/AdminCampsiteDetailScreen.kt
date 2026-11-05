package com.example.daemon_expedite.frontend

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.CampsiteOverview
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.LotteryStatus
import com.example.daemon_expedite.ui_components.OwnerCard
import com.example.daemon_expedite.ui_components.RedButton

/**
 * Campsite details as seen by an admin. Opened by tapping any campsite on Admin Home,
 * including the admin's own.
 *
 * Same layout as [CampsiteDetailScreen] without the lottery card (admins don't enter lotteries
 * from here). The owner card has a red delete button instead of the message button, and a red
 * "Delete Campsite" button sits at the bottom. Both deletes ask for confirmation first.
 *
 * @param state The campsite to show, or loading / error.
 * @param isDeleting True while the campsite or its owner is being deleted; both delete buttons ignore taps.
 * @param onBackClick Called when the back button is tapped.
 * @param onRetry Called when the error message is tapped.
 * @param onViewMapClick Called when "View on map" is tapped.
 * @param onPoliciesClick Called when "Campsite Policies" is tapped.
 * @param onDeleteCampsiteConfirmed Called after the admin confirms deleting the campsite.
 * @param onDeleteOwnerConfirmed Called after the admin confirms deleting the campsite's owner.
 */
@Composable
fun AdminCampsiteDetailScreen(
    state: UiState<CampsiteDetails>,
    isDeleting: Boolean,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onViewMapClick: () -> Unit,
    onPoliciesClick: () -> Unit,
    onDeleteCampsiteConfirmed: () -> Unit,
    onDeleteOwnerConfirmed: () -> Unit,
) {
    var askDeleteCampsite by rememberSaveable { mutableStateOf(false) }
    var askDeleteOwner by rememberSaveable { mutableStateOf(false) }

    DetailPageScaffold("Campsite Details", state = state, onBackClick = onBackClick, onRetry = onRetry) { details ->
        val ownerName = details.ownerName ?: "Campsite owner"

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

        CalendarSection(title = "Available Dates", ranges = details.availableNights)

        OwnerCard(
            name = ownerName,
            onDeleteClick = { if (!isDeleting) askDeleteOwner = true },
        )

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            RedButton(
                onClick = { if (!isDeleting) askDeleteCampsite = true },
                icon = AppIcons.Dustbin,
                buttonText = if (isDeleting) "Deleting…" else "Delete Campsite",
                customWidth = maxWidth.value.toInt(),
            )
        }

        PoliciesLink(onPoliciesClick)


        if (askDeleteCampsite) {
            ConfirmDialog(
                title = "Delete campsite?",
                message = "${details.name} and its photos will be deleted for everyone. This can't be undone.",
                confirmText = "Yes, delete campsite",
                dismissText = "Keep it",
                onConfirm = {
                    askDeleteCampsite = false
                    onDeleteCampsiteConfirmed()
                },
                onDismiss = { askDeleteCampsite = false },
            )
        }

        if (askDeleteOwner) {
            ConfirmDialog(
                title = "Delete this owner?",
                message = "$ownerName's owner profile will be deleted, along with all of their campsites " +
                    "and lotteries. Their entrant account stays. This can't be undone.",
                confirmText = "Yes, delete owner",
                dismissText = "Keep owner",
                onConfirm = {
                    askDeleteOwner = false
                    onDeleteOwnerConfirmed()
                },
                onDismiss = { askDeleteOwner = false },
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 1500)
@Composable
private fun AdminCampsiteDetailScreenPreview() {
    AdminCampsiteDetailScreen(
        state = UiState.Content(previewCampsiteDetails(LotteryStatus.OPEN)),
        isDeleting = false,
        onBackClick = {},
        onRetry = {},
        onViewMapClick = {},
        onPoliciesClick = {},
        onDeleteCampsiteConfirmed = {},
        onDeleteOwnerConfirmed = {},
    )
}