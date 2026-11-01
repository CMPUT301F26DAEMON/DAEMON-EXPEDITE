package com.example.daemon_expedite.backend

import android.location.Location
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlin.to
import kotlin.uuid.Uuid

/**
 * Thrown when a document that was asked for does not exist (as opposed to the database being
 * unreachable or a document being malformed).
 *
 * Callers use this to tell "first launch, nothing saved yet" apart from real errors. Treating
 * every error as "not found" would create a duplicate user or owner and overwrite the real one.
 */
class NotFoundException(message: String) : Exception(message)

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

    suspend fun fetchImage(id: Id): Image

    fun fetchAllImages(): List<Image>

    suspend fun deleteImage(id: Id)

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
        // Null when the user has no photo, so this must be a safe cast.
        val profileImageData = (userData["profileImage"] as? String).orEmpty()
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
            throw NotFoundException("User data is null")
        }
        val idData = userData["id"] as String
        val name = userData["name"] as String
        val email = userData["email"] as String
        val phoneNumber = userData["phoneNumber"] as String
        val notificationsEnabled = userData["notificationsEnabled"] as Boolean
        val profileImageData = userData["profileImage"] as? String
        // A missing or broken image record must not stop the user from loading: the caller
        // would treat that as "no user" and create a new, empty one.
        val profileImage = profileImageData?.let { fetchImageOrNull(it) }
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

    /**
     * Deletes the whole account.
     *
     * The user document is the account; the owner record is a profile attached to it. So this
     * also deletes the owner side if there is one ([deleteOwnerData]), and the profile photo.
     * (Deleting only the owner side, via [deleteOwner], leaves the user alone.)
     *
     * The owner side is deleted first: if that fails, the account is still there and the user
     * can try again, rather than ending up with an owner record that has no user.
     */
    override suspend fun deleteUser(user: User) {
        val userRef = db.collection("users").document(user.id.value.toString())
        if (ownerRef(user.id).get().await().exists()) {
            deleteOwnerData(user.id)
        }
        val photo = user.profileImage
        val batch = db.batch()
        batch.delete(userRef)
        if (photo != null) batch.delete(imageRef(photo.id))
        batch.commit().await()
        if (photo != null) deleteStorageFiles(listOf(photo.path))
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

    /**
     * Like [fetchImageRecord], but returns null (and logs) instead of throwing when the record is
     * missing or malformed. Use it wherever one broken image shouldn't stop the rest from loading.
     */
    private suspend fun fetchImageOrNull(imageId: String): Image? =
        try {
            fetchImageRecord(Id(Uuid.parse(imageId)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("DatabaseInner", "Skipping missing or malformed image '$imageId'", e)
            null
        }

    private fun imageRef(id: Id) = db.collection("images").document(id.value.toString())

    private fun ownerRef(id: Id) = db.collection("owners").document(id.value.toString())

    /** Deletes files from Firebase Storage. Best effort: a file that is already gone is skipped. */
    private suspend fun deleteStorageFiles(paths: List<String>) {
        for (path in paths.filter { it.isNotBlank() }) {
            try {
                dbStorage.reference.child(path).delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DatabaseInner", "Couldn't delete storage file $path", e)
            }
        }
    }

    override suspend fun updateUser(user: User) {
        addUser(user)
    }

    override fun fetchAllUsers(): List<User> {
        TODO("Not yet implemented")
    }

    /**
     * Saves the owner side of an account in `owners/<id>`.
     *
     * Like a user document, it has two identifiers:
     * - `authId`: the Firebase sign-in, used to find *your own* owner record ([getOwner]).
     * - `id`: the owner's [Id], used by other collections to point at this owner (for example a
     *   campsite's `owner` field). It is the same as the user's id: one person has one id across
     *   `users`, `owners` and `admins`, and the document is named with it.
     *
     * Name, email, phone and photo are not stored here; they live on the user document.
     */
    override suspend fun addOwner(owner: Owner) {
        val userData =
            mapOf(
                "authId" to auth.currentUser?.uid,
                "id" to owner.id.value.toString(),
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
        val ownerDoc =
            db
                .collection("owners")
                .whereEqualTo("authId", auth.currentUser?.uid)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
        val ownerData = ownerDoc?.data ?: throw NotFoundException("User data is null")

        // The owner's id, as other collections reference it. Required; same as the user's id.
        val ownerId = Id(Uuid.parse(ownerData["id"] as String))

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
            id = ownerId,
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

    /**
     * Deletes only the owner profile: the owner record, their campsites (and those campsites'
     * images, documents and files) and the lotteries they created. The user account is kept, so
     * switching to the owner view later creates a fresh owner.
     *
     * Entrants who joined those lotteries still have the ids in their own `lotteries` list. That
     * is intentional for now (the app skips lotteries that no longer exist) because the database
     * doesn't record who joined a lottery, so they can't be found to clean up.
     */
    suspend fun deleteOwner(owner: Owner) {
        deleteOwnerData(owner.id)
    }

    /**
     * Deletes everything on the owner side for [ownerId], reading the lists from the stored
     * owner document (not from an in-memory [Owner], which could be out of date).
     *
     * All documents are deleted in one batched write, so either all of them go or none do.
     * Storage files are deleted afterwards; if that part fails only unused files are left.
     */
    private suspend fun deleteOwnerData(ownerId: Id) {
        val ownerDoc = ownerRef(ownerId).get().await()
        val ownerData = ownerDoc.data ?: return // nothing to delete

        fun ids(key: String): List<String> =
            (ownerData[key] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        val campsiteIds = ids("ownerCampsites")
        val lotteryIds = ids("ownerLotteries")

        // Collect each campsite's image records first (ids for the batch, paths for Storage).
        val images = mutableListOf<Image>()
        for (campsiteId in campsiteIds) {
            val campsiteData =
                db.collection("campsites").document(campsiteId).get().await().data ?: continue
            (campsiteData["images"] as? List<*>)
                ?.mapNotNull { it?.toString()?.let { id -> fetchImageOrNull(id) } }
                ?.let(images::addAll)
        }

        val batch = db.batch()
        batch.delete(ownerDoc.reference)
        campsiteIds.forEach { batch.delete(db.collection("campsites").document(it)) }
        lotteryIds.forEach { batch.delete(db.collection("lotteries").document(it)) }
        images.forEach { batch.delete(imageRef(it.id)) }
        // TODO: also delete ownerReservations once reservations are stored in the database.
        batch.commit().await()

        deleteStorageFiles(images.map { it.path })
    }

    /**
     * Returns true if [userId] is an admin, i.e. has an entry in the "admins" collection.
     *
     * Admins are added by hand in the Firebase console. This accepts either a document named
     * with the user's id, or a document with an `id` field holding it, so it works whichever
     * way the entry was created.
     */
    suspend fun isAdmin(userId: Id): Boolean {
        val id = userId.value.toString()
        val admins = db.collection("admins")
        if (admins.document(id).get().await().exists()) return true
        return !admins.whereEqualTo("id", id).limit(1).get().await().isEmpty
    }

    override suspend fun addCampsite(campsite: Campsite) {
        val campsiteData =
            mapOf(
                "id" to campsite.id.value.toString(),
                "name" to campsite.name,
                "description" to campsite.description,
                "tags" to campsite.tags,
                // Must be a List: Firestore throws on Kotlin/Java arrays ("use Lists instead").
                "location" to listOf(campsite.location.latitude, campsite.location.longitude),
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

    /**
     * Builds a [Campsite] from its stored document.
     *
     * The document only stores image *ids*; each one is looked up in the "images" collection so
     * the returned images have their real uploader and Storage path. Missing images are skipped.
     */
    suspend fun parseCampsite(
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

        val tagsData = campsiteData["tags"] as? List<*>
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
            (campsiteData["images"] as? List<*>)
                ?.mapNotNull { item -> item?.toString()?.let { fetchImageOrNull(it) } }
                ?: emptyList()

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
        // Firestore returns whole numbers as Long, so `as Int` would always crash.
        val maxBook = (lotteryData["maxBookableByASingleUser"] as Number).toInt()
        val registrationStart = lotteryData["registrationStart"] as String
        val registrationEnd = lotteryData["registrationEnd"] as String
        val bookingStart = lotteryData["bookingStart"] as String
        val bookingEnd = lotteryData["bookingEnd"] as String
        val winnerCount = (lotteryData["winnerCount"] as Number).toInt()
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

    /**
     * Uploads the file at a local filesystem [path] as a profile photo.
     * See [uploadProfileImage] for where it is stored.
     */
    override suspend fun uploadImage(
        path: String,
        uploaderId: Id,
    ): Image = uploadProfileImage(Uri.fromFile(File(path)), uploaderId)

    /**
     * Uploads a profile photo to Storage at `profiles/<imageId>.jpg`, shrunk to at most
     * [ImageCompressor.PROFILE_MAX_SIDE_PX] on its longest side.
     */
    suspend fun uploadProfileImage(
        uri: Uri,
        uploaderId: Id,
    ): Image =
        uploadImageTo(uri, uploaderId, ImageCompressor.PROFILE_MAX_SIDE_PX) { imageId ->
            "profiles/${imageId.value}.jpg"
        }

    /**
     * Uploads a campsite photo to Storage at `campsites/<campsiteId>/<imageId>.jpg`, shrunk to
     * at most [ImageCompressor.CAMPSITE_MAX_SIDE_PX] on its longest side.
     */
    suspend fun uploadCampsiteImage(
        uri: Uri,
        campsiteId: Id,
        uploaderId: Id,
    ): Image =
        uploadImageTo(uri, uploaderId, ImageCompressor.CAMPSITE_MAX_SIDE_PX) { imageId ->
            "campsites/${campsiteId.value}/${imageId.value}.jpg"
        }

    /**
     * Uploads an image to Firebase Storage at the path [storagePathFor] gives, then saves its
     * record in the "images" collection.
     *
     * Storage layout (must match what is already in the bucket):
     * - profile photos: `profiles/<imageId>.jpg`
     * - campsite photos: `campsites/<campsiteId>/<imageId>.jpg`
     *
     * Takes a [Uri] so it works with the photo picker, which returns `content://` URIs that
     * can't be opened as a [File].
     *
     * Every upload is compressed first ([ImageCompressor]): scaled so its longest side is at most
     * [maxSidePx] and saved as JPEG, which is why every path ends in `.jpg`.
     *
     * The record is what other documents point to: users and campsites store only the image id,
     * and loading them reads `images/<id>` (a Firestore document, not a Storage folder) to get
     * the Storage path.
     */
    private suspend fun uploadImageTo(
        uri: Uri,
        uploaderId: Id,
        maxSidePx: Int,
        storagePathFor: (Id) -> String,
    ): Image {
        val imageId = Id()
        val imagePath = storagePathFor(imageId)
        // Firebase already holds the app context; borrowing it keeps context out of this API.
        val resolver = Firebase.app.applicationContext.contentResolver
        val jpeg = ImageCompressor.compressToJpeg(resolver, uri, maxSidePx)
        val metadata = storageMetadata { contentType = "image/jpeg" }
        dbStorage.reference.child(imagePath).putBytes(jpeg, metadata).await()
        imageRef(imageId)
            .set(
                mapOf(
                    "id" to imageId.value.toString(),
                    "uploader" to uploaderId.value.toString(),
                    "path" to imagePath,
                ),
            ).await()
        return Image(imageId, uploaderId, imagePath)
    }

    override fun addImageToCampsite(
        campsiteId: Id,
        path: String,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun fetchImage(id: Id): Image = fetchImageRecord(id)

    override fun fetchAllImages(): List<Image> {
        TODO("Not yet implemented")
    }

    /**
     * Deletes an image completely: its `images/<id>` record in Firestore and its file in Storage.
     *
     * The record is read first to find the Storage path. If the record is already gone there is
     * no path to delete, so only the (missing) record is "deleted", which is harmless.
     *
     * Callers must first remove every reference to the image (e.g. set the user's profileImage
     * to null and save), otherwise that document would point to an image that no longer exists.
     */
    override suspend fun deleteImage(id: Id) {
        val image = fetchImageOrNull(id.value.toString())
        imageRef(id).delete().await()
        if (image != null) deleteStorageFiles(listOf(image.path))
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

    /**
     * Searches campsites. Every filter is optional; leave it null to skip it.
     * [address] matches the start of the address and is case-sensitive.
     */
    suspend fun searchCampsite(
        location: Location? = null,
        radiusInMeters: Int? = null,
        maxOccupancy: Int? = null,
        address: String? = null,
        limit: Int? = null,
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

    /** Uploads a picked photo as a profile photo (`profiles/<imageId>.jpg`) and saves its record. */
    suspend fun uploadProfileImage(
        uploaderId: Id,
        uri: Uri,
    ): Image = db.uploadProfileImage(uri, uploaderId)

    /** Uploads a picked photo for a campsite (`campsites/<campsiteId>/<imageId>.jpg`) and saves its record. */
    suspend fun uploadCampsiteImage(
        campsiteId: Id,
        uploaderId: Id,
        uri: Uri,
    ): Image = db.uploadCampsiteImage(uri, campsiteId, uploaderId)

    suspend fun fetchImage(id: Id): Image = db.fetchImage(id)

    /** Deletes an image's record and its Storage file. Remove references to it first. */
    suspend fun deleteImage(id: Id) = db.deleteImage(id)

    suspend fun getImage(path: String): String = db.getImage(path)

    /** Deletes only the owner profile (owner record, campsites, their images, lotteries). */
    suspend fun deleteOwner(owner: Owner) = db.deleteOwner(owner)

    /** True if the user has an entry in the "admins" collection. */
    suspend fun isAdmin(userId: Id): Boolean = db.isAdmin(userId)
}
