// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.LotteryProblems
import com.example.daemon_expedite.backend.LotteryRules
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.DatePickerField
import com.example.daemon_expedite.ui_components.DateRangesPicker
import com.example.daemon_expedite.ui_components.FormSelectField
import com.example.daemon_expedite.ui_components.FormTextField
import com.example.daemon_expedite.ui_components.MultiSelectField
import com.example.daemon_expedite.ui_components.ScreenHeader
import com.example.daemon_expedite.ui_components.YellowButton
import java.time.LocalDate

/**
 * The owner's Create lottery form, from the Figma frame "Start lottery · Empty". Also the
 * Edit lottery form when [isEditing] is true; [locks] then greys out what can't change.
 *
 * Changes from the frame, as agreed with the team: "Lottery Period" is when entrants can join,
 * the single calendar became "Stay Periods" (several ranges of bookable nights), and the
 * QR code button is left for later.
 *
 * Only draws [draft] and reports changes; [CreateLotteryViewModel] holds the values and saves.
 *
 * @param draft Current form values.
 * @param campsiteOptions The owner's campsites that aren't in another lottery, or loading / error.
 * @param collaboratorOptions Other owners who can be invited, or loading / error. Campsites of
 *   the ones picked are added to the campsites dropdown, under a heading per owner.
 * @param problems Messages to show under the fields (only filled in after the first submit).
 * @param isSaving True while saving: the create button says "Creating…" and ignores taps.
 * @param today The owner's today; entries can't open before it.
 * @param isEditing True when editing a saved lottery: the title and save button change.
 * @param locks What can't be changed (only set when editing a lottery whose entries are open).
 */
@Composable
fun CreateLotteryScreen(
    draft: LotteryDraft,
    campsiteOptions: UiState<List<CampsiteOption>>,
    collaboratorOptions: UiState<List<CollaboratorOption>>,
    problems: LotteryProblems,
    isSaving: Boolean,
    today: LocalDate,
    onBackClick: () -> Unit,
    isEditing: Boolean = false,
    locks: LotteryEditLocks = LotteryEditLocks(),
    onRetryCampsites: () -> Unit,
    onRetryCollaborators: () -> Unit,
    onToggleCampsite: (Id) -> Unit,
    onToggleCollaborator: (Id) -> Unit,
    onToggleInvitedCampsite: (Id) -> Unit,
    onEntryFirstDayChange: (LocalDate) -> Unit,
    onEntryLastDayChange: (LocalDate) -> Unit,
    onAddStayPeriod: (firstNight: LocalDate, checkOut: LocalDate) -> Unit,
    onRemoveStayPeriod: (index: Int) -> Unit,
    onWinnerCountChange: (String) -> Unit,
    onWaitlistCapacityChange: (String) -> Unit,
    onCreateClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ScreenHeader(title = if (isEditing) "Edit Lottery" else "Create Lottery", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Lottery Details", style = AppTextStyles.PageTitleMultiline, color = AppColors.TextPrimary)
            if (locks.anyLocked) {
                Caption(
                    "Entries are open, so you can add to this lottery but not take anything away: " +
                        "greyed-out items are fixed, and numbers can only go up."
                )
            }

            // region Collaborators
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Collaborators")
                Caption("Invite other owners to include their campsites. Their campsites are added only after approval.")
                when (collaboratorOptions) {
                    UiState.Loading -> FormSelectField(
                        label = "Loading other owners…", value = null, onClick = {},
                        icon = AppIcons.Guests, enabled = false,
                    )
                    is UiState.Error -> FormSelectField(
                        label = "Select collaborators", value = "${collaboratorOptions.message}. Tap to try again.",
                        onClick = onRetryCollaborators, icon = AppIcons.Guests,
                    )
                    is UiState.Content -> MultiSelectField(
                        label = "Select collaborators",
                        options = collaboratorOptions.data,
                        selected = collaboratorOptions.data.filter { it.id in draft.collaborators }.toSet(),
                        optionLabel = { it.name },
                        onToggle = { onToggleCollaborator(it.id) },
                        icon = AppIcons.Guests,
                        emptyText = "No other owners have a free campsite right now.",
                        isOptionEnabled = { it.id !in locks.collaborators },
                    )
                }
            }
            // endregion

            // region Campsites
            SectionHeading("Select Campsites")
            when (campsiteOptions) {
                UiState.Loading -> FormSelectField(
                    label = "Loading your campsites…", value = null, onClick = {},
                    icon = AppIcons.Campsite, enabled = false,
                )
                is UiState.Error -> FormSelectField(
                    label = "Select campsites", value = "${campsiteOptions.message}. Tap to try again.",
                    onClick = onRetryCampsites, icon = AppIcons.Campsite,
                )
                is UiState.Content -> {
                    // The owner's own campsites, then those of each collaborator picked.
                    val picked = (collaboratorOptions as? UiState.Content)?.data.orEmpty().filter { it.id in draft.collaborators }
                    val options = campsiteOptions.data.map { PickableCampsite(it, ownerName = null) } +
                        picked.flatMap { owner -> owner.campsites.map { PickableCampsite(it, owner.name) } }
                    MultiSelectField(
                        label = "Select campsites",
                        options = options,
                        selected = options.filter {
                            if (it.ownerName == null) it.campsite.id in draft.campsites else it.campsite.id in draft.invitedCampsites
                        }.toSet(),
                        optionLabel = { it.campsite.name },
                        onToggle = {
                            if (it.ownerName == null) onToggleCampsite(it.campsite.id) else onToggleInvitedCampsite(it.campsite.id)
                        },
                        icon = AppIcons.Campsite,
                        emptyText = "No free campsites. Each campsite can only be in one lottery at a time.",
                        error = problems.campsites,
                        isOptionEnabled = {
                            if (it.ownerName == null) it.campsite.id !in locks.campsites else it.campsite.id !in locks.invitedCampsites
                        },
                        // Headings only help once there is more than one owner to tell apart.
                        sectionOf = if (picked.isEmpty()) null else { option -> option.ownerName ?: "Your campsites" },
                    )
                }
            }
            // endregion

            // region Lottery period
            SectionHeading("Lottery Period")
            Caption("When entrants can join the waiting list. Winners are drawn at the end of the close date.")
            DatePickerField(
                label = "Open date",
                date = draft.entryFirstDay,
                onDateSelected = onEntryFirstDayChange,
                icon = AppIcons.Calendar,
                minDate = today,
                enabled = !locks.openDateLocked,
            )
            DatePickerField(
                label = "Close date",
                date = draft.entryLastDay,
                onDateSelected = onEntryLastDayChange,
                icon = AppIcons.Clock,
                // Not before the open date or today, and once entries are open, only later.
                minDate = listOfNotNull(draft.entryFirstDay, today, locks.earliestClose).max(),
                error = problems.entryPeriod,
            )
            // endregion

            // region Stay periods
            SectionHeading("Stay Periods")
            Caption(
                "Nights winners can book, first come first served, until the first night. " +
                    "Add as many ranges as you like: tap the first night, then the check-out day."
            )
            DateRangesPicker(
                ranges = draft.stayPeriods.map { it.start..it.end },
                onAddRange = onAddStayPeriod,
                onRemoveRange = onRemoveStayPeriod,
                // Stays start a full booking day after entries close, so winners have time to book.
                minDate = (draft.entryLastDay ?: today).plusDays(LotteryRules.MIN_BOOKING_DAYS + 1L),
                error = problems.stayPeriods,
                isRemovable = { draft.stayPeriods.getOrNull(it) !in locks.stayPeriods },
            )
            // endregion

            // region Draw settings
            SectionHeading("Draw settings")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormTextField(
                    label = "Number of winners",
                    value = draft.winnerCount,
                    onValueChange = onWinnerCountChange,
                    icon = AppIcons.Trophy,
                    error = problems.winnerCount,
                    keyboardType = KeyboardType.NumberPassword,
                )
                if (problems.winnerCount == null && draft.maxWinners > 0) {
                    val atLeast = locks.minWinners?.let { "At least $it, up" } ?: "Up"
                    Caption("$atLeast to ${draft.maxWinners}: ${draft.campsites.size} campsite(s) × ${draft.stayPeriods.sumOf { it.nights }} night(s).")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormTextField(
                    label = "Waitlist capacity (optional)",
                    value = draft.waitlistCapacity,
                    onValueChange = onWaitlistCapacityChange,
                    icon = AppIcons.Waitlist,
                    error = problems.waitlistCapacity,
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                )
                if (problems.waitlistCapacity == null) {
                    Caption(locks.minCapacity?.let { "At least $it, or leave empty for no limit." } ?: "Leave empty for no limit.")
                }
            }
            // endregion

            BoxWithConstraints {
                YellowButton(
                    onClick = { if (!isSaving) onCreateClick() },
                    icon = AppIcons.Tick,
                    buttonText = when {
                        isEditing && isSaving -> "Saving…"
                        isEditing -> "Save changes"
                        isSaving -> "Creating…"
                        else -> "Create Lottery"
                    },
                    customHeight = 58,
                    customWidth = maxWidth.value.toInt(),
                )
            }
        }
    }
}

