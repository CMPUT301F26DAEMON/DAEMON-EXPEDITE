package com.example.daemon_expedite.backend

import org.junit.Test
import org.junit.Assert.*

class RatingTest {

    private val campsiteId = Id()
    private val authorId = Id()
    private val reservationId = Id()

    @Test
    fun testInitialization() {
        val rating = Rating(
            campsiteId = campsiteId,
            authorId = authorId,
            score = 4,
            reservationId = reservationId,
            description = "Great views",
        )

        assertEquals(campsiteId, rating.campsiteId)
        assertEquals(authorId, rating.authorId)
        assertEquals(4, rating.score)
        assertEquals(reservationId, rating.reservationId)
        assertEquals("Great views", rating.description)
    }

    @Test
    fun testDescriptionIsOptional() {
        val rating = Rating(campsiteId = campsiteId, authorId = authorId, score = 5, reservationId = reservationId)

        assertNull(rating.description)
    }

    @Test
    fun testIdIsGenerated() {
        val rating1 = Rating(campsiteId = campsiteId, authorId = authorId, score = 3, reservationId = reservationId)
        val rating2 = Rating(campsiteId = campsiteId, authorId = authorId, score = 3, reservationId = reservationId)

        assertNotNull(rating1.id)
        assertNotEquals(rating1.id, rating2.id)
    }
}
