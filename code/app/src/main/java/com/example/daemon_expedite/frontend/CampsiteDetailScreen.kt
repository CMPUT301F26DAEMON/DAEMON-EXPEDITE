package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsiteOverview
import com.example.daemon_expedite.ui_components.CampsitePill
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.DateRangeCalendar
import com.example.daemon_expedite.ui_components.LotteryStatus
import com.example.daemon_expedite.ui_components.LotteryStatusCard
import com.example.daemon_expedite.ui_components.OwnerCard
import com.example.daemon_expedite.ui_components.PillVariant
import com.example.daemon_expedite.ui_components.ScreenHeader
import java.time.LocalDate
import java.time.YearMonth

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
 * @param onLeaveLotteryClick Called after the entrant confirms leaving the waiting list.
 * @param onBookClick Called when a winner taps "Book your stay".
 * @param onMessageOwnerClick Called when the message button on the owner card is tapped.
 * @param onPoliciesClick Called when "Campsite Policies" is tapped.
 * @param isLotteryBusy True while joining or leaving; the lottery button ignores taps.
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
    isLotteryBusy: Boolean = false,
    onBookClick: () -> Unit = {},
) {
    var askLeave by rememberSaveable { mutableStateOf(false) }

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

        LotteryStatusCard(
            status = details.lotteryStatus,
            endsText = details.lotteryEndsText,
            opensText = details.lotteryOpensText,
            bookByText = details.lotteryBookByText,
            bookingText = details.lotteryBookingText,
            busy = isLotteryBusy,
            onBookClick = onBookClick,
            onJoinClick = onJoinLotteryClick,
            onLeaveClick = { askLeave = true },
        )

        CalendarSection(title = "Available Dates", ranges = details.availableNights)

        OwnerCard(
            name = details.ownerName ?: "Campsite owner",
            onMessageClick = onMessageOwnerClick,
        )

        PoliciesLink(onPoliciesClick)

        if (askLeave) {
            ConfirmDialog(
                title = "Leave the waiting list?",
                message = "You won't be in the draw for ${details.name}. " +
                    "You can join again while entries are open, if there's still room.",
                confirmText = "Yes, leave waiting list",
                dismissText = "Stay on the list",
                icon = AppIcons.LeaveList,
                onConfirm = {
                    askLeave = false
                    onLeaveLotteryClick()
                },
                onDismiss = { askLeave = false },
            )
        }
    }
}

// region Shared with the other details screens

/**
 * The frame shared by the details pages (campsite, lottery): a header titled [title], then a
 * spinner, an error with a retry link, or [content] in a scrolling column with 24 dp between
 * sections.
 */
@Composable
internal fun <T> DetailPageScaffold(
    title: String,
    state: UiState<T>,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = title, onBackClick = onBackClick)
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

            is UiState.Content -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 412.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    content(state.data)
                }
            }
        }
    }
}

/** The pill on the photo: amber when a lottery is running, outlined when one is coming, muted otherwise. */
@Composable
internal fun LotteryPill(status: LotteryStatus) {
    when (status) {
        LotteryStatus.NONE -> CampsitePill("Lottery · None", variant = PillVariant.Muted, icon = AppIcons.Clock)
        LotteryStatus.UPCOMING -> CampsitePill("Lottery · Soon", variant = PillVariant.Outlined, icon = AppIcons.Clock)
        else -> CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = AppIcons.LotteryTicket)
    }
}

/**
 * A section heading over a read-only [DateRangeCalendar] showing [ranges] in green. The calendar
 * opens on the month of the first range.
 */
@Composable
internal fun CalendarSection(title: String, ranges: List<ClosedRange<LocalDate>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
        DateRangeCalendar(
            startDate = null,
            endDate = null,
            onRangeChange = null,
            initialMonth = ranges.minOfOrNull { it.start }?.let(YearMonth::from) ?: YearMonth.now(),
            addedRanges = ranges,
            addedRangesColor = AppColors.Primary,
        )
    }
}

// endregion

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

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/** Sample data for the previews of both details screens. */
internal fun previewCampsiteDetails(status: LotteryStatus = LotteryStatus.JOINED) = CampsiteDetails(
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
    lotteryStatus = status,
    lotteryEndsText = "12 Oct 2026 · 6:00 PM PT",
    stayPeriods = listOf(DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18))),
)

@Preview(widthDp = 412, heightDp = 1700)
@Composable
private fun CampsiteDetailScreenPreview() {
    CampsiteDetailScreen(
        state = UiState.Content(previewCampsiteDetails()),
        onBackClick = {},
        onRetry = {},
        onViewMapClick = {},
        onJoinLotteryClick = {},
        onLeaveLotteryClick = {},
        onMessageOwnerClick = {},
        onPoliciesClick = {},
    )
}
