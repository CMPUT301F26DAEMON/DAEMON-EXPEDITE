package com.example.daemon_expedite.backend

/**
 * Basic Chat object. Holds messages and users.
 *
 * @param id Id. The id of the chat.
 * @param ownUser Id. The id of the user who is the owner of the chat.
 * @param otherUser Id. The id of the user who is the other user of the chat.
 * @param messageOwn ArrayList<Message>. The messages of the user who is the owner of the chat.
 * @param messageOther ArrayList<Message>. The messages of the user who is the other user of the chat.
 * */
class Chat(
    val id: Id,
    val ownUser: Id,
    val otherUser: Id,
    private val messageOwn: MutableList<Message>,
    private val messageOther: MutableList<Message>,
) {
    /**
     * Returns the messages of the user who is the owner of the chat.
     *
     * @return List<Message>. The messages of the user who is the owner of the chat.
     * */
    fun getMessagesOwn(): List<Message> = messageOwn

    /**
     * Returns the messages of the user who is the other user of the chat.
     *
     * @return List<Message>. The messages of the user who is the other user of the chat.
     * */
    fun getMessageOther(): List<Message> = messageOther
}
