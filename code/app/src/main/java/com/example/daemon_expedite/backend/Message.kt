package com.example.daemon_expedite.backend

data class Message(
    val id: Id,
    val message: String,
    val sender: Id,
)
