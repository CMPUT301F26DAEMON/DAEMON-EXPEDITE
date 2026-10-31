package com.example.daemon_expedite.backend

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Represents a campsite booking lottery with registration and winner selection windows.
 *
 * A lottery tracks which campsites it applies to, when users can register, and which users
 * ultimately win access to the available booking dates.
 *
 * @property id Unique identifier for the lottery.
 * @property waitingList Queue of users waiting to participate in the lottery.
 */
class Lottery(
    val id: Id = Id(),
    campsites: List<Id> = emptyList(),
    maxBookableByASingleUser: Int,
    registrationStart: Instant,
    registrationEnd: Instant,
    bookingStartDate: Instant,
    bookingEndDate: Instant,
    winnerCount: Int,
    winners: List<Id> = emptyList(),
    lotteryStart: Instant,
    lotteryEnd: Instant,
    availableDates: List<Instant> = emptyList(),
    waitingList: WaitingList = WaitingList()
) {
    var maxBookableByASingleUser: Int = maxBookableByASingleUser
        private set
    var registrationStart: Instant = registrationStart
        private set
    var registrationEnd: Instant = registrationEnd
        private set
    var bookingStartDate: Instant = bookingStartDate
        private set
    var bookingEndDate: Instant = bookingEndDate
        private set
    var winnerCount: Int = winnerCount
        private set
    var lotteryStart: Instant = lotteryStart
        private set
    var lotteryEnd: Instant = lotteryEnd
        private set
    val waitingList: WaitingList = waitingList

    private val _campsites = campsites.toMutableList()
    val campsites: List<Id> get() = _campsites

    private val _winners = winners.toMutableList()
    val winners: List<Id> get() = _winners

    private val _availableDates = availableDates.toMutableList()
    val availableDates: List<Instant> get() = _availableDates

    fun isActive(): Boolean {
        val now = Clock.System.now()
        return now >= lotteryStart && now < lotteryEnd
    }

    suspend fun joinWaitingList(user: Id): Boolean {
        if (!isActive()) return false
        if (waitingList.add(user)) {
            //Database.addEntrant(id, user)
            return true
        }
        return false
    }

    suspend fun leaveWaitingList(user: Id): Boolean {
        if (!isActive()) return false
        if (waitingList.remove(user)) {
            //Database.removeEntrant(id, user)
            return true
        }
        return false
    }

}