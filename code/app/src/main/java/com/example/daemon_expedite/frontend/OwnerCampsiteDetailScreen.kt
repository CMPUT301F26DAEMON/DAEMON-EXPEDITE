// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Add the owner's campsite details screen with edit and delete", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppFonts
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsiteOverview
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.LotteryStatus
import com.example.daemon_expedite.ui_components.RedButton

/**
 * An owner's view of one of their own campsites. Opened by tapping a campsite on Owner Home.
 *
 * Built from the "Campsite Detail: owner view" frame in the Figma file, plus Edit and Delete
 * buttons. Edit and Show QR code actions are located on the right side of the top title bar.
 * Delete asks for confirmation first.
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
 * @param onReviewsClick Called when the rating line ("★ 4.9 · 24 reviews ›") is tapped.
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
    onReviewsClick: () -> Unit = {},
    onDeleteConfirmed: () -> Unit,
) {
    var askDelete by rememberSaveable { mutableStateOf(false) }

    DetailPageScaffold(
        title = "Campsite Details",
        state = state,
        onBackClick = onBackClick,
        onRetry = onRetry,
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShowQrCodeClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = AppIcons.QRCode,
                        contentDescription = "Show QR code",
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = AppIcons.EditPencilOutlined,
                        contentDescription = "Edit campsite",
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    ) { details ->
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
            onReviewsClick = onReviewsClick,
            pill = { LotteryPill(details.lotteryStatus) },
        )

        CalendarSection(title = "Booked Dates", ranges = details.bookedNights)

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt()
            val count = details.waitlistCount
            val subtitle = if (count != null) {
                when (count) {
                    0 -> "No entrants on waitlist yet"
                    1 -> "1 entrant on waitlist"
                    else -> "$count entrants on waitlist"
                }
            } else {
                "View waitlist entrants"
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BlackButton(
                    onClick = onViewWaitlistClick,
                    icon = AppIcons.Waitlist,
                    buttonText = "View Waitlist",
                    subtitleText = subtitle,
                    customWidth = width,
                )
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

@Composable
private fun BlackButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    buttonText: String,
    subtitleText: String? = null,
    customWidth: Int = 356,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(customWidth.dp)
            .height(if (subtitleText != null) 64.dp else 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Background,
            contentColor = AppColors.TextOnPrimary,
            disabledContainerColor = AppColors.SurfaceDisabled,
            disabledContentColor = AppColors.TextSecondary,
        ),
        border = BorderStroke(1.dp, AppColors.OutlineStrong),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = buttonText,
                    fontFamily = AppFonts.Inter,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextOnPrimary,
                    textAlign = TextAlign.Start,
                )
                if (subtitleText != null) {
                    Text(
                        text = subtitleText,
                        style = AppTextStyles.Caption,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Start,
                    )
                }
            }
            Icon(
                imageVector = AppIcons.ArrowForward,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(18.dp),
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
