package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AmenityChips
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsiteImageCarousel
import com.example.daemon_expedite.ui_components.CampsitePill
import com.example.daemon_expedite.ui_components.DateRangeCalendar
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.LotteryStatus
import com.example.daemon_expedite.ui_components.LotteryStatusCard
import com.example.daemon_expedite.ui_components.OwnerCard
import com.example.daemon_expedite.ui_components.PillVariant
import com.example.daemon_expedite.ui_components.RatingLabel
import com.example.daemon_expedite.ui_components.ScreenHeader
import com.example.daemon_expedite.ui_components.StayFactsCard
import java.time.LocalDate

/**
 * Campsite details as seen by an entrant. Opened by tapping a campsite card on Entrant Home.
 *
 * Built from the "Campsite Detail Active" and "Campsite Detail Not Active" frames in the Figma
 * file. The lottery card changes with [CampsiteDetails.lotteryStatus].
 *
 * @param state The campsite to show, or loading / error.
 * @param onBackClick Called when the back button is tapped.
 * @param onRetry Called when the error message is tapped.
 * @param onViewMapClick Called when "View on map" is tapped.
 * @param onJoinLotteryClick Called when "Join Lottery!" is tapped.
 * @param onLeaveLotteryClick Called when "Leave waiting list" is tapped.
 * @param onMessageOwnerClick Called when the message button on the owner card is tapped.
 * @param onPoliciesClick Called when "Campsite Policies" is tapped.
 */
@Composable
fun CampsiteDetailScreen(
    state: UiState<CampsiteDetails>,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onViewMapClick: () -> Unit,
    onJoinLotteryClick: () -> Unit,
    onLeaveLotteryClick: () -> Unit,
    onMessageOwnerClick: () -> Unit,
    onPoliciesClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = "Campsite Details", onBackClick = onBackClick)
        when (state) {
            UiState.Loading -> Centered { CircularProgressIndicator(color = AppColors.Primary) }

            is UiState.Error -> Centered {
                Text(
                    text = "${state.message}. Tap to try again.",
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(onClick = onRetry).padding(12.dp),
                )
            }

            is UiState.Content -> DetailsContent(
                details = state.data,
                onViewMapClick = onViewMapClick,
                onJoinLotteryClick = onJoinLotteryClick,
                onLeaveLotteryClick = onLeaveLotteryClick,
                onMessageOwnerClick = onMessageOwnerClick,
                onPoliciesClick = onPoliciesClick,
            )
        }
    }
}

@Composable
private fun DetailsContent(
    details: CampsiteDetails,
    onViewMapClick: () -> Unit,
    onJoinLotteryClick: () -> Unit,
    onLeaveLotteryClick: () -> Unit,
    onMessageOwnerClick: () -> Unit,
    onPoliciesClick: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 412.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CampsiteImageCarousel(
                imageUrls = details.imageUrls,
                pill = { LotteryPill(details.lotteryStatus) },
            )

            // Name, rating and description sit closer together than the page's 24dp sections.
            Column {
                Text(details.name, style = AppTextStyles.PageTitleMultiline, color = AppColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                RatingLabel(average = details.averageRating, reviewCount = details.reviewCount)
                if (details.description.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(details.description, style = AppTextStyles.BodySmallMultiline, color = AppColors.TextPrimary)
                }
            }

            if (details.amenities.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading("Amenities")
                    AmenityChips(details.amenities)
                }
            }

            StayFactsCard(maxOccupancy = details.maxOccupancy, pricePerNight = details.pricePerNight)

            FullWidth { width ->
                GreenButton(onClick = onViewMapClick, icon = AppIcons.MapPin, buttonText = "View on map", customWidth = width)
            }

            LotteryStatusCard(
                status = details.lotteryStatus,
                endsText = details.lotteryEndsText,
                onJoinClick = onJoinLotteryClick,
                onLeaveClick = onLeaveLotteryClick,
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Available Dates")
                DateRangeCalendar(
                    startDate = details.bookingStart,
                    endDate = details.bookingEnd,
                    onRangeChange = null, // read-only
                )
            }

            OwnerCard(
                name = details.ownerName ?: "Campsite owner",
                onMessageClick = onMessageOwnerClick,
            )

            PoliciesLink(onPoliciesClick)
        }
    }
}

/** The pill on the photo: amber when a lottery is open or joined, muted otherwise. */
@Composable
private fun LotteryPill(status: LotteryStatus) {
    if (status == LotteryStatus.NONE) {
        CampsitePill("Lottery · None", variant = PillVariant.Muted, icon = AppIcons.Clock)
    } else {
        CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = AppIcons.LotteryTicket)
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
}

@Composable
private fun PoliciesLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(AppIcons.Info, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
        Text(
            text = "Campsite Policies",
            style = AppTextStyles.BodySmall.copy(textDecoration = TextDecoration.Underline),
            color = AppColors.TextSecondary,
        )
    }
}

/** Gives [content] the full available width in dp, for the shared buttons that take a width. */
@Composable
private fun FullWidth(content: @Composable (width: Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        content(maxWidth.value.toInt())
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Preview(widthDp = 412, heightDp = 1700)
@Composable
private fun CampsiteDetailScreenPreview() {
    CampsiteDetailScreen(
        state = UiState.Content(
            CampsiteDetails(
                id = Id(),
                name = "Pine Creek Hideaway",
                description = "This is the campsite description!",
                averageRating = 4.9,
                reviewCount = 48,
                policies = "",
                amenities = listOf("Has bathroom", "Water"),
                maxOccupancy = 6,
                pricePerNight = "$45",
                imageUrls = emptyList(),
                ownerName = "Maya",
                latitude = 0.0,
                longitude = 0.0,
                lotteryStatus = LotteryStatus.JOINED,
                lotteryEndsText = "12 Oct 2026 · 6:00 PM PT",
                bookingStart = LocalDate.of(2026, 10, 16),
                bookingEnd = LocalDate.of(2026, 10, 18),
            )
        ),
        onBackClick = {},
        onRetry = {},
        onViewMapClick = {},
        onJoinLotteryClick = {},
        onLeaveLotteryClick = {},
        onMessageOwnerClick = {},
        onPoliciesClick = {},
    )
}
