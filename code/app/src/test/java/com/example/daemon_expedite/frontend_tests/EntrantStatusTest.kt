package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.WaitingList
import com.example.daemon_expedite.frontend.entrantStatus
import com.example.daemon_expedite.ui_components.LotteryStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class EntrantStatusTest {

    // entrantStatus uses the device zone for stay dates; entry times are exact moments.
    private val zone = ZoneId.systemDefault()
    private val me = Id()

    private fun day(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun midnight(date: LocalDate): Instant = date.atStartOfDay(zone).toInstant().toKotlinInstant()

    /** Entries 1–5 Oct, stays 16–18 Oct. */
    private fun lottery(users: List<Id> = emptyList(), capacity: Int = Int.MAX_VALUE) = Lottery(
        ownerId = Id(),
        campsites = listOf(Id()),
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(DatePeriod(day(10, 16), day(10, 18))),
        winnerCount = 1,
        waitingList = WaitingList(capacity = capacity, users = users),
    )

    @Test
    fun beforeEntriesOpen_isUpcoming() {
        assertEquals(LotteryStatus.UPCOMING, lottery().entrantStatus(me, midnight(day(9, 30))))
    }

    @Test
    fun whileOpen_joinedFullOrOpen() {
        val during = midnight(day(10, 3))
        assertEquals(LotteryStatus.OPEN, lottery().entrantStatus(me, during))
        assertEquals(LotteryStatus.JOINED, lottery(users = listOf(me)).entrantStatus(me, during))
        assertEquals(LotteryStatus.FULL, lottery(users = listOf(Id()), capacity = 1).entrantStatus(me, during))
        // Joined beats full: you can still leave a full list.
        assertEquals(LotteryStatus.JOINED, lottery(users = listOf(me), capacity = 1).entrantStatus(me, during))
    }

    @Test
    fun afterEntriesClose_joinedSeeDrawingOthersSeeNothing() {
        val after = midnight(day(10, 7))
        assertEquals(LotteryStatus.DRAWING, lottery(users = listOf(me)).entrantStatus(me, after))
        assertEquals(LotteryStatus.NONE, lottery().entrantStatus(me, after))
    }

    @Test
    fun afterTheDraw_wonLostOrNothing() {
        val after = midnight(day(10, 7))
        val other = Id()
        assertEquals(LotteryStatus.WON, drawn(users = listOf(me, other), winners = listOf(me)).entrantStatus(me, after))
        assertEquals(LotteryStatus.LOST, drawn(users = listOf(me, other), winners = listOf(other)).entrantStatus(me, after))
        assertEquals(LotteryStatus.NONE, drawn(users = listOf(other), winners = listOf(other)).entrantStatus(me, after))
        // Once stays begin the card has nothing to offer yet.
        assertEquals(LotteryStatus.NONE, drawn(users = listOf(me), winners = listOf(me)).entrantStatus(me, midnight(day(10, 16))))
    }

    private fun drawn(users: List<Id>, winners: List<Id>) = Lottery(
        ownerId = Id(),
        campsites = listOf(Id()),
        entryStart = midnight(day(10, 1)),
        entryEnd = midnight(day(10, 6)),
        stayPeriods = listOf(DatePeriod(day(10, 16), day(10, 18))),
        winnerCount = 1,
        waitingList = WaitingList(users = users),
        winners = winners,
        drawn = true,
    )
}
