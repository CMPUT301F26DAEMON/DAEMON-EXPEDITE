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

/**
 * A user's id and display name, for pickers such as the admin's Messages screen.
 *
 * @param id Id. The user's id.
 * @param name String. The user's name, or "Unnamed user" if they never set one.
 */
data class NamedUser(
    val id: Id,
    val name: String,
)

/**
 * One of an owner's chats, with the entrant on the other side.
 *
 * @param chatId Id. The chat, for [Database.fetchChat].
 * @param entrant NamedUser. The user the owner is chatting with.
 */
data class OwnerChatPartner(
    val chatId: Id,
    val entrant: NamedUser,
)
