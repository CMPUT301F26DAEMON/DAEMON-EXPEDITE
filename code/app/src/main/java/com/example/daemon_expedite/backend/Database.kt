package com.example.daemon_expedite.backend

import android.location.Location
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlin.to
import kotlin.uuid.Uuid

interface DatabaseInterface {
    suspend fun addUser(user: User)

    suspend fun fetchUser(userId: Id): Map<String, String>

    suspend fun fetchOwnUser(): User

    suspend fun updateUser(user: User)

    suspend fun deleteUser(user: User)

    fun fetchAllUsers(): List<User>

    suspend fun addOwner(owner: Owner)

    suspend fun getOwner(): Owner

    suspend fun updateOwner(owner: Owner)

    suspend fun addCampsite(campsite: Campsite)

    suspend fun fetchCampsite(campsiteId: Id): Campsite

    suspend fun searchCampsites(
        location: Location? = null,
        radius: Int? = null,
        maxOccupancy: Int? = null,
        text: String? = null,
        limit: Int? = null,
    ): List<Campsite>

    suspend fun updateCampsite(campsite: Campsite)

    fun deleteCampsite(id: Id)

    suspend fun addLottery(lottery: Lottery)

    suspend fun fetchLottery(id: Id): Lottery

    suspend fun updateLottery(lottery: Lottery)

    fun addReservation(reservation: Reservation)

    fun fetchReservation(id: Id): Reservation

    fun updateReservation(reservation: Reservation)

    suspend fun uploadImage(
        path: String,
        uploaderId: Id,
    ): Image

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
                "profileImage" to
                    user.profileImage
                        ?.id
                        ?.value
                        ?.toString(),
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

