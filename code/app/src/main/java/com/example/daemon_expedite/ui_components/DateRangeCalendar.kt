package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A month calendar for picking a date range (start and end date).
 *
 * Built from the "Availability calendar" frame in the Expedite Figma file.
 *
 * The composable is stateless with respect to the selection: it shows whatever
 * [startDate] and [endDate] you pass and reports taps through [onRangeChange].
 * Taps follow these rules (see [nextDateRange]):
 * - No start yet, or a full range is already chosen: the tapped day becomes the new start.
 * - Start chosen, tapped day is after it: the tapped day becomes the end.
 * - Start chosen, tapped day is on or before it: the tapped day becomes the new start.
 *
 * The visible month is kept internally and survives configuration changes.
 *
 * Usage:
 * ```
 * var start by remember { mutableStateOf<LocalDate?>(null) }
 * var end by remember { mutableStateOf<LocalDate?>(null) }
 *
 * DateRangeCalendar(
 *     startDate = start,
 *     endDate = end,
 *     onRangeChange = { s, e -> start = s; end = e },
 *     minDate = LocalDate.now(),
 * )
 * ```
 *
 * @param startDate First day of the selected range, or null if nothing is selected.
 * @param endDate Last day of the selected range, or null if only a start is selected.
 * @param onRangeChange Called with the new (start, end) pair after a tap.
 * @param modifier [Modifier] applied to the calendar card.
 * @param initialMonth Month shown first. Defaults to the month of [startDate], or the current month.
 * @param minDate Days before this are greyed out and cannot be tapped. Null means no limit.
 * @param maxDate Days after this are greyed out and cannot be tapped. Null means no limit.
 */
@Composable
fun DateRangeCalendar(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onRangeChange: (start: LocalDate?, end: LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = startDate?.let(YearMonth::from) ?: YearMonth.now(),
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    var visibleMonth by rememberSaveable { mutableStateOf(initialMonth) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(BorderStroke(1.dp, AppColors.OutlineBright), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        MonthHeader(
            month = visibleMonth,
            onPrevious = { visibleMonth = visibleMonth.minusMonths(1) },
            onNext = { visibleMonth = visibleMonth.plusMonths(1) },
        )
        WeekdayLabels()
        monthWeeks(visibleMonth).forEach { week ->
            Row(Modifier.fillMaxWidth().height(48.dp)) {
                week.forEachIndexed { column, date ->
                    Box(Modifier.weight(1f).height(48.dp)) {
                        if (date != null) {
                            val enabled = (minDate == null || !date.isBefore(minDate)) &&
                                (maxDate == null || !date.isAfter(maxDate))
                            DayCell(
                                date = date,
                                column = column,
                                startDate = startDate,
                                endDate = endDate,
                                enabled = enabled,
                                onClick = {
                                    val (s, e) = nextDateRange(startDate, endDate, date)
                                    onRangeChange(s, e)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns the new (start, end) range after [tapped] is pressed, given the
 * current [start] and [end]. See [DateRangeCalendar] for the rules.
 */
fun nextDateRange(
    start: LocalDate?,
    end: LocalDate?,
    tapped: LocalDate,
): Pair<LocalDate?, LocalDate?> = when {
    start == null || end != null -> tapped to null
    tapped.isAfter(start) -> start to tapped
    else -> tapped to null
}

/** Month title with the previous and next buttons on either side. */
@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val title = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthNavButton(AppIcons.ChevronLeft, "Previous month", onPrevious)
        Text(
            text = title,
            style = AppTextStyles.TitleSmall.copy(fontSize = 16.sp),
            color = AppColors.TextPrimary,
        )
        MonthNavButton(AppIcons.ChevronRight, "Next month", onNext)
    }
}

@Composable
private fun MonthNavButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AppColors.FillSubtle)
            .border(BorderStroke(1.dp, AppColors.OutlineStrong), CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColors.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** The M T W T F S S row. Weeks start on Monday, as in the design. */
@Composable
private fun WeekdayLabels() {
    Row(Modifier.fillMaxWidth().height(30.dp)) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
            Text(
                text = label,
                style = AppTextStyles.Caption,
                color = AppColors.Secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One day in the grid. Days inside the range get a green band; the band is
 * rounded at the start and end of the range and where it wraps to a new week.
 */
@Composable
private fun DayCell(
    date: LocalDate,
    column: Int,
    startDate: LocalDate?,
    endDate: LocalDate?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isStart = date == startDate
    val isEnd = date == endDate
    val inRange = startDate != null && endDate != null &&
        !date.isBefore(startDate) && !date.isAfter(endDate)
    val highlighted = isStart || isEnd || inRange

    val roundLeft = isStart || column == 0 || date.dayOfMonth == 1
    val roundRight = isEnd || endDate == null || column == 6 ||
        date.dayOfMonth == date.lengthOfMonth()
    val shape = RoundedCornerShape(
        topStart = if (roundLeft) 24.dp else 0.dp,
        bottomStart = if (roundLeft) 24.dp else 0.dp,
        topEnd = if (roundRight) 24.dp else 0.dp,
        bottomEnd = if (roundRight) 24.dp else 0.dp,
    )

    val description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) + when {
        isStart && isEnd -> ", start and end date"
        isStart -> ", start date"
        isEnd -> ", end date"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .then(if (highlighted) Modifier.background(AppColors.Primary) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = highlighted
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = if (highlighted) AppTextStyles.CalendarDateSelected else AppTextStyles.BodyMedium,
            color = if (enabled) AppColors.TextPrimary else AppColors.TextPrimary.copy(alpha = 0.3f),
        )
    }
}

/** A month can span at most 6 Monday-first weeks. */
private const val WEEKS_SHOWN = 6

/**
 * Splits [month] into Monday-first weeks of 7 slots; empty slots are null.
 *
 * Always returns [WEEKS_SHOWN] weeks so the calendar keeps the same height
 * when switching months (some months need 5 rows, others 6). Without this the
 * layout below the calendar would jump as the user pages through months.
 */
private fun monthWeeks(month: YearMonth): List<List<LocalDate?>> {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val slots = List<LocalDate?>(leadingBlanks) { null } + days
    val totalSlots = WEEKS_SHOWN * 7
    return (slots + List(totalSlots - slots.size) { null }).chunked(7)
}

/** October 2026 with 16 to 18 selected, matching the Figma frame. */
@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun DateRangeCalendarPreview() {
    var start by remember { mutableStateOf<LocalDate?>(LocalDate.of(2026, 10, 16)) }
    var end by remember { mutableStateOf<LocalDate?>(LocalDate.of(2026, 10, 18)) }
    Column(Modifier.padding(12.dp)) {
        DateRangeCalendar(
            startDate = start,
            endDate = end,
            onRangeChange = { s, e -> start = s; end = e },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Start: ${start ?: "-"}   End: ${end ?: "-"}",
            style = AppTextStyles.BodySmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(320.dp),
        )
    }
}