/** One row of the campsites dropdown. [ownerName] is null for the owner's own campsites. */
private data class PickableCampsite(val campsite: CampsiteOption, val ownerName: String?)

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
}

@Composable
private fun Caption(text: String) {
    Text(text, style = AppTextStyles.CaptionMultiline, color = AppColors.TextMuted)
}

@Preview(widthDp = 412, heightDp = 1900)
@Composable
private fun CreateLotteryScreenPreview() {
    val pine = CampsiteOption(Id(), "Pine Creek Hideaway")
    CreateLotteryScreen(
        draft = LotteryDraft(
            campsites = setOf(pine.id),
            entryFirstDay = LocalDate.of(2026, 10, 1),
            entryLastDay = LocalDate.of(2026, 10, 5),
            stayPeriods = listOf(DatePeriod(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18))),
            winnerCount = "2",
        ),
        campsiteOptions = UiState.Content(listOf(pine, CampsiteOption(Id(), "Maple Campsite"))),
        collaboratorOptions = UiState.Content(listOf(CollaboratorOption(Id(), "Alex Chen", listOf(CampsiteOption(Id(), "Birch Hollow"))))),
        problems = LotteryProblems(),
        isSaving = false,
        today = LocalDate.of(2026, 9, 20),
        onBackClick = {},
        onRetryCampsites = {},
        onRetryCollaborators = {},
        onToggleCampsite = {},
        onToggleCollaborator = {},
        onToggleInvitedCampsite = {},
        onEntryFirstDayChange = {},
        onEntryLastDayChange = {},
        onAddStayPeriod = { _, _ -> },
        onRemoveStayPeriod = {},
        onWinnerCountChange = {},
        onWaitlistCapacityChange = {},
        onCreateClick = {},
    )
}
