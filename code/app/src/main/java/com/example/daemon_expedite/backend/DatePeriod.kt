// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A range of nights at a campsite.
 *
 * Stays run from 2 PM on the first day to 11 AM the next, so a period is counted in nights:
 * [start] is the first night and [end] is the check-out day, which is **not** a night.
 * For example 16 Oct to 18 Oct is the nights of the 16th and 17th.
 *
 * This is also what the calendar's start and end taps mean, for both owners (stay periods) and
 * entrants (bookings).
 *
 * @property start First night.
 * @property end Check-out day. Must be after [start], so every period has at least one night.
 * @throws IllegalArgumentException if [end] is not after [start].
 */
data class DatePeriod(val start: LocalDate, val end: LocalDate) {
    init {
        require(end.isAfter(start)) { "A period needs at least one night: $start to $end" }
    }

    /** Number of nights, for example 2 for 16 Oct to 18 Oct. */
    val nights: Int get() = ChronoUnit.DAYS.between(start, end).toInt()

    /** True if the two periods share at least one night. Back-to-back periods don't overlap. */
    fun overlaps(other: DatePeriod): Boolean = start.isBefore(other.end) && other.start.isBefore(end)

    /** True if every night of [other] is also a night of this period. */
    fun contains(other: DatePeriod): Boolean = !other.start.isBefore(start) && !other.end.isAfter(end)
}
