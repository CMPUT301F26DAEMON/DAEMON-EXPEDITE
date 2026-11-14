// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Add lottery and booking database functions to our schema", 2026-11-02 to 2026-11-05.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

import android.location.Location
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.to
import kotlin.uuid.Uuid

/**
 * Thrown when a document that was asked for does not exist (as opposed to the database being
 * unreachable or a document being malformed).
 *
 * Callers use this to tell "first launch, nothing saved yet" apart from real errors. Treating
 * every error as "not found" would create a duplicate user or owner and overwrite the real one.
 */

interface DatabaseInterface {
    suspend fun addUser(user: User)

    suspend fun fetchUser(userId: Id): Map<String, String>

    suspend fun fetchOwnUser(): User

    suspend fun updateUser(user: User)

    suspend fun deleteUser(user: Id)

    suspend fun fetchAllUsers(): List<Map<String, String>>

    suspend fun addOwner(owner: Owner)

    suspend fun getOwner(): Owner

    suspend fun updateOwner(owner: Owner)

    suspend fun addCampsite(campsite: Campsite)

    suspend fun fetchCampsite(campsiteId: Id): Campsite

    suspend fun searchCampsites(
        location: String? = null,
        maxOccupancy: Int? = null,
        dates: DatePeriod? = null,
        titleText: String? = null,
        limit: Int = 7,
        cursor: SearchCursor? = null,
    ): CampsitePage

    suspend fun updateCampsite(campsite: Campsite)

    suspend fun deleteCampsite(campsite: Campsite)

    suspend fun addLottery(lottery: Lottery)

    suspend fun createLottery(lottery: Lottery)

    suspend fun fetchOwnerLotteries(ownerId: Id): List<Lottery>

    suspend fun fetchLotteriesForCampsite(campsiteId: Id): List<Lottery>

    suspend fun cancelLottery(lotteryId: Id): CancelOutcome

    suspend fun updateLotterySettings(edited: Lottery)

    suspend fun bookLotteryStay(
        lotteryId: Id,
        userId: Id,
        campsiteId: Id,
        period: DatePeriod,
    ): Reservation

    suspend fun declineLottery(
        lotteryId: Id,
        userId: Id,
    )

    suspend fun cancelReservation(
        reservationId: Id,
        userId: Id,
    ): Reservation

    suspend fun rateStay(
        reservationId: Id,
        userId: Id,
        score: Int,
        review: String?,
    ): Rating

    suspend fun fetchLottery(id: Id): Lottery

    suspend fun updateLottery(lottery: Lottery)

    suspend fun addReservation(reservation: Reservation)

    suspend fun fetchReservation(id: Id): Reservation?

    suspend fun addUserToLottery(
        lotteryId: Id,
        userId: Id,
    )

    suspend fun removeUserFromLottery(
        lotteryId: Id,
        userId: Id,
    )

    suspend fun updateReservation(reservation: Reservation)

    suspend fun uploadImage(
        path: String,
        uploaderId: Id,
    ): Image

    suspend fun addImage(
        campsiteId: Id,
        image: Image,
    )

    suspend fun fetchImage(id: Id): Image

    suspend fun fetchAllImages(): List<Image>

    suspend fun deleteImage(id: Id)

    suspend fun uploadMessage(
        message: Message,
        chatId: Id,
    )

    suspend fun fetchChat(
        id: Id,
        lastMessage: Message? = null,
    ): Chat

    suspend fun fetchChatWithUserId(
        userA: Id,
        userB: Id,
        lastMessage: Message? = null,
    ): Chat

    suspend fun sendLotteryNotification(lotteryId: Id, senderId: Id, title: String?, text: String): Notification

    suspend fun fetchReceivedNotifications(userId: Id): List<Notification>

