package com.example.daemon_expedite.backend

/**
 * Basic Chat object. Holds messages and users.
 *
 * @param id Id. The id of the chat.
 * @param userA Id. The id of the first user
 * @param userB Id. The id of the second user
 * @param messages List<Message> last 50 messages in the chat
 * */
data class Chat(
    val id: Id,
    val userA: Id,
    val userB: Id,
    val messages: List<Message>,
)
