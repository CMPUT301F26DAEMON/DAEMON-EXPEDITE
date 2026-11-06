// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Picks several date ranges with one calendar: select a range, tap "Add these nights", repeat.
 * Added ranges show in a lighter green on the calendar and as chips underneath; tapping a chip
 * removes it.
 *
 * Ranges are counted in nights: the start is the first night and the end is the check-out
 * day, so 16 Oct to 18 Oct is 2 nights. A range can't overlap one already added, but can start
 * on another's check-out day.
 *
 * The picker only holds the range being picked; the added ranges are yours:
 * ```
 * var stays by remember { mutableStateOf(listOf<ClosedRange<LocalDate>>()) }
 * DateRangesPicker(
 *     ranges = stays,
 *     onAddRange = { start, end -> stays = (stays + (start..end)).sortedBy { it.start } },
 *     onRemoveRange = { index -> stays = stays - stays[index] },
 * )
 * ```
 *
 * @param ranges Ranges added so far, each from its first night to its check-out day.
 * @param onAddRange Called with (first night, check-out day) when "Add these nights" is tapped.
 * @param onRemoveRange Called with the index in [ranges] of the chip that was tapped.
 * @param modifier [Modifier] applied to the column.
 * @param minDate Earliest day that can be picked, or null for no limit.
 * @param error Message shown under the chips, or null.
 * @param isRemovable False for ranges that can't be removed; their chip has no ✕ and ignores taps.
 */
@Composable
fun DateRangesPicker(
    ranges: List<ClosedRange<LocalDate>>,
    onAddRange: (start: LocalDate, end: LocalDate) -> Unit,
    onRemoveRange: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    error: String? = null,
    isRemovable: (index: Int) -> Boolean = { true },
) {
    var start by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var end by rememberSaveable { mutableStateOf<LocalDate?>(null) }

    val nights = if (start != null && end != null) ChronoUnit.DAYS.between(start, end).toInt() else 0
    // Nights overlap if each range starts before the other's check-out day.
    val overlaps = remember(start, end, ranges) {
        val s = start
        val e = end
        s != null && e != null && ranges.any { s < it.endInclusive && it.start < e }
    }
    val canAdd = nights > 0 && !overlaps

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DateRangeCalendar(
            startDate = start,
            endDate = end,
            onRangeChange = { s, e -> start = s; end = e },
            minDate = minDate,
            addedRanges = ranges,
        )

        Text(
            text = when {
                overlaps -> "These nights overlap a range you already added."
                start == null -> "Tap the first night, then the check-out day."
                end == null -> "Now tap the check-out day."
                else -> "${start!!.short()} → ${end!!.short()} · ${nightsText(nights)}"
            },
            style = AppTextStyles.CaptionMultiline,
            color = if (overlaps) AppColors.Accent else AppColors.TextSecondary,
        )

        OutlinedButton(
            onClick = {
                onAddRange(start!!, end!!)
                start = null
                end = null
            },
            enabled = canAdd,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (canAdd) AppColors.Secondary else AppColors.OutlineStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AppColors.Surface,
                contentColor = AppColors.TextPrimary,
                disabledContainerColor = AppColors.Surface,
                disabledContentColor = AppColors.TextSecondary,
            ),
        ) {
            Icon(AppIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = if (canAdd) "  Add these nights (${nights})" else "  Add these nights",
                style = AppTextStyles.ButtonLabel,
            )
        }

        if (ranges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ranges.forEachIndexed { index, range ->
                    RangeChip(range, onRemove = if (isRemovable(index)) ({ onRemoveRange(index) }) else null)
                }
            }
        }

        if (error != null) {
            Text(error, style = AppTextStyles.Caption, color = AppColors.Danger, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun RangeChip(range: ClosedRange<LocalDate>, onRemove: (() -> Unit)?) {
    val nights = ChronoUnit.DAYS.between(range.start, range.endInclusive).toInt()
    val text = "${range.start.short()} → ${range.endInclusive.short()} · ${nightsText(nights)}"
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(AppColors.Primary)
            .then(
                if (onRemove != null) Modifier.clickable(role = Role.Button, onClickLabel = "Remove $text", onClick = onRemove)
                else Modifier
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = AppTextStyles.TabLabel, color = AppColors.TextOnPrimary)
        if (onRemove != null) {
            Icon(AppIcons.Cross, contentDescription = null, tint = AppColors.TextOnPrimary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        }
    }
}

private val SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

private fun LocalDate.short() = format(SHORT)

private fun nightsText(nights: Int) = if (nights == 1) "1 night" else "$nights nights"

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380, heightDp = 700)
@Composable
private fun DateRangesPickerPreview() {
    var ranges by remember {
        mutableStateOf(listOf(LocalDate.of(2026, 10, 16)..LocalDate.of(2026, 10, 18)))
    }
    DateRangesPicker(
        ranges = ranges,
        onAddRange = { s, e -> ranges = (ranges + (s..e)).sortedBy { it.start } },
        onRemoveRange = { i -> ranges = ranges.filterIndexed { j, _ -> j != i } },
        modifier = Modifier.padding(12.dp),
    )
}
