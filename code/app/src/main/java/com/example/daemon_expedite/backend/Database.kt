package com.example.daemon_expedite.backend

import android.location.Location
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlin.to
import kotlin.uuid.Uuid

interface DatabaseInterface {
    suspend fun addUser(user: User)

    suspend fun fetchUser(userId: String): Map<String, String>

    suspend fun fetchOwnUser(): User

    fun updateUser(user: User)

    fun deleteUser(user: User)

    fun fetchAllUsers(): List<User>

    suspend fun addOwner(owner: Owner)

    suspend fun getOwner(): Owner

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

    suspend fun fetchLottery(id: Id): Lottery

    suspend fun updateLottery(lottery: Lottery)

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
                "notificationsEnabled" to user.notificationsEnabled,
                "profileImage" to user.profileImage?.id?.value?.toString(),
                "lotteries" to user.lotteryIds.map { it.value.toString() },
                "reservations" to user.reservationIds.map { it.value.toString() },
                "chats" to user.chatIds.map { it.value.toString() },
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

    override suspend fun fetchOwnUser(): User {
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
        val notificationsEnabled = userData["notificationsEnabled"] as Boolean
        val profileImageData = userData["profileImageData"] as? String
        val profileImage = profileImageData?.let { fetchImageRecord(Id(Uuid.parse(it))) }
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
        return User(
            id = Id(Uuid.parse(idData)),
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            notificationsEnabled = notificationsEnabled,
            profileImage = profileImage,
            lotteries = lotteries,
            reservations = reservations,
            chats = chats,
        )
    }

