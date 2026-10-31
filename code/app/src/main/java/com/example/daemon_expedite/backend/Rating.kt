package com.example.daemon_expedite.backend

/**
 * Aggregates user ratings for a campsite or similar entity.
 *
 * The rating keeps a running total score and count of submissions so the average can be
 * calculated without storing each individual review separately.
 *
 * @property id Unique identifier for the rating record.
 */
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