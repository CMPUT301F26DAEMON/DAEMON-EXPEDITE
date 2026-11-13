// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsitePill
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.DateRangeCalendar
import com.example.daemon_expedite.ui_components.LotteryStatus
import com.example.daemon_expedite.ui_components.LotteryStatusCard
import com.example.daemon_expedite.ui_components.PillVariant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * ViewModel for the entrant's lottery details screen.
 * Handles loading lottery details, joining, and leaving the waiting list.
 */
class EntrantLotteryDetailViewModel(
    savedState: SavedStateHandle,
) : ViewModel() {
    private val lotteryId: Id? = savedState.get<String>(Routes.LOTTERY_ID_ARG).toIdOrNull()
    private val _details = MutableStateFlow<UiState<LotteryDetails>>(UiState.Loading)
    val details: StateFlow<UiState<LotteryDetails>> = _details.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun load() {
        val id =
            lotteryId ?: run {
                _details.value = UiState.Error("Couldn't find this lottery")
                return
            }
        viewModelScope.launch {
            if (_details.value !is UiState.Content) _details.value = UiState.Loading
            val fresh =
                loadAsUiState("Couldn't load this lottery") {
                    coroutineScope {
                        val user = quietly("Couldn't load user") { Database.fetchOwnUser() }
                        val lottery = Database.fetchLottery(id)
                        val names =
                            lottery.campsites
                                .map { campsiteId ->
                                    async {
                                        campsiteId to
                                            quietly("Couldn't load campsite $campsiteId") { Database.fetchCampsite(campsiteId).name }
                                    }
                                }.awaitAll()
                                .mapNotNull { (campsiteId, name) -> name?.let { campsiteId to it } }
                                .toMap()
                        lottery.toLotteryDetails(campsiteNames = names, viewerId = user?.id)
                    }
                }
            if (fresh is UiState.Content || _details.value !is UiState.Content) _details.value = fresh
        }
    }

    fun joinLottery(onDone: () -> Unit = {}) {
        val id = lotteryId ?: return
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            runCatching {
                val userId = Database.fetchOwnUser().id
                Database.addUserToLottery(id, userId)
            }
            _isBusy.value = false
            load()
            onDone()
        }
    }

    fun leaveLottery(onDone: () -> Unit = {}) {
        val id = lotteryId ?: return
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            runCatching {
                val userId = Database.fetchOwnUser().id
                Database.removeUserFromLottery(id, userId)
            }
            _isBusy.value = false
            load()
            onDone()
        }
    }
}

/**
 * The entrant's view of one lottery details page.
 *
 * Displays lottery status, campsites, entry period, stay periods, and the standard [LotteryStatusCard].
 *
 * @param state The lottery to show, or loading / error.
 * @param isBusy True while joining/leaving; the button shows "…" and ignores taps.
 * @param onBackClick Called when the back button is clicked.
 * @param onRetry Called when the user taps an error message to retry.
 * @param onCampsiteClick Called with the id of a tapped campsite.
 * @param onJoinClick Called when "Join Lottery!" is tapped.
 * @param onLeaveClick Called when "Leave waiting list" is tapped.
 * @param onBookClick Called when "Book your stay" is tapped.
 * @param onViewReservationClick Called when the user views reservation details.
 */