    private suspend fun fetchImageRecord(id: Id): Image {
        val imageData =
            db
                .collection("images")
                .document(id.value.toString())
                .get()
                .await()
                .data ?: error("Image data is missing for ${id.value}")

        val imageId = Id(Uuid.parse(imageData["id"] as? String ?: error("Image id is missing")))
        val uploader = Id(Uuid.parse(imageData["uploader"] as? String ?: error("Image uploader is missing")))
        val path = imageData["path"] as? String ?: error("Image path is missing")
        return Image(imageId, uploader, path)
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

    override suspend fun addOwner(owner: Owner) {
        val userData =
            mapOf(
                "authId" to auth.currentUser?.uid,
                "id" to owner.id.value.toString(),
                "name" to owner.name,
                "email" to owner.email,
                "phoneNumber" to owner.phoneNumber,
                "notificationsEnabled" to owner.notificationsEnabled,
                "profileImage" to owner.profileImage?.id?.value?.toString(),
                "lotteries" to owner.lotteryIds.map { it.value.toString() },
                "reservations" to owner.reservationIds.map { it.value.toString() },
                "chats" to owner.chatIds.map { it.value.toString() },
                "ownerCampsites" to owner.ownerCampsites.map { it.value.toString() },
                "ownerNotification" to owner.sentNotifications.map { it.value.toString() },
                "ownerReservations" to owner.ownerReservations.map { it.value.toString() },
                "ownerLotteries" to owner.ownerLotteries.map { it.value.toString() },
            )
        db
            .collection("owners")
            .document(owner.id.value.toString())
            .set(userData)
            .await()
    }

    override suspend fun getOwner(): Owner {
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
        val notificationsEnabled = userData["notificationsEnabled"] as Boolean
        val profileImageData = userData["profileImageData"] as? String
        val profileImage = profileImageData?.let { fetchImageRecord(Id(Uuid.parse(it))) }
        val lotteriesData = userData["lotteries"] as List<*>
        val reservationsData = userData["reservations"] as List<*>
        val chatsData = userData["chats"] as List<*>
        val ownerCampsitesData = userData["ownerCampsites"] as List<*>
        val ownerNotificationsData = userData["ownerNotifications"] as List<*>
        val ownerChatsData = userData["ownerChats"] as List<*>
        val ownerReservationsData = userData["ownerReservations"] as List<*>
        val ownerLotteriesData = userData["ownerLotteries"] as List<*>
        val ownerCampsites = mutableListOf<Id>()
        val ownerNotifications = mutableListOf<Id>()
        val ownerChats = mutableListOf<Id>()
        val ownerReservations = mutableListOf<Id>()
        val ownerLotteries = mutableListOf<Id>()
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
        for (item in ownerCampsitesData) {
            ownerCampsites.add(Id(Uuid.parse(item.toString())))
        }
        for (item in ownerNotificationsData) {
            ownerNotifications.add(Id(Uuid.parse(item.toString())))
        }
        for (item in ownerChatsData) {
            ownerChats.add(Id(Uuid.parse(item.toString())))
        }
        for (item in ownerReservationsData) {
            ownerReservations.add(Id(Uuid.parse(item.toString())))
        }

        for (item in ownerLotteriesData) {
            ownerLotteries.add(Id(Uuid.parse(item.toString())))
        }
        return Owner(
            id = Id(Uuid.parse(idData)),
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            notificationsEnabled = notificationsEnabled,
            profileImage = profileImage,
            lotteries = lotteries,
            reservations = reservations,
            chats = chats,
            ownerCampsites = ownerCampsites,
            ownerReservations = ownerReservations,
            ownerLotteries = ownerLotteries,
            sentNotifications = ownerNotifications,
            ownerChats = ownerChats,
        )
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
                "bookingStart" to lottery.bookingStartDate.toString(),
                "bookingEnd" to lottery.bookingEndDate.toString(),
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

    override suspend fun fetchLottery(id: Id): Lottery {
        val lotteryData =
            db
                .collection("lotteries")
                .document(id.value.toString())
                .get()
                .await()
                .data
        if (lotteryData == null) {
            throw Exception("Lottery data is null")
        }
        val idData = lotteryData["id"] as String
        val campsitesData = lotteryData["campsites"] as List<*>
        val campsites = mutableListOf<Id>()
        for (item in campsitesData) {
            campsites.add(Id(Uuid.parse(item.toString())))
        }
        val maxBook = lotteryData["maxBookableByASingleUser"] as Int
        val registrationStart = lotteryData["registrationStart"] as String
        val registrationEnd = lotteryData["registrationEnd"] as String
        val bookingStart = lotteryData["bookingStart"] as String
        val bookingEnd = lotteryData["bookingEnd"] as String
        val winnerCount = lotteryData["winnerCount"] as Int
        val winnersData = lotteryData["winners"] as List<*>
        val winners = mutableListOf<Id>()
        for (item in winnersData) {
            winners.add(Id(Uuid.parse(item.toString())))
        }
        val lotteryStart = lotteryData["lotteryStart"] as String
        val lotteryEnd = lotteryData["lotteryEnd"] as String
        val availableDatesData = lotteryData["availableDates"] as List<*>
        val availableDates = mutableListOf<Instant>()
        for (item in availableDatesData) {
            availableDates.add(Instant.parse(item.toString()))
        }
        return Lottery(
            Id(Uuid.parse(idData)),
            campsites,
            maxBook,
            Instant.parse(registrationStart),
            Instant.parse(registrationEnd),
            Instant.parse(bookingStart),
            Instant.parse(bookingEnd),
            winnerCount,
            winners,
            Instant.parse(lotteryStart),
            Instant.parse(lotteryEnd),
            availableDates,
        )
    }

    override suspend fun updateLottery(lottery: Lottery) {
        addLottery(lottery)
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

    suspend fun fetchUser(userId: String): Map<String, String> = db.fetchUser(userId)

    suspend fun fetchOwnUser(): User = db.fetchOwnUser()

    suspend fun addOwner(owner: Owner) {
        db.addOwner(owner)
    }

    suspend fun fetchLottery(id: Id): Lottery = db.fetchLottery(id)

    suspend fun updateLottery(lottery: Lottery) {
        db.updateLottery(lottery)
    }

    suspend fun getOwner(): Owner = db.getOwner()

    suspend fun addUser(user: User) {
        db.addUser(user)
    }

    suspend fun getImage(path: String): String = db.getImage(path)

    suspend fun addLottery(lottery: Lottery) {
        db.addLottery(lottery)
    }
}