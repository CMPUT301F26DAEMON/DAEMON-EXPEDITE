package com.example.daemon_expedite.backend

<<<<<<< Updated upstream
import java.time.LocalDate

/**
 * One review of a campsite: a star score and optional text, left by an entrant after a stay.
 *
 * The rules for rating a stay ([rateProblem], [cleanReview]) are pure functions on the
 * companion, so they're easy to test, and run again inside the rating transaction
 * (see [Database.rateStay]).
 *
 * @property id Unique identifier for the rating record.
 * @property campsiteId The campsite being reviewed.
 * @property authorId The user who wrote the review.
 * @property score Stars given, from 1 to [MAX_RATING].
 * @property reservationId The stay this review is for.
 * @property description The review text, or null if the user only gave stars.
 * @throws IllegalArgumentException if [score] is outside 1..[MAX_RATING].
 */
class Rating(
    val id: Id = Id(),
    val campsiteId: Id,
    val authorId: Id,
    val score: Int,
    val reservationId: Id,
    val description: String? = null
) {
    init {
        require(score in 1..MAX_RATING) { "Rating must be 1 to $MAX_RATING stars, was $score" }
    }

    companion object {
        /** Highest rating allowed; matches the 5 stars in RatingPopup. */
        const val MAX_RATING = 5

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
}
=======
class Rating(
    val id: Id = Id(),
    totalScore: Int = 0,
    count: Int = 0
) {
    var totalScore: Int = totalScore
        private set

    var count: Int = count
        private set

    fun addRating(rating: Int) {
        totalScore += rating
        count++
    }

    fun average(): Double {
        return if (count == 0) 0.0 else totalScore.toDouble() / count
    }
}
>>>>>>> Stashed changes
