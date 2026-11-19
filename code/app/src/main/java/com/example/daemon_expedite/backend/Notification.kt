package com.example.daemon_expedite.backend

/**
 * A message an owner sends to everyone on one of their lotteries' waiting lists.
 *
 * One notification is stored once (in `notifications/<id>`) and its id is added to each
 * recipient's `receivedNotifications` and to the sender's `ownerNotifications`. When it was
 * sent comes from the id itself: see [Id.getTime].
 *
 * @property id The notification's id. Also gives the time it was sent.
 * @property text The message. Required.
 * @property title An optional short heading shown above the message.
 * @property receiversOfLottery The lottery whose waiting list it was sent to.
 * @property senderId The owner who sent it.
 * @param isRead Whether the user *viewing* it has read it. Read state is per recipient (the
 *   database keeps a `readBy` list), so this is filled in for whoever loaded the notification.
 */
class Notification(
    val id: Id,
    val text: String,
    val receiversOfLottery: Id, // Lottery Id
    val senderId: Id,
    isRead: Boolean = false,
    val title: String? = null,
) {
    var isRead: Boolean = isRead
        private set

    fun read(){
        this.isRead = true
    }
}
