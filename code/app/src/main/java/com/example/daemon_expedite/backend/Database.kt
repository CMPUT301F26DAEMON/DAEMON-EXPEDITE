package com.example.daemon_expedite.backend

import java.util.Date

interface Database {
    fun addUser(user: User)

    fun fetchUser(userId: String): User

    fun fetchOwnUser(deviceId: String): User

    fun updateUser(user: User)

    fun deleteUser(user: User)

    fun fetchAllUsers(): List<User>

    fun getOwner(deviceId: String): Owner

    fun addCampsite(campsite: Campsite)

    fun fetchCampsite(campsiteId: String): Campsite

    fun searchCampsites(
        location: Location? = null,
        maxOccupancy: Int? = null,
        dates: List<Date>? = null,
    ): List<Campsite>

    fun updateCampsite(campsite: Campsite)

    fun deleteCampsite(id: Id)

    fun addLottery(lottery: Lottery)

    fun fetchLottery(id: Id): Lottery

    fun updateLottery(lottery: Lottery)

    fun addReservation(reservation: Reservation)

    fun fetchReservation(id: Id): Reservation

    fun updateReservation(reservation: Reservation)

    fun uploadImage(
        path: String,
        uploaderId: Id,
    ): Id

    fun addImageToCampsite(
        campsiteId: Id,
        path: String,
    )

    fun fetchImage(id: Id): Image

    fun fetchAllImages(): List<Image>

    fun deleteImage(id: Id)

    fun uploadMessage(
        message: Message,
        chatId: Id,
    )

    fun fetchChat(id: Id): Chat

    fun fetchAllChats(): List<Chat>

    fun addNotification(notification: Notification)

    fun fetchAllNotifications(): List<Notification>
}