    suspend fun markNotificationsRead(userId: Id, notificationIds: List<Id>)
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
                "receivedNotifications" to user.receivedNotifications.map { it.value.toString() },
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
            throw NotFoundException("User data is null")
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
            "profileImage" to profileImageData,
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
        // Users saved before this field existed have none, so treat a missing list as empty.
        val receivedNotifications =
            (userData["receivedNotifications"] as? List<*>)
                .orEmpty()
                .map { Id(Uuid.parse(it as String)) }
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
            receivedNotifications = receivedNotifications,
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
    override suspend fun deleteUser(user: Id) {
        val userRef = db.collection("users").document(user.value.toString())
        val userData = userRef.get().await().data
        if (ownerRef(user).get().await().exists()) {
            deleteOwnerData(user)
        }
        val photoId = userData?.get("profileImage") as? String
        val photo = photoId?.let { fetchImageOrNull(it) }
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
                dbStorage.reference
                    .child(path)
                    .delete()
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DatabaseInner", "Couldn't delete storage file $path", e)
            }
        }
    }

    /**
     * Saves the user's own fields.
     *
     * `receivedNotifications` is left out on purpose: other people add to it (see
     * [sendLotteryNotification]), so the in-memory [User] can be missing ids that arrived after
     * it was loaded. Writing the list here would delete them.
     */
    override suspend fun updateUser(user: User) {
        val data =
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
            .set(data, SetOptions.merge())
            .await()
    }

    /**
     * Returns all users in a map object with id, name, email, phoneNumber and profileImage
     * */
    override suspend fun fetchAllUsers(): List<Map<String, String>> {
        val allUserData = db.collection("users").get().await()
        val returnedUsers = mutableListOf<Map<String, String>>()
        for (userData in allUserData.documents) {
            val user = fetchUser(Id(Uuid.parse(userData["id"] as String)))
            returnedUsers.add(user)
        }
        return returnedUsers
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
            receivedNotifications = user.receivedNotifications,
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
     * Users who joined those lotteries still have the ids in their own `lotteries` list. That
     * is intentional for now (the app skips lotteries that no longer exist) because the database
     * doesn't record who joined a lottery, so they can't be found to clean up.
     */
    suspend fun deleteOwner(owner: Id) {
        deleteOwnerData(owner)
    }

    /**
     * Deletes everything on the owner side for [ownerId], reading the lists from the stored
     * owner document (not from an in-memory [Owner], which could be out of date).
     *
     * All documents are deleted in one batched write, so either all of them go or none do.
     * Storage files are deleted afterward; if that part fails only unused files are left.
     */
    private suspend fun deleteOwnerData(ownerId: Id) {
        val ownerDoc = ownerRef(ownerId).get().await()
        val ownerData = ownerDoc.data ?: return // nothing to delete

        fun ids(key: String): List<String> = (ownerData[key] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        val campsiteIds = ids("ownerCampsites")
        val lotteryIds = ids("ownerLotteries")
        val reservationIds = ids("ownerReservations")
        val reservationRef = db.collection("reservations")
        // Collect each campsite's image records first (ids for the batch, paths for Storage).
        val images = mutableListOf<Image>()
        for (campsiteId in campsiteIds) {
            val campsiteData =
                db
                    .collection("campsites")
                    .document(campsiteId)
                    .get()
                    .await()
                    .data ?: continue
            (campsiteData["images"] as? List<*>)
                ?.mapNotNull { it?.toString()?.let { id -> fetchImageOrNull(id) } }
                ?.let(images::addAll)
        }

        val batch = db.batch()
        batch.delete(ownerDoc.reference)
        campsiteIds.forEach { batch.delete(db.collection("campsites").document(it)) }
        lotteryIds.forEach { batch.delete(db.collection("lotteries").document(it)) }
        images.forEach { batch.delete(imageRef(it.id)) }
        reservationIds.forEach { batch.delete(reservationRef.document(it)) }
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
        if (admins
                .document(id)
                .get()
                .await()
                .exists()
        ) {
            return true
        }
        return !admins
            .whereEqualTo("id", id)
            .limit(1)
            .get()
            .await()
            .isEmpty
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
                "nameLower" to normalizeForSearch(campsite.name),
                "addressLower" to normalizeForSearch(campsite.address),
                "maxOccupancy" to campsite.maxOccupancy,
                "policies" to campsite.policies,
                "pricePerNight" to campsite.pricePerNight,
                "images" to campsite.images.map { it.id.value.toString() },
                "owner" to campsite.ownerId.value.toString(),
                "ratings" to campsite.ratings.map { it.toMap() },
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

        // Unreadable entries are skipped so one bad review doesn't hide the whole campsite.
        // Documents saved before reviews had text only kept a total and count, so they load
        // with no ratings.
        val ratings =
            (campsiteData["ratings"] as? List<*>)
                ?.mapNotNull { (it as? Map<*, *>)?.let(::parseRating) }
                ?: emptyList()

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
            ratings = ratings,
            address = address,
        )
    }

    private fun Rating.toMap(): Map<String, Any?> =
        mapOf(
            "id" to id.value.toString(),
            "campsiteId" to campsiteId.value.toString(),
            "authorId" to authorId.value.toString(),
            "score" to score,
            "reservationId" to reservationId.value.toString(),
            "description" to description,
        )

    private fun parseRating(data: Map<*, *>): Rating? =
        try {
            Rating(
                id = Id(Uuid.parse(data["id"] as String)),
                campsiteId = Id(Uuid.parse(data["campsiteId"] as String)),
                authorId = Id(Uuid.parse(data["authorId"] as String)),
                score = (data["score"] as Number).toInt(),
                reservationId = Id(Uuid.parse(data["reservationId"] as String)),
                description = data["description"] as? String,
            )
        } catch (e: Exception) {
            Log.e("DatabaseInner", "Unreadable rating $data", e)
            null
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

    /**
     * Function to search for campsites
     * @param location String? -> this is the location desired by the user
     * @param maxOccupancy Int? -> this is the maximum occupancy allowed
     * @param titleText String? -> this is the text to search for in the title of the campsite
     * @param limit Int -> this is the number of campsites to return
     * @param cursor SearchCursor? -> this is an internal pagination cursor, basically pass this in the next search campsites function call, and it will start from the last returned campsite. pass null for new query
     *
     * @return CampsitePage -> object which holds campsites and SearchCursor for the next search query, cursor will be null if no more campsites are found
     * */
    override suspend fun searchCampsites(
        location: String?,
        maxOccupancy: Int?,
        dates: DatePeriod?,
        titleText: String?,
        limit: Int,
        cursor: SearchCursor?,
    ): CampsitePage {
        val title = titleText?.let(::normalizeForSearch)?.takeIf { it.isNotEmpty() }
        val place = location?.let(::normalizeForSearch)?.takeIf { it.isNotEmpty() }

        var base: Query = db.collection("campsites")
        base =
            when {
                title != null -> {
                    base.orderBy("nameLower").startAt(title).endAt(title + '\uf8ff')
                }

                place != null -> {
                    base.orderBy("addressLower").startAt(place).endAt(place + '\uf8ff')
                }

                maxOccupancy != null -> {
                    base.orderBy("maxOccupancy").whereGreaterThanOrEqualTo("maxOccupancy", maxOccupancy)
                }

                else -> {
                    base.orderBy(FieldPath.documentId())
                }
            }

        fun passesFilters(data: Map<String, Any?>): Boolean {
            if (title != null) {
                val name = (data["name"] as? String)?.let(::normalizeForSearch) ?: return false
                if (!name.startsWith(title)) return false
            }
            if (place != null) {
                val address = (data["address"] as? String)?.let(::normalizeForSearch) ?: return false
                if (!address.startsWith(place)) return false
            }
            if (maxOccupancy != null) {
                val max = (data["maxOccupancy"] as? Number)?.toInt() ?: 0
                if (max < maxOccupancy) return false
            }
            return true
        }

        // One lottery fetch per owner per call, not per campsite
        val lotteryCache = mutableMapOf<Id, List<Lottery>>()

        suspend fun isAvailable(
            _campsiteId: Id,
            ownerId: Id,
        ): Boolean {
            if (dates == null) return true
            val lotteries = lotteryCache.getOrPut(ownerId) { fetchOwnerLotteries(ownerId) }
            return lotteries.any { lottery ->
                !lottery.drawn &&
                    !lottery.cancelled &&
                    // lottery.campsiteId == campsiteId &&   // if lotteries are per campsite
                    lottery.stayPeriods.any { it.contains(dates) }
            }
        }

        val batchSize = (limit * 2).toLong().coerceAtLeast(20)
        val results = mutableListOf<Campsite>()
        var lastDoc: DocumentSnapshot? = cursor?.lastDoc

        while (true) {
            var query = base.limit(batchSize)
            if (lastDoc != null) query = query.startAfter(lastDoc)

            val docs = query.get().await().documents

            for (doc in docs) {
                val data = doc.data ?: continue
                if (!passesFilters(data)) continue

                // Only parsing is guarded, so network errors still propagate
                val campsite =
                    try {
                        parseCampsite(Id(Uuid.parse(doc.id)), data)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e("DatabaseInner", "Skipping malformed campsite ${doc.id}", e)
                        continue
                    }

                if (!isAvailable(campsite.id, campsite.ownerId)) continue

                results.add(campsite)
                if (results.size >= limit) return CampsitePage(results, SearchCursor(doc))
            }

            if (docs.size < batchSize) return CampsitePage(results, null)
            lastDoc = docs.last()
        }
    }

    override suspend fun updateCampsite(campsite: Campsite) {
        addCampsite(campsite)
    }

    override suspend fun deleteCampsite(campsite: Campsite) {
        db
            .batch()
            .apply {
                delete(db.collection("campsites").document(campsite.id.value.toString()))
                update(
                    db.collection("owners").document(campsite.ownerId.value.toString()),
                    "ownerCampsites",
                    FieldValue.arrayRemove(campsite.id.value.toString()),
                )
            }.commit()
            .await()
    }

    // region Lotteries

    private fun lotteryRef(id: Id) = db.collection("lotteries").document(id.value.toString())

    /**
     * Saves [lottery], replacing any stored copy. For a brand-new lottery use [createLottery],
     * which also records it on the owner.
     *
     * Document layout (see [lotteryToMap]): times are Firestore Timestamps so the Cloud
     * Function can query "entry closed and not drawn yet"; stay and booking dates are
     * "yyyy-MM-dd" strings because they are calendar days, not moments.
     */
    override suspend fun addLottery(lottery: Lottery) {
        lotteryRef(lottery.id).set(lotteryToMap(lottery)).await()
    }

    /** Saves a new lottery and adds its id to the owner's `ownerLotteries`, in one write. */
    override suspend fun createLottery(lottery: Lottery) {
        db
            .batch()
            .apply {
                set(lotteryRef(lottery.id), lotteryToMap(lottery))
                update(ownerRef(lottery.ownerId), "ownerLotteries", FieldValue.arrayUnion(lottery.id.value.toString()))
            }.commit()
            .await()
    }

    override suspend fun fetchLottery(id: Id): Lottery {
        val data = lotteryRef(id).get().await().data ?: throw NotFoundException("Lottery ${id.value} not found")
        return parseLottery(id, data)
    }

    /** Every lottery created by [ownerId], in no particular order. Unreadable ones are skipped. */
    override suspend fun fetchOwnerLotteries(ownerId: Id): List<Lottery> =
        db
            .collection("lotteries")
            .whereEqualTo("owner", ownerId.value.toString())
            .get()
            .await()
            .documents
            .mapNotNull { parseLotteryOrNull(it.id, it.data) }

    /**
     * Every lottery that includes [campsiteId], past ones too. Unreadable ones are skipped.
     * At most one of them is not past (see [LotteryPhase.isPast]).
     */
    override suspend fun fetchLotteriesForCampsite(campsiteId: Id): List<Lottery> =
        db
            .collection("lotteries")
            .whereArrayContains("campsites", campsiteId.value.toString())
            .get()
            .await()
            .documents
            .mapNotNull { parseLotteryOrNull(it.id, it.data) }

    override suspend fun updateLottery(lottery: Lottery) {
        addLottery(lottery)
    }

    /**
     * Saves the owner's edits to a lottery's settings: campsites, entry period, stay periods,
     * winner count and waitlist limit. Nothing else is written, so entrants who join while the
     * owner is editing aren't lost, and winners and bookings are untouched.
     *
     * Runs as a transaction: it re-reads the lottery and checks the edit with
     * [LotteryRules.checkEdit] against that latest copy (its phase and waitlist), so an edit
     * that was fine when the form opened can't break the rules by the time it is saved.
     *
     * @param edited The lottery with the new settings. Its id says which lottery to update.
     * @throws LotteryEditRejectedException if the edit breaks the rules against the latest copy.
     */
    override suspend fun updateLotterySettings(edited: Lottery) {
        val ref = lotteryRef(edited.id)
        val zone = ZoneId.systemDefault()
        db
            .runTransaction { tx ->
                val data = tx.get(ref).data ?: throw NotFoundException("Lottery ${edited.id.value} not found")
                val current = parseLottery(edited.id, data)
                val problems =
                    LotteryRules.checkEdit(
                        original = current,
                        phase = current.phase(),
                        campsites = edited.campsites.toSet(),
                        entryFirstDay = edited.entryFirstDay(zone),
                        entryLastDay = edited.entryLastDay(zone),
                        stayPeriods = edited.stayPeriods,
                        winnerCount = edited.winnerCount,
                        waitlistCapacity = edited.waitlistCapacity,
                        today = LocalDate.now(zone),
                        zone = zone,
                    )
                if (!problems.isEmpty) throw LotteryEditRejectedException(edited.id, problems)
                val all = lotteryToMap(edited)
                val settings = listOf("campsites", "entryStart", "entryEnd", "stayPeriods", "winnerCount", "waitlistCapacity")
                tx.update(ref, settings.associateWith { all[it] })
                null
            }.await()
    }

    /**
     * Cancels a lottery, in a transaction that re-reads it so the phase check is up to date.
     *
     * - Not opened yet ([LotteryPhase.UPCOMING]): nobody can have joined, so it is deleted and
     *   taken off the owner's `ownerLotteries`.
     * - Open, drawing or booking: marked `cancelled` and kept, so entrants still see it under
     *   their past lotteries. Its campsites are free for a new lottery straight away.
     *
     * @throws LotteryNotCancellableException once stays have started, or if it is already over.
     */
    override suspend fun cancelLottery(lotteryId: Id): CancelOutcome {
        val ref = lotteryRef(lotteryId)
        return db
            .runTransaction { tx ->
                val data = tx.get(ref).data ?: throw NotFoundException("Lottery ${lotteryId.value} not found")
                val lottery = parseLottery(lotteryId, data)
                val phase = lottery.phase()
                when {
                    !phase.canCancel -> {
                        throw LotteryNotCancellableException(lotteryId)
                    }

                    phase == LotteryPhase.UPCOMING -> {
                        tx.delete(ref)
                        tx.update(ownerRef(lottery.ownerId), "ownerLotteries", FieldValue.arrayRemove(lotteryId.value.toString()))
                        CancelOutcome.DELETED
                    }

                    else -> {
                        tx.update(ref, "cancelled", true)
                        CancelOutcome.CANCELLED
                    }
                }
            }.await()
    }

    private fun lotteryToMap(lottery: Lottery): Map<String, Any?> =
        mapOf(
            "id" to lottery.id.value.toString(),
            "owner" to lottery.ownerId.value.toString(),
            "campsites" to lottery.campsites.map { it.value.toString() },
            "entryStart" to lottery.entryStart.toTimestamp(),
            "entryEnd" to lottery.entryEnd.toTimestamp(),
            "stayPeriods" to lottery.stayPeriods.map { it.toMap() },
            "winnerCount" to lottery.winnerCount,
            "waitlistCapacity" to lottery.waitlistCapacity,
            "users" to lottery.waitingList.users.map { it.value.toString() },
            "winners" to lottery.winners.map { it.value.toString() },
            "bookings" to lottery.bookings.map { it.toMap() },
            "declined" to lottery.declined.map { it.value.toString() },
            "drawn" to lottery.drawn,
            "cancelled" to lottery.cancelled,
        )

    /**
     * Builds a [Lottery] from its document. Not `suspend`, so it can run inside a transaction.
     *
     * @throws InvalidDataException if a required field is missing or malformed.
     */
    private fun parseLottery(
        id: Id,
        data: Map<String, Any?>,
    ): Lottery {
        fun fail(field: String): Nothing = throw InvalidDataException("Lottery ${id.value}: bad or missing '$field'")

        fun idOf(
            value: Any?,
            field: String,
        ): Id =
            try {
                Id(Uuid.parse(value as String))
            } catch (e: Exception) {
                fail(field)
            }

        fun ids(field: String): List<Id> = (data[field] as? List<*>)?.map { idOf(it, field) } ?: emptyList()

        fun time(field: String): Instant = (data[field] as? Timestamp)?.asInstant() ?: fail(field)

        fun period(
            map: Any?,
            field: String,
        ): DatePeriod =
            try {
                val m = map as Map<*, *>
                DatePeriod(LocalDate.parse(m["start"] as String), LocalDate.parse(m["end"] as String))
            } catch (e: Exception) {
                fail(field)
            }

        val capacity = (data["waitlistCapacity"] as? Number)?.toInt()
        return Lottery(
            id = id,
            ownerId = idOf(data["owner"], "owner"),
            campsites = ids("campsites"),
            entryStart = time("entryStart"),
            entryEnd = time("entryEnd"),
            stayPeriods = (data["stayPeriods"] as? List<*>)?.map { period(it, "stayPeriods") } ?: fail("stayPeriods"),
            winnerCount = (data["winnerCount"] as? Number)?.toInt() ?: fail("winnerCount"),
            waitingList = WaitingList(capacity = capacity ?: Int.MAX_VALUE, users = ids("users")),
            winners = ids("winners"),
            bookings =
                (data["bookings"] as? List<*>)?.map {
                    val m = it as? Map<*, *> ?: fail("bookings")
                    Booking(
                        user = idOf(m["user"], "bookings"),
                        campsite = idOf(m["campsite"], "bookings"),
                        period = period(m, "bookings"),
                        reservation = m["reservation"]?.let { r -> idOf(r, "bookings") },
                    )
                } ?: emptyList(),
            declined = ids("declined"),
            drawn = data["drawn"] as? Boolean ?: false,
            cancelled = data["cancelled"] as? Boolean ?: false,
        )
    }

    /** [parseLottery], but logs and returns null for a document that can't be read (for lists). */
    private fun parseLotteryOrNull(
        docId: String,
        data: Map<String, Any?>?,
    ): Lottery? =
        try {
            parseLottery(Id(Uuid.parse(docId)), data ?: return null)
        } catch (e: Exception) {
            Log.w("DatabaseInner", "Skipping unreadable lottery $docId", e)
            null
        }

    // endregion

    private fun reservationRef(id: Id) = db.collection("reservations").document(id.value.toString())

    private fun reservationToMap(reservation: Reservation): Map<String, Any?> =
        reservation.period.toMap() +
            mapOf(
                "id" to reservation.id.value.toString(),
                "campsite" to reservation.campsite.value.toString(),
                "reservedBy" to reservation.reservedBy.value.toString(),
                "lottery" to reservation.lotteryId?.value?.toString(),
                "isCancelled" to reservation.isCancelled,
            )

    /**
     * Reads reservations saved before they had `start`/`end`: a list of instants. The earliest
     * day is the first night and the latest the check-out day (one night if they're the same).
     */
    private fun legacyReservationPeriod(dates: List<*>): DatePeriod {
        val days =
            dates.map {
                Instant
                    .parse(it.toString())
                    .toJavaInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        val first = days.min()
        return DatePeriod(first, maxOf(days.max(), first.plusDays(1)))
    }

    override suspend fun addReservation(reservation: Reservation) {
        reservationRef(reservation.id).set(reservationToMap(reservation)).await()
    }

    /** The reservation with [id], or null if there isn't one or it can't be read. */
    override suspend fun fetchReservation(id: Id): Reservation? {
        val data = reservationRef(id).get().await().data ?: return null
        return parseReservation(id, data)
    }

    /** A reservation from its Firestore [data], or null if it can't be read. */
    private fun parseReservation(
        id: Id,
        data: Map<String, Any?>,
    ): Reservation? =
        try {
            Reservation(
                id = id,
                campsite = Id(Uuid.parse(data["campsite"] as String)),
                period =
                    (data["start"] as? String)
                        ?.let { DatePeriod(LocalDate.parse(it), LocalDate.parse(data["end"] as String)) }
                        ?: legacyReservationPeriod(data["reservationDates"] as List<*>),
                reservedBy = Id(Uuid.parse(data["reservedBy"] as String)),
                lotteryId = (data["lottery"] as? String)?.let { Id(Uuid.parse(it)) },
                isCancelled = data["isCancelled"] as? Boolean ?: false,
            )
        } catch (e: Exception) {
            Log.e("DatabaseInner", "Unreadable reservation ${id.value}", e)
            null
        }

    /**
     * Adds [userId] to the lottery's waiting list and the lottery to the user's `lotteries`.
     *
     * Runs as a transaction that re-reads the lottery, so the open check and the capacity
     * check use the latest data: two entrants can't both take the last spot. Joining twice
     * does nothing.
     *
     * @throws LotteryClosedException if the entry period isn't running.
     * @throws LotteryFullException if the waitlist limit is reached.
     */
    override suspend fun addUserToLottery(
        lotteryId: Id,
        userId: Id,
    ) {
        val lotteryRef = lotteryRef(lotteryId)
        val userRef = db.collection("users").document(userId.value.toString())
        val lotteryData = lotteryRef.get().await().data ?: throw NotFoundException("Lottery ${lotteryId.value} not found")
        if (lotteryData["owner"] as? String == userId.value.toString()) {
            throw Exception("cant add owner to their own lottery")
        }
        db
            .runTransaction { tx ->
                val data = tx.get(lotteryRef).data ?: throw NotFoundException("Lottery ${lotteryId.value} not found")
                val lottery = parseLottery(lotteryId, data)
                if (lottery.phase() != LotteryPhase.OPEN) throw LotteryClosedException(lotteryId)
                if (userId in lottery.waitingList.users) return@runTransaction null
                if (lottery.waitingList.isFull()) throw LotteryFullException(lotteryId)
                tx.update(lotteryRef, "users", FieldValue.arrayUnion(userId.value.toString()))
                tx.update(userRef, "lotteries", FieldValue.arrayUnion(lotteryId.value.toString()))
                null
            }.await()
    }

    /**
     * Removes [userId] from the lottery's waiting list and the lottery from the user's
     * `lotteries`. Only allowed while the entry period is running, so nobody can leave after
     * the draw.
     *
     * @throws LotteryClosedException if the entry period isn't running.
     */
    override suspend fun removeUserFromLottery(
        lotteryId: Id,
        userId: Id,
    ) {
        val lotteryRef = lotteryRef(lotteryId)
        val userRef = db.collection("users").document(userId.value.toString())
        db
            .runTransaction { tx ->
                val data = tx.get(lotteryRef).data ?: throw NotFoundException("Lottery ${lotteryId.value} not found")
                if (parseLottery(lotteryId, data).phase() != LotteryPhase.OPEN) throw LotteryClosedException(lotteryId)
                tx.update(lotteryRef, "users", FieldValue.arrayRemove(userId.value.toString()))
                tx.update(userRef, "lotteries", FieldValue.arrayRemove(lotteryId.value.toString()))
                null
            }.await()
    }

    /**
     * Books [period] at [campsiteId] for lottery winner [userId], first come first served.
     *
     * Runs as a transaction that re-reads the lottery and checks the booking with
     * [BookingRules.check] against that latest copy, so two winners can never get the same
     * night. In the same write it:
     * - adds the booking to the lottery's `bookings`,
     * - creates the [Reservation],
     * - adds the reservation to the user's `reservations` and the owner's `ownerReservations`.
     *
     * @return The new reservation.
     * @throws BookingRejectedException if the booking breaks a rule; its message says why.
     */
    override suspend fun bookLotteryStay(
        lotteryId: Id,
        userId: Id,
        campsiteId: Id,
        period: DatePeriod,
    ): Reservation {
        val ref = lotteryRef(lotteryId)
        val reservation = Reservation(campsite = campsiteId, period = period, reservedBy = userId, lotteryId = lotteryId)
        db
            .runTransaction { tx ->
                val data = tx.get(ref).data ?: throw NotFoundException("Lottery ${lotteryId.value} not found")
                val lottery = parseLottery(lotteryId, data)
                BookingRules
                    .check(lottery, lottery.phase(), userId, campsiteId, period)
                    ?.let { throw BookingRejectedException(it) }

                val booking = Booking(userId, campsiteId, period, reservation.id)
                tx.update(ref, "bookings", (lottery.bookings + booking).map { it.toMap() })
                tx.set(reservationRef(reservation.id), reservationToMap(reservation))
                tx.update(
                    db.collection("users").document(userId.value.toString()),
                    "reservations",
                    FieldValue.arrayUnion(reservation.id.value.toString()),
                )
                tx.update(ownerRef(lottery.ownerId), "ownerReservations", FieldValue.arrayUnion(reservation.id.value.toString()))
                null
            }.await()
        return reservation
    }

    /**
     * Records that winner [userId] turns their win down. Final: they can't book afterwards.
     * Only during the booking window, and only before they've booked (cancel the booking first).
     *
     * @throws BookingRejectedException if they can't decline; its message says why.
     */
    override suspend fun declineLottery(
        lotteryId: Id,
        userId: Id,
    ) {
        val ref = lotteryRef(lotteryId)
        db
            .runTransaction { tx ->
                val data = tx.get(ref).data ?: throw NotFoundException("Lottery ${lotteryId.value} not found")
                val lottery = parseLottery(lotteryId, data)
                val problem =
                    when {
                        lottery.phase() != LotteryPhase.BOOKING -> "Booking for this lottery is closed"
                        userId !in lottery.winners -> "Only winners can decline"
                        lottery.bookings.any { it.user == userId } -> "You've booked a stay. Cancel it before declining."
                        else -> null
                    }
                problem?.let { throw BookingRejectedException(it) }
                tx.update(ref, "declined", FieldValue.arrayUnion(userId.value.toString()))
                null
            }.await()
    }

    /**
     * Cancels [userId]'s reservation [reservationId], allowed any time before the stay starts
     * (see [BookingRules.cancelProblem]).
     *
     * If it was won in a lottery, the same transaction takes the booking out of the lottery's
     * `bookings`, so the nights are free again and every screen stops showing it as booked.
     * During the booking window the winner may then book again; after it the cancel is final,
     * so they're added to `declined` too.
     *
     * @return The cancelled reservation.
     * @throws BookingRejectedException if it can't be cancelled; its message says why.
     */
    override suspend fun cancelReservation(
        reservationId: Id,
        userId: Id,
    ): Reservation {
        val ref = reservationRef(reservationId)
        return db
            .runTransaction { tx ->
                // Firestore needs every read before the first write.
                val data = tx.get(ref).data ?: throw NotFoundException("Reservation ${reservationId.value} not found")
                val reservation =
                    parseReservation(reservationId, data)
                        ?: throw NotFoundException("Reservation ${reservationId.value} can't be read")
                BookingRules
                    .cancelProblem(reservation, userId, LocalDate.now())
                    ?.let { throw BookingRejectedException(it) }
                val lottery =
                    reservation.lotteryId?.let { id ->
                        tx.get(lotteryRef(id)).data?.let { parseLottery(id, it) }
                    }

                if (lottery != null) {
                    val lotteryDoc = lotteryRef(lottery.id)
                    tx.update(lotteryDoc, "bookings", BookingRules.bookingsAfterCancel(lottery, reservation).map { it.toMap() })
                    if (BookingRules.cancelGivesUpWin(lottery.phase())) {
                        tx.update(lotteryDoc, "declined", FieldValue.arrayUnion(userId.value.toString()))
                    }
                }
                tx.update(ref, "isCancelled", true)
                Reservation(
                    id = reservation.id,
                    campsite = reservation.campsite,
                    period = reservation.period,
                    reservedBy = reservation.reservedBy,
                    lotteryId = reservation.lotteryId,
                    isCancelled = true,
                )
            }.await()
    }

    override suspend fun updateReservation(reservation: Reservation) {
        addReservation(reservation)
    }

    /**
     * Saves [userId]'s rating of the stay [reservationId] onto its campsite's `ratings` array.
     *
     * Runs as a transaction that re-checks [RatingRules.rateProblem] against the campsite's
     * current ratings, then appends only the new rating with `arrayUnion`. It never rewrites the
     * whole campsite, so two people rating at the same time can't overwrite each other, and the
     * same stay can't be rated twice. The average isn't stored: [Campsite.averageRating]
     * works it out from `ratings` each time the campsite is loaded.
     *
     * @return The saved rating.
     * @throws BookingRejectedException if the stay can't be rated; its message says why.
     * @throws IllegalArgumentException if [score] is outside 1..[Rating.MAX_RATING].
     */
    override suspend fun rateStay(
        reservationId: Id,
        userId: Id,
        score: Int,
        review: String?,
    ): Rating {
        val ref = reservationRef(reservationId)
        return db
            .runTransaction { tx ->
                // Firestore needs every read before the first write.
                val data = tx.get(ref).data ?: throw NotFoundException("Reservation ${reservationId.value} not found")
                val reservation =
                    parseReservation(reservationId, data)
                        ?: throw NotFoundException("Reservation ${reservationId.value} can't be read")
                val campsiteRef = db.collection("campsites").document(reservation.campsite.value.toString())
                val campsiteData =
                    tx.get(campsiteRef).data
                        ?: throw BookingRejectedException("This campsite no longer exists")
                val existing =
                    (campsiteData["ratings"] as? List<*>)
                        ?.mapNotNull { (it as? Map<*, *>)?.let(::parseRating) }
                        ?: emptyList()
                RatingRules
                    .rateProblem(reservation, userId, existing, LocalDate.now())
                    ?.let { throw BookingRejectedException(it) }

                val rating =
                    Rating(
                        campsiteId = reservation.campsite,
                        authorId = userId,
                        score = score,
                        reservationId = reservation.id,
                        description = RatingRules.cleanReview(review),
                    )
                tx.update(campsiteRef, "ratings", FieldValue.arrayUnion(rating.toMap()))
                rating
            }.await()
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
        dbStorage.reference
            .child(imagePath)
            .putBytes(jpeg, metadata)
            .await()
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

    override suspend fun addImage(
        campsiteId: Id,
        image: Image,
    ) {
        val campsiteRef = db.collection("campsites").document(campsiteId.value.toString())
        val imageData =
            mapOf(
                "id" to image.id.value.toString(),
                "uploader" to image.uploader.value.toString(),
                "path" to image.path,
            )
        db
            .batch()
            .apply {
                set(imageRef(image.id), imageData)
                update(campsiteRef, "images", FieldValue.arrayUnion(image.id.value.toString()))
            }.commit()
            .await()
    }

    override suspend fun fetchImage(id: Id): Image = fetchImageRecord(id)

    override suspend fun fetchAllImages(): List<Image> {
        val snapshot = db.collection("images").get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                val idStr = data["id"] as? String ?: doc.id
                val uploaderStr = data["uploader"] as? String ?: return@mapNotNull null
                val path = data["path"] as? String ?: return@mapNotNull null
                Image(
                    id = Id(Uuid.parse(idStr)),
                    uploader = Id(Uuid.parse(uploaderStr)),
                    path = path,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("DatabaseInner", "Skipping malformed image document '${doc.id}'", e)
                null
            }
        }
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

    override suspend fun uploadMessage(
        message: Message,
        chatId: Id,
    ) {
        db
            .collection("chats")
            .document(chatId.value.toString())
            .get()
            .await()
            .exists() || throw NotFoundException("Chat ${chatId.value} not found")
        val messageMap =
            mapOf(
                "message" to message.message,
                "id" to message.id.value.toString(),
                "senderId" to message.sender.value.toString(),
            )
        db
            .collection("chats")
            .document(chatId.value.toString())
            .collection("messages")
            .document(message.id.value.toString())
            .set(messageMap)
            .await()
    }

    override suspend fun fetchChat(
        id: Id,
        lastMessage: Message?,
    ): Chat {
        val chatRef = db.collection("chats").document(id.value.toString())

        val (chatSnapshot, messageDocs) =
            coroutineScope {
                val chat = async { chatRef.get().await() }
                val messages =
                    async {
                        var query: Query =
                            chatRef
                                .collection("messages")
                                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                                .limit(50)
                        if (lastMessage != null) {
                            query = query.startAfter(lastMessage.id.value.toString())
                        }
                        query.get().await().documents
                    }
                chat.await() to messages.await()
            }

        val data = chatSnapshot.data
        if (!chatSnapshot.exists() || data == null) throw ChatNotFoundException(id)

        fun requireId(key: String): Id {
            val raw =
                data[key] as? String
                    ?: throw InvalidChatDataException(id, "missing or invalid '$key'")
            return try {
                Id(Uuid.parse(raw))
            } catch (_: IllegalArgumentException) {
                throw InvalidChatDataException(id, "malformed '$key': $raw")
            }
        }

        val messages =
            messageDocs
                .mapNotNull { doc ->
                    try {
                        val text = doc.getString("message") ?: error("no message text")
                        val sender = Id(Uuid.parse(doc.getString("senderId") ?: error("no sender")))
                        Message(Id(Uuid.parse(doc.id)), text, sender)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e("DatabaseInner", "Skipping malformed message ${doc.id} in chat ${id.value}", e)
                        null
                    }
                }

        return Chat(id, requireId("userA"), requireId("userB"), messages)
    }

    override suspend fun fetchChatWithUserId(
        userA: Id,
        userB: Id,
        lastMessage: Message?,
    ): Chat {
        val a = userA.value.toString()
        val b = userB.value.toString()

        val chatDoc =
            db
                .collection("chats")
                .where(
                    Filter.or(
                        Filter.and(Filter.equalTo("userA", a), Filter.equalTo("userB", b)),
                        Filter.and(Filter.equalTo("userA", b), Filter.equalTo("userB", a)),
                    ),
                ).limit(1) // doesn't matter ig because there can only be one chat for given two users
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: throw ChatNotFoundException(userA)
        return fetchChat(Id(Uuid.parse(chatDoc.id)), lastMessage)
    }

    // region Notifications

    private fun notificationRef(id: Id) = db.collection("notifications").document(id.value.toString())

    /**
     * Sends a notification from owner [senderId] to everyone on [lotteryId]'s waiting list.
     *
     * Stores the notification once in `notifications/<id>`, then adds its id to each entrant's
     * `receivedNotifications` and to the owner's `ownerNotifications` with `arrayUnion`, so it
     * never overwrites ids added by someone else at the same time. Entrants whose account no
     * longer exists are skipped (updating a missing document would fail the whole write).
     *
     * Firestore allows 500 writes per batch, so very long waiting lists are written in chunks.
     *
     * @param title Optional heading. Blank is treated as no title.
     * @param text The message. Must not be blank.
     * @return The saved notification. Record it on the in-memory owner with
     *   [Owner.addSentNotification].
     */
    override suspend fun sendLotteryNotification(
        lotteryId: Id,
        senderId: Id,
        title: String?,
        text: String,
    ): Notification {
        require(text.isNotBlank()) { "Notification text is required" }
        val lottery = fetchLottery(lotteryId)
        val notification =
            Notification(
                id = Id(),
                text = text.trim(),
                receiversOfLottery = lotteryId,
                senderId = senderId,
                title = title?.trim()?.takeIf { it.isNotEmpty() },
            )
        val idString = notification.id.value.toString()

        val recipients = existingUserRefs(lottery.waitingList.users)

        val first = db.batch()
        first.set(
            notificationRef(notification.id),
            mapOf(
                "id" to idString,
                "title" to notification.title,
                "text" to notification.text,
                "lottery" to lotteryId.value.toString(),
                "sender" to senderId.value.toString(),
                "readBy" to emptyList<String>(),
            ),
        )
        first.update(ownerRef(senderId), "ownerNotifications", FieldValue.arrayUnion(idString))
        // 2 writes already in the first batch.
        val firstChunk = recipients.take(498)
        firstChunk.forEach { first.update(it, "receivedNotifications", FieldValue.arrayUnion(idString)) }
        first.commit().await()

        recipients.drop(firstChunk.size).chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(it, "receivedNotifications", FieldValue.arrayUnion(idString)) }
            batch.commit().await()
        }
        return notification
    }

    /** References to the `users` documents that exist among [ids], looked up 30 at a time. */
    private suspend fun existingUserRefs(ids: List<Id>) =
        coroutineScope {
            ids
                .distinct()
                .chunked(30) // Firestore `in` limit
                .map { chunk ->
                    async {
                        db
                            .collection("users")
                            .whereIn(FieldPath.documentId(), chunk.map { it.value.toString() })
                            .get()
                            .await()
                            .documents
                            .map { it.reference }
                    }
                }.awaitAll()
                .flatten()
        }

    /**
     * The notifications sent to [userId], newest first (by [Id.getTime]).
     *
     * Reads the ids from the stored user document rather than an in-memory [User], because
     * owners add to that list after the user was loaded. Each notification's
     * [Notification.isRead] says whether *this* user has read it. Ids whose notification is
     * missing or malformed are skipped.
     */
    override suspend fun fetchReceivedNotifications(userId: Id): List<Notification> {
        val userData =
            db.collection("users").document(userId.value.toString()).get().await().data
                ?: throw NotFoundException("User ${userId.value} not found")
        val ids =
            (userData["receivedNotifications"] as? List<*>)
                .orEmpty()
                .mapNotNull { (it as? String)?.let { s -> runCatching { Uuid.parse(s) }.getOrNull() } }
                .distinct()
        val me = userId.value.toString()

        val docs =
            coroutineScope {
                ids
                    .chunked(30) // Firestore `in` limit
                    .map { chunk ->
                        async {
                            db
                                .collection("notifications")
                                .whereIn(FieldPath.documentId(), chunk.map { it.toString() })
                                .get()
                                .await()
                                .documents
                        }
                    }.awaitAll()
                    .flatten()
            }

        return docs
            .mapNotNull { doc ->
                runCatching {
                    Notification(
                        id = Id(Uuid.parse(doc.id)),
                        text = doc.getString("text")!!,
                        receiversOfLottery = Id(Uuid.parse(doc.getString("lottery")!!)),
                        senderId = Id(Uuid.parse(doc.getString("sender")!!)),
                        isRead = (doc.get("readBy") as? List<*>)?.contains(me) == true,
                        title = doc.getString("title")?.takeIf { it.isNotBlank() },
                    )
                }.onFailure { Log.e("DatabaseInner", "Skipping malformed notification ${doc.id}", it) }
                    .getOrNull()
            }.sortedByDescending { it.id.getTime() }
    }

    /**
     * Marks each of [notificationIds] as read by [userId] (adds the user to its `readBy`).
     * Other recipients are unaffected.
     */
    override suspend fun markNotificationsRead(userId: Id, notificationIds: List<Id>) {
        val me = userId.value.toString()
        notificationIds.distinct().chunked(500).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(notificationRef(it), "readBy", FieldValue.arrayUnion(me)) }
            batch.commit().await()
        }
    }

    // endregion

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

    private val PUNCTUATION = Regex("[\\p{P}\\p{S}]+")
    private val WHITESPACE = Regex("\\s+")

    /** Lowercase, punctuation removed, whitespace collapsed: "Calgary, Alberta" -> "calgary alberta" */
    fun normalizeForSearch(text: String): String =
        text
            .lowercase(Locale.ROOT)
            .replace(PUNCTUATION, " ")
            .replace(WHITESPACE, " ")
            .trim()
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

    suspend fun deleteUser(user: User) = db.deleteUser(user.id)

    suspend fun deleteUser(user: Id) = db.deleteUser(user)

    suspend fun fetchOwnUser(): User = db.fetchOwnUser()

    suspend fun addOwner(owner: Owner) {
        db.addOwner(owner)
    }

    suspend fun getOwner(): Owner = db.getOwner()

    suspend fun updateOwner(owner: Owner) = db.updateOwner(owner)

    suspend fun deleteOwner(owner: Owner) = db.deleteOwner(owner.id)

    suspend fun deleteOwner(owner: Id) = db.deleteOwner(owner)

    suspend fun addCampsite(campsite: Campsite) = db.addCampsite(campsite)

    suspend fun fetchCampsite(campsiteId: Id): Campsite = db.fetchCampsite(campsiteId)

    suspend fun updateCampsite(campsite: Campsite) = db.updateCampsite(campsite)

    /**
     * Searches campsites. Every filter is optional; leave it null to skip it.
     * [location] matches the start of the address and is case-sensitive.
     */
    suspend fun searchCampsite(
        location: String? = null,
        maxOccupancy: Int? = null,
        dates: DatePeriod? = null,
        titleText: String? = null,
        limit: Int = 7,
    ): CampsitePage = db.searchCampsites(location, maxOccupancy, dates, titleText, limit)

    suspend fun addLottery(lottery: Lottery) {
        db.addLottery(lottery)
    }

    suspend fun fetchLottery(id: Id): Lottery = db.fetchLottery(id)

    /** Saves a new lottery and records it on its owner in one write. */
    suspend fun createLottery(lottery: Lottery) = db.createLottery(lottery)

    /** Books a lottery winner's stay. See [DatabaseInner.bookLotteryStay]. */
    suspend fun bookLotteryStay(
        lotteryId: Id,
        userId: Id,
        campsiteId: Id,
        period: DatePeriod,
    ): Reservation = db.bookLotteryStay(lotteryId, userId, campsiteId, period)

    /** Turns down a lottery win. See [DatabaseInner.declineLottery]. */
    suspend fun declineLottery(
        lotteryId: Id,
        userId: Id,
    ) = db.declineLottery(lotteryId, userId)

    /** Cancels a reservation and frees its lottery booking. See [DatabaseInner.cancelReservation]. */
    suspend fun cancelReservation(
        reservationId: Id,
        userId: Id,
    ): Reservation = db.cancelReservation(reservationId, userId)

    /** Rates a finished stay and adds it to the campsite's ratings. See [DatabaseInner.rateStay]. */
    suspend fun rateStay(
        reservationId: Id,
        userId: Id,
        score: Int,
        review: String?,
    ): Rating = db.rateStay(reservationId, userId, score, review)

    /** Saves edited lottery settings without touching its waitlist. See [DatabaseInner.updateLotterySettings]. */
    suspend fun updateLotterySettings(edited: Lottery) = db.updateLotterySettings(edited)

    /** Cancels (or, if it hasn't opened yet, deletes) a lottery. See [DatabaseInner.cancelLottery]. */
    suspend fun cancelLottery(lotteryId: Id): CancelOutcome = db.cancelLottery(lotteryId)

    suspend fun fetchOwnerLotteries(ownerId: Id): List<Lottery> = db.fetchOwnerLotteries(ownerId)

    suspend fun fetchLotteriesForCampsite(campsiteId: Id): List<Lottery> = db.fetchLotteriesForCampsite(campsiteId)

    suspend fun updateLottery(lottery: Lottery) {
        db.updateLottery(lottery)
    }

    suspend fun addUserToLottery(
        lotteryId: Id,
        userId: Id,
    ) = db.addUserToLottery(lotteryId, userId)

    suspend fun removeUserFromLottery(
        lotteryId: Id,
        userId: Id,
    ) = db.removeUserFromLottery(lotteryId, userId)

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

    suspend fun addImage(
        campsiteId: Id,
        image: Image,
    ) = db.addImage(campsiteId, image)

    /** Deletes an image's record and its Storage file. Remove references to it first. */
    suspend fun deleteImage(id: Id) = db.deleteImage(id)

    suspend fun getImage(path: String): String = db.getImage(path)

    suspend fun fetchAllImages(): List<Image> = db.fetchAllImages()

    /** True if the user has an entry in the "admins" collection. */
    suspend fun isAdmin(userId: Id): Boolean = db.isAdmin(userId)

    suspend fun fetchChat(chatId: Id): Chat = db.fetchChat(chatId)

    suspend fun uploadMessage(
        message: Message,
        chatId: Id,
    ) = db.uploadMessage(message, chatId)

    suspend fun addReservation(reservation: Reservation) = db.addReservation(reservation)

    suspend fun fetchReservation(id: Id): Reservation? = db.fetchReservation(id)

    suspend fun updateReservation(reservation: Reservation) = db.updateReservation(reservation)

    suspend fun deleteCampsite(campsite: Campsite) = db.deleteCampsite(campsite)

    suspend fun fetchAllChats(
        userA: Id,
        userB: Id,
        lastMessage: Message?,
    ): Chat = db.fetchChatWithUserId(userA, userB, lastMessage)

    /** Sends a notification to everyone on a lottery's waiting list. See [DatabaseInner.sendLotteryNotification]. */
    suspend fun sendLotteryNotification(lotteryId: Id, senderId: Id, title: String?, text: String): Notification =
        db.sendLotteryNotification(lotteryId, senderId, title, text)

    /** The user's notifications, newest first, with their own read state. */
    suspend fun fetchReceivedNotifications(userId: Id): List<Notification> = db.fetchReceivedNotifications(userId)

    /** Marks the notifications as read for [userId] only. */
    suspend fun markNotificationsRead(userId: Id, notificationIds: List<Id>) =
        db.markNotificationsRead(userId, notificationIds)
}

// region Lottery document helpers

private fun Instant.toTimestamp(): Timestamp = Timestamp(epochSeconds, nanosecondsOfSecond)

private fun Timestamp.asInstant(): Instant = Instant.fromEpochSeconds(seconds, nanoseconds.toLong())

private fun DatePeriod.toMap(): Map<String, String> = mapOf("start" to start.toString(), "end" to end.toString())

private fun Booking.toMap(): Map<String, String?> =
    period.toMap() +
        mapOf(
            "user" to user.value.toString(),
            "campsite" to campsite.value.toString(),
            "reservation" to reservation?.value?.toString(),
        )

// endregion
