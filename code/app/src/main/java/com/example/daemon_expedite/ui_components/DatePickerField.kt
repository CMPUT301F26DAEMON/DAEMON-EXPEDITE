// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * A form field that opens a date picker dialog. Shows the picked date as, for example,
 * "Mon, 5 Oct 2026".
 *
 * ```
 * DatePickerField(
 *     label = "Open date",
 *     date = openDate,
 *     onDateSelected = { openDate = it },
 *     minDate = LocalDate.now(),
 * )
 * ```
 *
 * @param label Field name, shown uppercase.
 * @param date The picked date, or null if none yet.
 * @param onDateSelected Called with the date when the owner taps OK.
 * @param modifier [Modifier] applied to the field.
 * @param icon Leading icon.
 * @param minDate Earliest date that can be picked, or null for no limit.
 * @param error Message shown under the field, or null.
 * @param enabled False greys the field out; tapping it does nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.Calendar,
    minDate: LocalDate? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }

    FormSelectField(
        label = label,
        value = date?.format(FIELD_FORMAT),
        onClick = { open = true },
        modifier = modifier,
        icon = icon,
        error = error,
        enabled = enabled,
    )

    if (open) {
        // The Material picker works in UTC milliseconds; LocalDate has no time zone, so UTC
        // midnight round-trips exactly.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: minDate)?.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    minDate == null || !utcTimeMillis.toLocalDate().isBefore(minDate)

                override fun isSelectableYear(year: Int): Boolean = minDate == null || year >= minDate.year
            },
        )
        val colors = DatePickerDefaults.colors(
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextSecondary,
            headlineContentColor = AppColors.TextPrimary,
            weekdayContentColor = AppColors.Secondary,
            subheadContentColor = AppColors.TextSecondary,
            navigationContentColor = AppColors.TextPrimary,
            yearContentColor = AppColors.TextPrimary,
            currentYearContentColor = AppColors.Accent,
            selectedYearContainerColor = AppColors.Primary,
            selectedYearContentColor = AppColors.TextOnPrimary,
            dayContentColor = AppColors.TextPrimary,
            disabledDayContentColor = AppColors.TextPrimary.copy(alpha = 0.3f),
            selectedDayContainerColor = AppColors.Primary,
            selectedDayContentColor = AppColors.TextOnPrimary,
            todayContentColor = AppColors.Accent,
            todayDateBorderColor = AppColors.Accent,
            dividerColor = AppColors.OutlineSubtle,
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateSelected(it.toLocalDate()) }
                        open = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text("OK", color = AppColors.Secondary) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel", color = AppColors.TextPrimary) }
            },
            colors = colors,
        ) {
            DatePicker(state = state, colors = colors, showModeToggle = false)
        }
    }
}

private val FIELD_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412)
@Composable
private fun DatePickerFieldPreview() {
    DatePickerField(label = "Open date", date = LocalDate.of(2026, 10, 1), onDateSelected = {})
}
