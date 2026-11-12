package com.example.daemon_expedite.backend

class Notification(
    val id: Id,
    val text: String,
    val receiversOfLottery: Id, // Lottery Id
    val senderId: Id,
    isRead: Boolean = false
) {
    var isRead: Boolean = isRead
        private set

    fun read(){
        this.isRead = true
    }
}
