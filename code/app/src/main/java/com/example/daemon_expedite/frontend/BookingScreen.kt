// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the lottery booking and decline flow from our rules", 2026-11-05.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.BookingRules
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsiteCard
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.DateRangeCalendar
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.OwnerCard
import com.example.daemon_expedite.ui_components.RedButton
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * "Make a Reservation": where a lottery winner picks a campsite and nights and books them, or
 * declines the win. Opened from "Book your stay" on the campsite page.
 *
 * Built from the "Won Lottery, tick box" and "Won Lottery, can reserve" frames in the Figma file,
 * with the "Decline Lottery · Dialog" frame for declining.
 *
 * Only nights that can still be booked on the picked campsite can be tapped (see
 * [BookingRules.isSelectable]). "Reserve Campsite" stays disabled until a valid range is picked
 * and the rules box is ticked.
 *
 * @param state The lottery and its campsites, or loading / error.
 * @param selection What the winner has picked so far.
 * @param problem What's wrong with the picked nights, or null.
 * @param isBooking True while booking: the reserve button says "Reserving…".
 * @param isDeclining True while declining: the decline button ignores taps.
 * @param onDeclineConfirmed Called after the winner confirms the decline dialog.
 * @param onPoliciesClick Called with the picked campsite when the rules link is tapped.
 */
@Composable
fun BookingScreen(
    state: UiState<BookingOptions>,
    selection: BookingSelection,
    problem: String?,
    isBooking: Boolean,
    isDeclining: Boolean,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onCampsiteClick: (Id) -> Unit,
    onDatesChange: (firstNight: LocalDate?, checkOut: LocalDate?) -> Unit,
    onAgreeChange: (Boolean) -> Unit,
    onPoliciesClick: (Id) -> Unit,
    onReserveClick: () -> Unit,
    onDeclineConfirmed: () -> Unit,
) {
    var askDecline by rememberSaveable { mutableStateOf(false) }

    DetailPageScaffold("Make a Reservation", state = state, onBackClick = onBackClick, onRetry = onRetry) { options ->
        val lottery = options.lottery
        val campsite = selection.campsite
        val booked = remember(lottery, campsite) {
            campsite?.let { BookingRules.bookedNights(lottery, it) }.orEmpty()
        }

        // region Campsite
        Text("Choose your campsite", style = AppTextStyles.PageTitleMultiline, color = AppColors.TextPrimary)
        options.campsites.forEach { option ->
            val picked = option.id == campsite
            CampsiteCard(
                name = option.name,
                location = option.location,
                pricePerNight = option.pricePerNight,
                imageUrl = option.imageUrl,
                rating = option.rating,
                containerColor = if (picked) AppColors.Primary else AppColors.Surface,
                pill = {
                    Icon(
                        imageVector = if (picked) AppIcons.CheckboxChecked else AppIcons.CheckboxEmpty,
                        contentDescription = if (picked) "Picked" else "Not picked",
                        tint = AppColors.TextOnPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = { onCampsiteClick(option.id) },
            )
        }
        // endregion

        // region Dates
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Select Dates", style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
            Text(
                "Tap your first night, then your check-out day. Greyed-out days are taken or outside the lottery's dates.",
                style = AppTextStyles.CaptionMultiline,
                color = AppColors.TextSecondary,
            )
            if (campsite != null) {
                DateRangeCalendar(
                    startDate = selection.firstNight,
                    endDate = selection.checkOut,
                    onRangeChange = onDatesChange,
                    initialMonth = YearMonth.from(lottery.firstStayNight),
                    isDateEnabled = { BookingRules.isSelectable(lottery, campsite, it, booked) },
                )
            }
            val period = selection.period
            when {
                problem != null -> Text(problem, style = AppTextStyles.BodySmallMultiline, color = AppColors.Danger)
                period != null -> Text(
                    "${period.start.format(SHORT)} → ${period.end.format(SHORT)} · " +
                        if (period.nights == 1) "1 night" else "${period.nights} nights",
                    style = AppTextStyles.ValueMedium,
                    color = AppColors.TextPrimary,
                )
            }
        }
        // endregion

        options.ownerName?.let { OwnerCard(name = it) }

        // Policy consent (user story 02.11.01).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (selection.agreedToRules) AppIcons.CheckboxChecked else AppIcons.CheckboxEmpty,
                contentDescription = if (selection.agreedToRules) "Agreed to the rules" else "Not agreed to the rules",
                tint = AppColors.TextPrimary,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(role = Role.Checkbox) { onAgreeChange(!selection.agreedToRules) },
            )
            Text(
                text = buildAnnotatedString {
                    append("I have read the ")
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append("rules and cancellation policy")
                    }
                },
                style = AppTextStyles.BodySmallMultiline,
                color = AppColors.TextPrimary,
                modifier = Modifier.clickable { campsite?.let(onPoliciesClick) },
            )
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GreenButton(
                    onClick = { if (!isBooking) onReserveClick() },
                    buttonText = if (isBooking) "Reserving…" else "Reserve Campsite",
                    customHeight = 62,
                    customWidth = width,
                    enabled = selection.period != null && problem == null && selection.agreedToRules && !isBooking,
                )
                RedButton(
                    onClick = { if (!isDeclining) askDecline = true },
                    icon = AppIcons.CancelCircle,
                    buttonText = if (isDeclining) "Declining…" else "Decline",
                    customWidth = width,
                )
            }
        }

        if (askDecline) {
            ConfirmDialog(
                title = "Decline this lottery?",
                message = "Your win will be released and you won't be able to book. This action can't be undone.",
                confirmText = "Yes, decline lottery",
                dismissText = "Keep my win",
                icon = AppIcons.CalendarCross,
                onConfirm = {
                    askDecline = false
                    onDeclineConfirmed()
                },
                onDismiss = { askDecline = false },
            )
        }
    }
}

private val SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
