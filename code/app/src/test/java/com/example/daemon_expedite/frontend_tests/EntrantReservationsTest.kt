package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.frontend.CampsiteSummary
import com.example.daemon_expedite.frontend.ReservationStatus
import com.example.daemon_expedite.frontend.datesText
import com.example.daemon_expedite.frontend.groupReservations
import com.example.daemon_expedite.frontend.status
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class EntrantReservationsTest {

    private val zone = ZoneId.of("America/Edmonton")
    private val pine = Id()
    private val maple = Id()
    private val campsites = mapOf(pine to summary(pine, "Pine Creek"), maple to summary(maple, "Maple"))

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun noon(date: LocalDate): Instant = date.atTime(12, 0).atZone(zone).toInstant().toKotlinInstant()

    /** 3 Oct at noon. */
    private val now = noon(day(10, 3))

    private fun summary(id: Id, name: String) = CampsiteSummary(
        id = id, name = name, location = "Banff", pricePerNight = "$40", rating = null, maxOccupancy = 4, imageUrl = null,
    )

    private fun reservation(
        from: LocalDate,
        to: LocalDate,
        campsite: Id = pine,
        cancelled: Boolean = false,
    ) = Reservation(
        campsite = campsite,
        reservationDates = arrayOf(noon(from), noon(to)),
        reservedBy = Id(),
        isCancelled = cancelled,
    )

    @Test
    fun cancelledIsCancelledEvenAfterItsDates() {
        assertEquals(ReservationStatus.CANCELLED, reservation(day(10, 10), day(10, 12), cancelled = true).status(now))
        assertEquals(ReservationStatus.CANCELLED, reservation(day(9, 1), day(9, 3), cancelled = true).status(now))
    }

    @Test
    fun pastWhenLastDateIsBeforeNow() {
        assertEquals(ReservationStatus.PAST, reservation(day(9, 28), day(10, 2)).status(now))
    }

    @Test
    fun activeWhenLastDateIsNotBeforeNow() {
        assertEquals(ReservationStatus.ACTIVE, reservation(day(10, 10), day(10, 12)).status(now))
        // A stay that has started but not ended is still active.
        assertEquals(ReservationStatus.ACTIVE, reservation(day(10, 1), day(10, 5)).status(now))
    }

    @Test
    fun usesTheLatestDateEvenIfNotLastInTheArray() {
        val outOfOrder = Reservation(
            campsite = pine,
            reservationDates = arrayOf(noon(day(10, 5)), noon(day(10, 1))),
            reservedBy = Id(),
        )
        assertEquals(ReservationStatus.ACTIVE, outOfOrder.status(now))
    }

    @Test
    fun groupsAndSortsEachTab() {
        val soon = reservation(day(10, 10), day(10, 12))
        val later = reservation(day(11, 1), day(11, 3), campsite = maple)
        val lastMonth = reservation(day(9, 1), day(9, 3))
        val lastWeek = reservation(day(9, 25), day(9, 27), campsite = maple)
        val cancelled = reservation(day(10, 20), day(10, 22), cancelled = true)

        val groups = groupReservations(listOf(later, lastMonth, cancelled, soon, lastWeek), campsites, now, zone)

        // Active: soonest first. Past: most recent first.
        assertEquals(listOf(soon.id, later.id), groups.active.map { it.id })
        assertEquals(listOf(lastWeek.id, lastMonth.id), groups.past.map { it.id })
        assertEquals(listOf(cancelled.id), groups.cancelled.map { it.id })
        assertEquals("Maple", groups.active[1].campsite.name)
        assertEquals("10 Oct – 12 Oct", groups.active[0].datesText)
    }

    @Test
    fun missingCampsiteStillGetsACard() {
        val orphan = reservation(day(10, 10), day(10, 12), campsite = Id())
        val groups = groupReservations(listOf(orphan), campsites, now, zone)
        assertEquals("Campsite unavailable", groups.active.single().campsite.name)
    }

    @Test
    fun datesText() {
        assertEquals("10 Oct – 12 Oct", reservation(day(10, 10), day(10, 12)).datesText(now, zone))
        assertEquals("10 Oct", reservation(day(10, 10), day(10, 10)).datesText(now, zone))
        assertEquals("30 Dec – 2 Jan 2027", reservation(day(12, 30), LocalDate.of(2027, 1, 2)).datesText(now, zone))
        val noDates = Reservation(campsite = pine, reservationDates = emptyArray(), reservedBy = Id())
        assertEquals("Dates not set", noDates.datesText(now, zone))
    }
}
