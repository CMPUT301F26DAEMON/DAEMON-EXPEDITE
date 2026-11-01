package com.example.daemon_expedite.backend

class Notification(
    val id: Id,
    val text: String,
    val receiversOfLottery: Id, // Lottery Id
    val senderId: Id,
)
