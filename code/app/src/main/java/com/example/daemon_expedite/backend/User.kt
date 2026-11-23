// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

import kotlin.collections.toList
import kotlin.collections.toMutableList

open class User(
<<<<<<< Updated upstream
    val id: Id = Id(),
    name: String = "",
    email: String = "",
    phoneNumber: String = "",
    notificationsEnabled: Boolean,
    var profileImage: Image? = null,
    lotteries: List<Id> = emptyList(),
    reservations: List<Id> = emptyList(),
    chats: List<Id> = emptyList(),
    receivedNotifications: List<Id> = emptyList(),
=======
    val id: Id,
    name: String,
    email: String,
    phoneNumber: String,
    val profileImage: Id,
    lotteries: List<Id> = emptyList(),
    reservations: List<Id> = emptyList(),
    chats: List<Id> = emptyList(),
>>>>>>> Stashed changes
) {
    var name: String = name
        private set
    var email: String = email
        private set
    var phoneNumber: String = phoneNumber
        private set
<<<<<<< Updated upstream
    var notificationsEnabled: Boolean = notificationsEnabled
        private set

    fun isProfileComplete(): Boolean = name.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank()

    private val _lotteries = lotteries.toMutableList()
    private val _reservations = reservations.toMutableList()
    private val _chats = chats.toMutableList()
    private val _receivedNotifications = receivedNotifications.toMutableList()

    val lotteryIds: List<Id> get() = _lotteries
    val reservationIds: List<Id> get() = _reservations
    val chatIds: List<Id> get() = _chats

    /** Ids of the notifications sent to this user, oldest first (read-only view). */
    val receivedNotifications: List<Id> get() = _receivedNotifications

    /** Changes the profile details. */
    fun updateProfile(
        name: String,
        email: String,
        phoneNumber: String,
    ) {
=======

    var notificationsEnabled: Boolean = true
        private set

    var isDeleted: Boolean = false
        private set

    private val lotteries: MutableList<Id> = lotteries.toMutableList()
    private val reservations: MutableList<Id> = reservations.toMutableList()
    private val chats: MutableList<Id> = chats.toMutableList()

    fun updateProfile(name: String, email: String, phoneNumber: String) {
>>>>>>> Stashed changes
        this.name = name
        this.email = email
        this.phoneNumber = phoneNumber
    }

<<<<<<< Updated upstream
    /** Sets the profile photo in memory. */
    fun updateProfileImage(image: Image) {
        profileImage = image
    }

    /** Turns notifications on or off in memory. */
=======
>>>>>>> Stashed changes
    fun enableOrDisableNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
    }

<<<<<<< Updated upstream
    /**
     * Remembers that this user joined [lotteryId]'s waiting list.
     */
    fun joinLottery(lotteryId: Id) {
        if (lotteryId !in _lotteries) _lotteries.add(lotteryId)
    }

    /**
     * Forgets [lotteryId].
     */
    fun leaveLottery(lotteryId: Id) {
        _lotteries.remove(lotteryId)
    }

    /**
     * Records [reservation] in memory. Returns false if it was already recorded.
     */
    fun addReservation(reservation: Reservation): Boolean {
        if (reservation.id in _reservations) return false
        return _reservations.add(reservation.id)
    }

    /** Records [chat] in memory. Returns false if it was already recorded. */
    fun addChat(chat: Chat): Boolean {
        if (chat.id in _chats) return false
        return _chats.add(chat.id)
    }

    /**
     * Records a notification sent to this user, in memory. Save the user with
     * [Database.updateUser] afterwards.
     *
     * @return false if the notification was already recorded.
     */
    fun addReceivedNotification(notification: Notification): Boolean {
        if (notification.id in _receivedNotifications) return false
        return _receivedNotifications.add(notification.id)
    }

    /** Clears the in-memory lists. */
    fun deleteProfile() {
        _lotteries.clear()
        _reservations.clear()
        _chats.clear()
        _receivedNotifications.clear()
    }
}
=======
    fun addLottery(lottery: Lottery) {
        if (lottery.id !in lotteries) lotteries.add(lottery.id)
    }

    fun removeLottery(lottery: Lottery) {
        lotteries.remove(lottery.id)
    }

    fun addReservation(reservation: Reservation) {
        if (reservation.id !in reservations) reservations.add(reservation.id)
    }

    fun getChats(): List<Id> = chats.toList()

    fun getLotteries(): List<Id> = lotteries.toList()
    fun getReservations(): List<Id> = reservations.toList()

    fun deleteProfile() {
        lotteries.clear()
        reservations.clear()
        chats.clear()
        isDeleted = true
    }
}
>>>>>>> Stashed changes
