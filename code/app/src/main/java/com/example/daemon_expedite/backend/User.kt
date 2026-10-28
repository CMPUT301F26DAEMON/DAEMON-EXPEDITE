package com.example.daemon_expedite.backend

open class User(
    val id: Id,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val profileImage: Id,
    val lotteries: List<Id>,
    val reservations: List<Id>,
    val chats: List<Id>,
)
