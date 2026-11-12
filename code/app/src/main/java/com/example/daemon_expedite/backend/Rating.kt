package com.example.daemon_expedite.backend

/**
 * One review of a campsite: a star score and optional text, left by an entrant after a stay.
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
    }
}
