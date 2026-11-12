// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

open class User(
    val id: Id = Id(),
    name: String = "",
    email: String = "",
    phoneNumber: String = "",
    notificationsEnabled: Boolean,
    var profileImage: Image? = null,
    lotteries: List<Id> = emptyList(),
    reservations: List<Id> = emptyList(),
    chats: List<Id> = emptyList(),
) {
    var name: String = name
        private set
    var email: String = email
        private set
    var phoneNumber: String = phoneNumber
        private set
    var notificationsEnabled: Boolean = notificationsEnabled
        private set

    fun isProfileComplete(): Boolean = name.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank()

    private val _lotteries = lotteries.toMutableList()
    private val _reservations = reservations.toMutableList()
    private val _chats = chats.toMutableList()

    val lotteryIds: List<Id> get() = _lotteries
    val reservationIds: List<Id> get() = _reservations
    val chatIds: List<Id> get() = _chats

    /** Changes the profile details. */
    fun updateProfile(
        name: String,
        email: String,
        phoneNumber: String,
    ) {
        this.name = name
        this.email = email
        this.phoneNumber = phoneNumber
    }

    /** Sets the profile photo in memory. */
    fun updateProfileImage(image: Image) {
        profileImage = image
    }

    /** Turns notifications on or off in memory. */
    fun enableOrDisableNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    /** Records [lottery] in memory. Returns false if it was already recorded. */
    fun addLottery(lottery: Lottery): Boolean {
        if (lottery.id in _lotteries) return false
        return _lotteries.add(lottery.id)
    }

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

    /** Forgets [lottery] in memory. Returns false if it wasn't recorded. */
    fun removeLottery(lottery: Lottery): Boolean = _lotteries.remove(lottery.id)

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

    /** Clears the in-memory lists. */
    fun deleteProfile() {
        _lotteries.clear()
        _reservations.clear()
        _chats.clear()
    }
}