    /**
     * Returns selective fields from User object(id, name, email, phone number, profile image)
     *
     * @param userId: Id of the user.
     * */
    override suspend fun fetchUser(userId: Id): Map<String, String> {
        val userData =
            db
                .collection("users")
                .document(userId.value.toString())
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
                .whereEqualTo("authId", auth.currentUser?.uid)
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
        val profileImageData = userData["profileImage"] as? String
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

    override suspend fun deleteUser(user: User) {
        db
            .collection("users")
            .document(user.id.value.toString())
            .delete()
            .await()
    }

    suspend fun fetchImageRecord(id: Id): Image {
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

    override suspend fun updateUser(user: User) {
        addUser(user)
    }

    override fun fetchAllUsers(): List<User> {
        TODO("Not yet implemented")
    }

    override suspend fun addOwner(owner: Owner) {
        val userData =
            mapOf(
                "authId" to auth.currentUser?.uid,
                "ownerCampsites" to owner.ownerCampsites.map { it.value.toString() },
                "ownerNotifications" to owner.sentNotifications.map { it.value.toString() },
                "ownerReservations" to owner.ownerReservations.map { it.value.toString() },
                "ownerLotteries" to owner.ownerLotteries.map { it.value.toString() },
                "ownerChats" to owner.ownerChats.map { it.value.toString() },
            )
        db
            .collection("owners")
            .document(owner.id.value.toString())
            .set(userData)
            .await()
    }

    override suspend fun getOwner(): Owner {
        val user = fetchOwnUser()
        val ownerData =
            db
                .collection("owners")
                .whereEqualTo("authId", auth.currentUser?.uid)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.reference
                ?.get()
                ?.await()
                ?.data
        if (ownerData == null) {
            throw Exception("User data is null")
        }

        fun parseIdList(
            data: Map<String, Any?>,
            key: String,
        ): List<Id> =
            (data[key] as? List<*>)
                ?.mapNotNull { item ->
                    item?.toString()?.let {
                        try {
                            Id(Uuid.parse(it))
                        } catch (_: Exception) {
                            Log.e("DatabaseInner", "Skipping malformed id '$it' in $key")
                            null
                        }
                    }
                }
                ?: emptyList()

        val ownerCampsites = parseIdList(ownerData, "ownerCampsites")
        val ownerNotifications = parseIdList(ownerData, "ownerNotifications")
        val ownerChats = parseIdList(ownerData, "ownerChats")
        val ownerReservations = parseIdList(ownerData, "ownerReservations")
        val ownerLotteries = parseIdList(ownerData, "ownerLotteries")
        return Owner(
            id = user.id,
            name = user.name,
            email = user.email,
            phoneNumber = user.phoneNumber,
            notificationsEnabled = user.notificationsEnabled,
            profileImage = user.profileImage,
            lotteries = user.lotteryIds,
            reservations = user.reservationIds,
            chats = user.chatIds,
            ownerCampsites = ownerCampsites,
            ownerReservations = ownerReservations,
            ownerLotteries = ownerLotteries,
            sentNotifications = ownerNotifications,
            ownerChats = ownerChats,
        )
    }

    override suspend fun updateOwner(owner: Owner) {
        addOwner(owner)
    }

    override suspend fun addCampsite(campsite: Campsite) {
        val campsiteData =
            mapOf(
                "id" to campsite.id.value.toString(),
                "name" to campsite.name,
                "description" to campsite.description,
                "tage" to campsite.tags,
                "location" to arrayOf(campsite.location.latitude, campsite.location.longitude),
                "maxOccupancy" to campsite.maxOccupancy,
                "policies" to campsite.policies,
                "pricePerNight" to campsite.pricePerNight,
                "images" to campsite.images.map { it.id.value.toString() },
                "owner" to campsite.ownerId.value.toString(),
                "rating" to mapOf("totalScore" to campsite.rating.totalScore, "count" to campsite.rating.count),
                "address" to campsite.address,
            )
        db
            .collection("campsites")
            .document(campsite.id.value.toString())
            .set(campsiteData)
            .await()
    }

    fun parseCampsite(
        campsiteId: Id,
        campsiteData: Map<String, Any?>,
    ): Campsite {
        val id =
            (campsiteData["id"] as? String)?.let {
                try {
                    Id(Uuid.parse(it))
                } catch (_: Exception) {
                    null
                }
            } ?: campsiteId

        val name =
            campsiteData["name"] as? String
                ?: throw Exception("${campsiteId.value}, missing name")
        val description = campsiteData["description"] as? String ?: ""

        val tagsData = (campsiteData["tags"] as? List<*>) ?: (campsiteData["tage"] as? List<*>)
        val tags = tagsData?.mapNotNull { it?.toString() } ?: emptyList()

        val locationData = campsiteData["location"] as? List<*>
        val latitude = (locationData?.getOrNull(0) as? Number)?.toDouble() ?: 0.0
        val longitude = (locationData?.getOrNull(1) as? Number)?.toDouble() ?: 0.0
        val location =
            Location("").apply {
                this.latitude = latitude
                this.longitude = longitude
            }

        val maxOccupancy = (campsiteData["maxOccupancy"] as? Number)?.toInt() ?: 0
        val policies = campsiteData["policies"] as? String ?: ""
        val pricePerNight = (campsiteData["pricePerNight"] as? Number)?.toInt() ?: 0

        val images =
            (campsiteData["images"] as? List<*>)?.mapNotNull { item ->
                item?.toString()?.let { imageIdStr ->
                    try {
                        Image(id = Id(Uuid.parse(imageIdStr)), uploader = id, path = "")
                    } catch (_: Exception) {
                        null
                    }
                }
            } ?: emptyList()

        val ownerStr =
            campsiteData["owner"] as? String
                ?: throw Exception("${campsiteId.value} missing owner")
        val ownerId =
            try {
                Id(Uuid.parse(ownerStr))
            } catch (e: Exception) {
                throw Exception("${ campsiteId.value }, malformed owner id '$ownerStr'")
            }

        val ratingMap = campsiteData["rating"] as? Map<*, *>
        val rating =
            Rating(
                totalScore = (ratingMap?.get("totalScore") as? Number)?.toInt() ?: 0,
                count = (ratingMap?.get("count") as? Number)?.toInt() ?: 0,
            )

        val address = campsiteData["address"] as? String ?: ""

        return Campsite(
            id = id,
            name = name,
            description = description,
            tags = tags,
            location = location,
            maxOccupancy = maxOccupancy,
            policies = policies,
            pricePerNight = pricePerNight,
            images = images,
            ownerId = ownerId,
            rating = rating,
            address = address,
        )
    }

    override suspend fun fetchCampsite(campsiteId: Id): Campsite {
        try {
            val campsiteSnapshot =
                db
                    .collection("campsites")
                    .document(campsiteId.value.toString())
                    .get()
                    .await()

            val campsiteData =
                campsiteSnapshot.data ?: throw Exception("${campsiteId.value}, campsite not found")
            return parseCampsite(campsiteId, campsiteData)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("DatabaseInner", "Error fetching campsite $campsiteId", e)
            throw e
        }
    }

    fun withinRadius(
        data: Map<String, Any?>,
        radius: Int?,
        location: Location?,
    ): Boolean {
        if (location == null || radius == null) return true
        val locationData = data["location"] as? List<*>
        val lat = (locationData?.getOrNull(0) as? Number)?.toDouble()
        val lng = (locationData?.getOrNull(1) as? Number)?.toDouble()
        if (lat == null || lng == null) return false
        val point =
            Location("").apply {
                latitude = lat
                longitude = lng
            }
        return location.distanceTo(point) <= radius
    }

    override suspend fun searchCampsites(
        location: Location?,
        radius: Int?,
        maxOccupancy: Int?,
        text: String?,
        limit: Int?,
    ): List<Campsite> {
        var query: Query = db.collection("campsites")

        if (maxOccupancy != null) {
            query = query.whereGreaterThanOrEqualTo("maxOccupancy", maxOccupancy)
        }
        if (!text.isNullOrEmpty()) {
            query =
                query
                    .orderBy("address")
                    .whereGreaterThanOrEqualTo("address", text)
                    .whereLessThan("address", text + '\uf8ff')
        }

        val results = mutableListOf<Campsite>()
        for (doc in query.get().await().documents) {
            if (limit != null && results.size >= limit) break

            val data = doc.data ?: continue
            if (!withinRadius(data, radius, location)) continue

            try {
                results.add(parseCampsite(Id(Uuid.parse(doc.id)), data))
            } catch (e: Exception) {
                Log.e("DatabaseInner", "Skipping malformed campsite ${doc.id}", e)
            }
        }
        return results
    }

    override suspend fun updateCampsite(campsite: Campsite) {
        addCampsite(campsite)
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

    override suspend fun uploadImage(
        path: String,
        uploaderId: Id,
    ): Image {
        val imageId = Id()
        val imagePath = "images/${imageId.value}"
        val imageRef = dbStorage.reference.child(imagePath)
        val fileObject = File(path)
        imageRef.putFile(Uri.fromFile(fileObject)).await()
        return Image(imageId, uploaderId, imagePath)
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

    suspend fun fetchUser(userId: Id): Map<String, String> = db.fetchUser(userId)

    suspend fun updateUser(user: User) = db.updateUser(user)

    suspend fun deleteUser(user: User) = db.deleteUser(user)

    suspend fun fetchOwnUser(): User = db.fetchOwnUser()

    suspend fun addOwner(owner: Owner) {
        db.addOwner(owner)
    }

    suspend fun getOwner(): Owner = db.getOwner()

    suspend fun updateOwner(owner: Owner) = db.updateOwner(owner)

    suspend fun addCampsite(campsite: Campsite) = db.addCampsite(campsite)

    suspend fun fetchCampsite(campsiteId: Id): Campsite = db.fetchCampsite(campsiteId)

    suspend fun searchCampsite(
        location: Location?,
        radiusInMeters: Int?,
        maxOccupancy: Int?,
        address: String?,
        limit: Int?,
    ): List<Campsite> = db.searchCampsites(location, radiusInMeters, maxOccupancy, address, limit)

    suspend fun addLottery(lottery: Lottery) {
        db.addLottery(lottery)
    }

    suspend fun fetchLottery(id: Id): Lottery = db.fetchLottery(id)

    suspend fun updateLottery(lottery: Lottery) {
        db.updateLottery(lottery)
    }

    suspend fun uploadImage(
        uploaderId: Id,
        path: String,
    ): Image = db.uploadImage(path, uploaderId)

    suspend fun getImage(path: String): String = db.getImage(path)
}