@Composable
fun EntrantLotteryDetailScreen(
    state: UiState<LotteryDetails>,
    isBusy: Boolean = false,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onCampsiteClick: (Id) -> Unit,
    onJoinClick: () -> Unit = {},
    onLeaveClick: () -> Unit = {},
    onBookClick: () -> Unit = {},
    onViewReservationClick: (() -> Unit)? = null,
) {
    var askLeave by rememberSaveable { mutableStateOf(false) }

    DetailPageScaffold("Lottery Details", state = state, onBackClick = onBackClick, onRetry = onRetry) { details ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = details.title,
                style = AppTextStyles.PageTitleMultiline,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            CampsitePill(details.status, variant = details.statusVariant)
        }

        Section("Campsites") {
            details.campsites.forEach { campsite ->
                CampsiteRow(campsite.name, onClick = { onCampsiteClick(campsite.id) })
            }
        }

        Section("Lottery Period") {
            val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }
            val firstStayDate = details.stayPeriods.minOfOrNull { it.start }
            val formattedFirstStayDate = firstStayDate?.format(dateFormatter) ?: ""

            val endDateText =
                details.entryText
                    .substringAfter("Closes end of ", "")
                    .ifEmpty { details.entryText.substringAfter("Closes ", "") }
                    .ifEmpty { formattedFirstStayDate }

            val periodSubtitle =
                when {
                    details.lotteryStatus == LotteryStatus.BOOKED -> {
                        "Your booking is confirmed. You can cancel it from Reservations."
                    }

                    details.lotteryStatus == LotteryStatus.WON -> {
                        "Book a campsite and dates before your first stay night. Spots not booked by $formattedFirstStayDate are declined."
                    }

                    details.lotteryStatus == LotteryStatus.LOST -> {
                        "The draw is done for this lottery."
                    }

                    else -> {
                        "You can leave while entries are open. Winners are drawn at the end of $endDateText"
                    }
                }

            Text(details.entryText, style = AppTextStyles.ValueMedium, color = AppColors.TextPrimary)
            Text(
                text = periodSubtitle,
                style = AppTextStyles.BodySmallMultiline,
                color = AppColors.TextSecondary,
            )
        }

        Section("Stay Periods") {
            DateRangeCalendar(
                startDate = null,
                endDate = null,
                onRangeChange = null, // read-only
                initialMonth =
                    details.stayPeriods
                        .firstOrNull()
                        ?.start
                        ?.let(YearMonth::from) ?: YearMonth.now(),
                addedRanges = details.stayPeriods.map { it.start..it.end },
                addedRangesColor = AppColors.Primary,
            )
            details.stayTexts.forEach {
                Text(it, style = AppTextStyles.BodySmall, color = AppColors.TextPrimary)
            }
        }

        Section("Draw") {
            Text(details.drawText, style = AppTextStyles.ValueMedium, color = AppColors.TextPrimary)

            Spacer(modifier = Modifier.height(4.dp))
            if (details.lotteryStatus != LotteryStatus.NONE) {
                LotteryStatusCard(
                    status = details.lotteryStatus,
                    endsText = details.endsText,
                    opensText = details.opensText,
                    bookByText = details.bookByText,
                    bookingText = details.bookingText,
                    busy = isBusy,
                    onJoinClick = onJoinClick,
                    onLeaveClick = { askLeave = true },
                    onBookClick = onBookClick,
                )
            }
        }
    }

    if (askLeave) {
        ConfirmDialog(
            title = "Leave the waiting list?",
            message =
                "You won't be in the draw for this lottery. " +
                    "You can join again while entries are open if there's still room.",
            confirmText = "Yes, leave waiting list",
            dismissText = "Stay on list",
            icon = AppIcons.LeaveList,
            onConfirm = {
                askLeave = false
                onLeaveClick()
            },
            onDismiss = { askLeave = false },
        )
    }
}


/** A heading with its content underneath, 12 dp apart. */
@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
        content()
    }
}

/** A campsite in the lottery; tapping it opens the campsite's page. */
@Composable
private fun CampsiteRow(
    name: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.SurfaceDeep)
                .border(1.dp, AppColors.OutlineSubtle, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.Campsite, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(20.dp))
        Text(
            text = name,
            style = AppTextStyles.ValueEmphasis,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(AppIcons.ArrowForward, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

@Preview(widthDp = 412, heightDp = 1500)
@Composable
private fun EntrantLotteryDetailScreenPreview() {
    val stays =
        listOf(
            DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18)),
            DatePeriod(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 25)),
        )
    EntrantLotteryDetailScreen(
        state =
            UiState.Content(
                LotteryDetails(
                    id = Id(),
                    title = "Pine Creek Hideaway + 1 more",
                    status = "Booked",
                    statusVariant = PillVariant.Highlight,
                    campsites = listOf(CampsiteOption(Id(), "Pine Creek Hideaway"), CampsiteOption(Id(), "Maple Campsite")),
                    entryText = "Opens 1 Oct · Closes end of 5 Oct",
                    phaseNote = "Entrants can join now. Winners are drawn at the end of 5 Oct.",
                    stayPeriods = stays,
                    stayTexts = listOf("16 Oct → 18 Oct · 2 nights", "23 Oct → 25 Oct · 2 nights"),
                    drawText = "12/20 joined · 4 winners to draw",
                    canEdit = true,
                    canCancel = true,
                    deletesOnCancel = false,
                    lotteryStatus = LotteryStatus.BOOKED,
                    bookingText = "16 Oct - 18 Oct"
                ),
            ),
        isBusy = false,
        onBackClick = {},
        onRetry = {},
        onCampsiteClick = {},
    )
}
