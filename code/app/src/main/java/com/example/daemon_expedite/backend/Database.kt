package com.example.daemon_expedite.backend

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import kotlin.to
import kotlin.uuid.Uuid

interface DatabaseInterface {
    suspend fun addUser(user: User)

    suspend fun fetchUser(userId: String): Map<String, String>

    suspend fun fetchOwnUser(deviceId: String): User

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

    suspend fun addLottery(lottery: Lottery)

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

/**
 * Basic Firestore interaction class. Every database interaction goes through this object. Handles anonymous user authentication and caches images and chats.
 * To create a new instance of the database, use database.create().
 * @throws Exception: If authentication fails.
 * */

private class DatabaseInner private constructor() : DatabaseInterface {
    private val dbStorage = Firebase.storage
    private val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()

    companion object {
        /**
         * creates an instance of the database.
         *
         * @throws Exception: If authentication fails.
         * */
        suspend fun create(): DatabaseInner {
            val auth = FirebaseAuth.getInstance()

            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }

            return DatabaseInner()
        }
    }

    init {
    }

    /**
     * Function to add a user to the Firestore database. That's about it. Not much to see here.
     * */
    override suspend fun addUser(user: User) {
        val userdata =
            mapOf(
                "authId" to auth.currentUser?.uid,
                "id" to user.id.value.toString(),
                "name" to user.name,
                "email" to user.email,
                "phoneNumber" to user.phoneNumber,
                "profileImage" to user.profileImage.value.toString(),
                "lotteries" to user.lotteries.map { it.value.toString() },
                "reservations" to user.reservations.map { it.value.toString() },
                "chats" to user.chats.map { it.value.toString() },
            )
        db
            .collection("users")
            .document(user.id.value.toString())
            .set(userdata)
            .await()
    }

    override suspend fun fetchUser(userId: String): Map<String, String> {
        val userData =
            db
                .collection("users")
                .document(userId)
                .get()
                .await()
                .data
        if (userData == null) {
            throw Exception("User data is null")
        }
        val idData = userData["id"] as String
        val name = userData["name"] as String
        val email = userData["email"] as String
        val phoneNumber = userData["phoneNumber"] as String
        val profileImageData = userData["profileImage"] as String
        val lotteriesData = userData["lotteries"] as List<*>
        val reservationsData = userData["reservations"] as List<*>
        val chatsData = userData["chats"] as List<*>
        val lotteries = mutableListOf<Id>()
        val reservations = mutableListOf<Id>()
        val chats = mutableListOf<Id>()
        for (item in lotteriesData) {
            lotteries.add(Id(Uuid.parse(item as String)))
        }
        for (item in reservationsData) {
            reservations.add(Id(Uuid.parse(item as String)))
        }
        for (item in chatsData) {
            chats.add(Id(Uuid.parse(item as String)))
        }

        return mapOf<String, String>(
            "id" to idData,
            "name" to name,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "profileImageData" to profileImageData,
        )
    }

    override suspend fun fetchOwnUser(deviceId: String): User {
        val userData =
            db
                .collection("users")
                .whereEqualTo("authUid", auth.currentUser?.uid)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.reference
                ?.get()
                ?.await()
                ?.data
        if (userData == null) {
            throw Exception("User data is null")
        }
        val idData = userData["id"] as String
        val name = userData["name"] as String
        val email = userData["email"] as String
        val phoneNumber = userData["phoneNumber"] as String
        val profileImageData = userData["profileImageData"] as String
        val lotteriesData = userData["lotteries"] as List<*>
        val reservationsData = userData["reservations"] as List<*>
        val chatsData = userData["chats"] as List<*>
        val lotteries = mutableListOf<Id>()
        val reservations = mutableListOf<Id>()
        val chats = mutableListOf<Id>()
        for (item in lotteriesData) {
            lotteries.add(Id(Uuid.parse(item as String)))
        }
        for (item in reservationsData) {
            reservations.add(Id(Uuid.parse(item as String)))
        }
        for (item in chatsData) {
            chats.add(Id(Uuid.parse(item as String)))
        }
        return User(Id(Uuid.parse(idData)), name, email, phoneNumber, Id(Uuid.parse(profileImageData)), lotteries, reservations, chats)
    }

