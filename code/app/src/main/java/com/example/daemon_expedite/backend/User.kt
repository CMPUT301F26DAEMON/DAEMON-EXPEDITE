package com.example.daemon_expedite.backend

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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

    suspend fun updateProfile(
        name: String,
        email: String,
        phoneNumber: String,
    ) {
        this.name = name
        this.email = email
        this.phoneNumber = phoneNumber
        // Database.updateUser(this)
    }

    suspend fun updateProfileImage(image: Image) {
        profileImage = image
        Database.updateUser(this)
    }

    suspend fun enableOrDisableNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        // Database.updateUser(this)
    }

    suspend fun addLottery(lottery: Lottery) {
        if (lottery.id !in _lotteries) {
            _lotteries.add(lottery.id)
            // Database.updateUser(this)
        }
    }

    suspend fun removeLottery(lottery: Lottery) {
        if (_lotteries.remove(lottery.id)) {
            //   Database.updateUser(this)
        }
    }

    suspend fun addReservation(reservation: Reservation) {
        if (reservation.id !in _reservations) {
            _reservations.add(reservation.id)
            //  Database.updateUser(this)
        }
    }

    suspend fun addChat(chat: Chat) {
        if (chat.id !in _chats) {
            _chats.add(chat.id)
            // Database.updateUser(this)
        }
    }

    /**
     suspend fun fetchLotteries(): List<Lottery> = coroutineScope {
     _lotteries.toList().map { async { Database.fetchLottery(it) } }.awaitAll()
     }

     suspend fun fetchReservations(): List<Reservation> = coroutineScope {
     _reservations.toList().map { async { Database.fetchReservation(it) } }.awaitAll()
     }

     suspend fun fetchChats(): List<Chat> = coroutineScope {
     _chats.toList().map { async { Database.fetchChat(it) } }.awaitAll()
     }
     */

    suspend fun deleteProfile() {
        // Database.deleteUser(id)
        _lotteries.clear()
        _reservations.clear()
        _chats.clear()
    }
}
