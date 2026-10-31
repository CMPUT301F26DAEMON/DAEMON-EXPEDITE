package com.example.daemon_expedite.backend

import org.junit.Test
import org.junit.Assert.*

class RatingTest {

    @Test
    fun testDefaultInitialization() {
        val rating = Rating()

        assertEquals(0, rating.totalScore)
        assertEquals(0, rating.count)
        assertEquals(0.0, rating.average(), 0.001)
    }

    @Test
    fun testCustomInitialization() {
        val rating = Rating(totalScore = 15, count = 3)

        assertEquals(15, rating.totalScore)
        assertEquals(3, rating.count)
        assertEquals(5.0, rating.average(), 0.001)
    }

    @Test
    fun testAddRating() {
        val rating = Rating()

        rating.addRating(4)

        assertEquals(4, rating.totalScore)
        assertEquals(1, rating.count)
    }

    @Test
    fun testAddMultipleRatings() {
        val rating = Rating()

        rating.addRating(5)
        rating.addRating(3)
        rating.addRating(4)

        assertEquals(12, rating.totalScore)
        assertEquals(3, rating.count)
    }

    @Test
    fun testAverage() {
        val rating = Rating()

        rating.addRating(5)
        rating.addRating(3)
        rating.addRating(4)

        assertEquals(4.0, rating.average(), 0.001)
    }

    @Test
    fun testAverageWithDecimalResult() {
        val rating = Rating()

        rating.addRating(5)
        rating.addRating(4)

        assertEquals(4.5, rating.average(), 0.001)
    }

    @Test
    fun testAverageWithNoRatings() {
        val rating = Rating()

        assertEquals(0.0, rating.average(), 0.001)
    }

    @Test
    fun testIdIsGenerated() {
        val rating1 = Rating()
        val rating2 = Rating()

        assertNotNull(rating1.id)
        assertNotNull(rating2.id)
        assertNotEquals(rating1.id, rating2.id)
    }
}