    override fun updateUser(user: User) {
        TODO("Not yet implemented")
    }

    override fun deleteUser(user: User) {
        TODO("Not yet implemented")
    }

    override fun fetchAllUsers(): List<User> {
        TODO("Not yet implemented")
    }

    override fun getOwner(deviceId: String): Owner {
        TODO("Not yet implemented")
    }

    override fun addCampsite(campsite: Campsite) {
        TODO("Not yet implemented")
    }

    override fun fetchCampsite(campsiteId: String): Campsite {
        TODO("Not yet implemented")
    }

    override fun searchCampsites(
        location: Location?,
        maxOccupancy: Int?,
        dates: List<Date>?,
    ): List<Campsite> {
        TODO("Not yet implemented")
    }

    override fun updateCampsite(campsite: Campsite) {
        TODO("Not yet implemented")
    }

    override fun deleteCampsite(id: Id) {
        TODO("Not yet implemented")
    }

    override suspend fun addLottery(lottery: Lottery) {
        val lotteryData =
            mapOf(
                "id" to lottery.id.value.toString(),
                "campsites" to lottery.campsites.map { it.value.toString() },
                "maxBookableByASingleUser" to lottery.maxBookableByASingleUser,
                "registrationStart" to lottery.registrationStart.toString(),
                "registrationEnd" to lottery.registrationEnd.toString(),
                "bookingStart" to lottery.bookingStart.toString(),
                "bookingEnd" to lottery.bookingEnd.toString(),
                "winnerCount" to lottery.winnerCount,
                "winners" to lottery.winners.map { it.value.toString() },
                "lotteryStart" to lottery.lotteryStart.toString(),
                "lotteryEnd" to lottery.lotteryEnd.toString(),
                "availableDates" to lottery.availableDates.map { it.toString() },
            )
        db
            .collection("lotteries")
            .document(lottery.id.value.toString())
            .set(lotteryData)
            .await()
    }

    override fun fetchLottery(id: Id): Lottery {
        TODO("Not yet implemented")
    }

    override fun updateLottery(lottery: Lottery) {
        TODO("Not yet implemented")
    }

    override fun addReservation(reservation: Reservation) {
        TODO("Not yet implemented")
    }

    override fun fetchReservation(id: Id): Reservation {
        TODO("Not yet implemented")
    }

    override fun updateReservation(reservation: Reservation) {
        TODO("Not yet implemented")
    }

    override fun uploadImage(
        path: String,
        uploaderId: Id,
    ): Id {
        TODO("Not yet implemented")
    }

    override fun addImageToCampsite(
        campsiteId: Id,
        path: String,
    ) {
        TODO("Not yet implemented")
    }

    override fun fetchImage(id: Id): Image {
        TODO("Not yet implemented")
    }

    override fun fetchAllImages(): List<Image> {
        TODO("Not yet implemented")
    }

    override fun deleteImage(id: Id) {
        TODO("Not yet implemented")
    }

    override fun uploadMessage(
        message: Message,
        chatId: Id,
    ) {
        TODO("Not yet implemented")
    }

    override fun fetchChat(id: Id): Chat {
        TODO("Not yet implemented")
    }

    override fun fetchAllChats(): List<Chat> {
        TODO("Not yet implemented")
    }

    override fun addNotification(notification: Notification) {
        TODO("Not yet implemented")
    }

    override fun fetchAllNotifications(): List<Notification> {
        TODO("Not yet implemented")
    }

    suspend fun getImage(path: String): String =
        try {
            dbStorage.reference
                .child(path)
                .downloadUrl
                .await()
                .toString()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception("Failed to load image $path", e)
        }
}

object Database {
    private lateinit var db: DatabaseInner

    suspend fun init() {
        db = DatabaseInner.create()
    }

    suspend fun addUser(user: User) {
        db.addUser(user)
    }

    suspend fun getImage(path: String): String = db.getImage(path)

    suspend fun addLottery(lottery: Lottery) {
        db.addLottery(lottery)
    }
}
