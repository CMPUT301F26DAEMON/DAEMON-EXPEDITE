// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Hook up the campsite rating pipeline", 2026-11-12.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

import java.time.LocalDate

/**
 * The rules for rating a stay. Pure functions, so they're easy to test, and run again inside
 * the rating transaction (see [Database.rateStay]).
 */
object RatingRules {

    /** Longest review text kept; anything longer is cut off. */
    const val MAX_REVIEW_LENGTH = 1000

    /**
     * Checks whether [user] may rate [reservation] on [today], given the campsite's
     * [existing] ratings. Returns a message saying why not, or null if they can.
     *
     * A stay can be rated once, by the person who booked it, after its check-out day has
     * arrived, and only if it wasn't cancelled.
     */
    fun rateProblem(reservation: Reservation, user: Id, existing: List<Rating>, today: LocalDate): String? = when {
        reservation.reservedBy != user -> "Only the person who stayed can rate it"
        reservation.isCancelled -> "Cancelled stays can't be rated"
        !reservation.isPast(today) -> "You can rate your stay once it's over"
        existing.any { it.reservationId == reservation.id } -> "You've already rated this stay"
        else -> null
    }

    /** Trims [text] and caps it at [MAX_REVIEW_LENGTH]; blank text becomes null (stars only). */
    fun cleanReview(text: String?): String? =
        text?.trim()?.take(MAX_REVIEW_LENGTH)?.takeIf { it.isNotEmpty() }
}
