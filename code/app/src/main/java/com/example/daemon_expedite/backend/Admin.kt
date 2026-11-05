package com.example.daemon_expedite.backend

/**
 * A user with moderation powers. Inherits everything from [Owner] (and so [User]),
 * which lets an admin act as a user, an owner and an admin, switched by the UI
 * toggle. Adds no state of its own, only the functions to remove content and accounts.
 *
 * Mutating methods are `suspend` because they write to the database through [Database].
 * If the database call throws, nothing further is done.
 *
 * @param id Id. The id of the admin. Same as the user id.
 * @param name String. The name of the admin. Same as the username.
 * @param email String. The email of the admin when acting as a user.
 * @param phoneNumber String. The phone number of the admin when acting as a user.
 * @param notificationsEnabled Boolean. Whether the admin has notifications enabled.
 * @param profileImage Image?. The profile image of the admin when acting as a user, if any.
 * @param lotteries List<Id>. The lotteries the admin has joined when acting as a user.
 * @param reservations List<Id>. The reservations the admin has made when acting as a user.
 * @param chats List<Id>. The chats of the admin when acting as a user.
 * @param ownerCampsites List<Id>. The ids of the campsites the admin has registered.
 * @param ownerReservations List<Id>. The ids of reservations made at the admin's campsites.
 * @param ownerLotteries List<Id>. The ids of lotteries the admin has created.
 * @param sentNotifications List<Id>. The ids of the notifications the admin has sent.
 * @param ownerChats List<Id>. The ids of the chats the admin has with users.
 */
class Admin(
    id: Id = Id(),
    name: String = "",
    email: String = "",
    phoneNumber: String = "",
    notificationsEnabled: Boolean,
    profileImage: Image? = null,
    lotteries: List<Id> = emptyList(),
    reservations: List<Id> = emptyList(),
    chats: List<Id> = emptyList(),
    ownerCampsites: List<Id> = emptyList(),
    ownerReservations: List<Id> = emptyList(),
    ownerLotteries: List<Id> = emptyList(),
    sentNotifications: List<Id> = emptyList(),
    ownerChats: List<Id> = emptyList(),
) : Owner(
    id,
    name,
    email,
    phoneNumber,
    notificationsEnabled,
    profileImage,
    lotteries,
    reservations,
    chats,
    ownerCampsites,
    ownerReservations,
    ownerLotteries,
    sentNotifications,
    ownerChats,
) {

    /**
     * Removes an image from the database.
     *
     * @param imageId Id. The id of the image to remove.
     */
    suspend fun removeImage(imageId: Id) {
        Database.deleteImage(imageId)
    }

    /**
     * Removes a campsite from the database.
     *
     * @param campsiteId Id. The id of the campsite to remove.
     */
    suspend fun removeCampsite(campsiteId: Id) {
        // deleteCampsite needs the full campsite to also unlink it from its owner.
        Database.deleteCampsite(Database.fetchCampsite(campsiteId))
    }

    /**
     * Removes a user's profile from the database.
     *
     * @param userId Id. The id of the user whose profile should be removed.
     */
    suspend fun removeProfile(userId: Id) {
        Database.deleteUser(userId)
    }

    /**
     * Removes an owner and every campsite they have registered.
     *
     * Does nothing if the id does not belong to an owner.
     *
     * @param ownerId Id. The id of the owner to remove.
     */


    suspend fun removeOwner(ownerId: Id) {
        Database.deleteOwner(ownerId)
    }

}