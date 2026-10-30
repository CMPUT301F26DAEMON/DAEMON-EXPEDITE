package com.example.daemon_expedite.backend

import kotlin.time.Instant

class Lottery(
    val id: Id,
    val campsites: MutableList<Id>,
    val maxBookableByASingleUser: Int,
    val registrationStart: Instant,
    val registrationEnd: Instant,
    val bookingStart: Instant,
    val bookingEnd: Instant,
    val winnerCount: Int,
    val winners: MutableList<Id>,
    val lotteryStart: Instant,
    val lotteryEnd: Instant,
    val availableDates: MutableList<Instant>,
)
