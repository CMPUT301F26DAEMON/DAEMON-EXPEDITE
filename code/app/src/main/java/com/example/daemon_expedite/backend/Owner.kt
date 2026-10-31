package com.example.daemon_expedite.backend

/**
 * A user who owns campsites. Inherits the basic profile fields and functions from [User]
 * and adds the owner-side state: campsites, lotteries, reservations, sent notifications
 * and owner chats.
 *
 * An owner is a user who has registered campsites, so the owner's id, name, email,
 * phone number, notification setting and profile image are the user's own.
 *
 * Mutating methods are `suspend` because they write to the database through [Database]
 * before updating the in-memory state. If the database call throws, the in-memory
 * state is left unchanged.
 *
 * @param id Id. The id of the owner. Same as the user id.
 * @param name String. The name of the owner. Same as the username.
 * @param email String. The email of the owner when acting as a user.
 * @param phoneNumber String. The phone number of the owner when acting as a user.
 * @param notificationsEnabled Boolean. Whether the owner has notifications enabled.
 * @param profileImage Image?. The profile image of the owner when acting as a user, if any.
 * @param lotteries List<Id>. The lotteries the owner has joined when acting as a user.
 * @param reservations List<Id>. The reservations the owner has made when acting as a user.
 * @param chats List<Id>. The chats of the owner when acting as a user.
 * @param ownerCampsites List<Id>. The ids of the campsites the owner has registered.
 * @param ownerReservations List<Id>. The ids of reservations made at the owner's campsites.
 * @param ownerLotteries List<Id>. The ids of lotteries the owner has created.
 * @param sentNotifications List<Id>. The ids of the notifications the owner has sent.
 * @param ownerChats List<Id>. The ids of the chats the owner has with entrants.
 */
open class Owner(
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
) : User(
    id,
    name,
    email,
    phoneNumber,
    notificationsEnabled,
    profileImage,
    lotteries,
    reservations,
    chats,
) {
    private val _ownerCampsites: MutableList<Id> = ownerCampsites.toMutableList()
    private val _ownerReservations: MutableList<Id> = ownerReservations.toMutableList()
    private val _ownerLotteries: MutableList<Id> = ownerLotteries.toMutableList()
    private val _sentNotifications: MutableList<Id> = sentNotifications.toMutableList()
    private val _ownerChats: MutableList<Id> = ownerChats.toMutableList()

    /** Ids of the campsites the owner has registered (read-only view). */
    val ownerCampsites: List<Id> get() = _ownerCampsites

    /** Ids of reservations made at the owner's campsites (read-only view). */
    val ownerReservations: List<Id> get() = _ownerReservations

    /** Ids of lotteries the owner has created (read-only view). */
    val ownerLotteries: List<Id> get() = _ownerLotteries

    /** Ids of notifications the owner has sent (read-only view). */
    val sentNotifications: List<Id> get() = _sentNotifications

    /**
     * Registers a new campsite for this owner.
     *
     * Saves the campsite to the database, records its id on this owner, then saves the
     * updated owner. Does nothing if the campsite is already registered to this owner.
     *
     * @param campsite Campsite. The campsite to register.
     */
    suspend fun addCampsite(campsite: Campsite) {
        if (campsite.id in _ownerCampsites) return
        //Database.addCampsite(campsite)
        _ownerCampsites.add(campsite.id)
        Database.addOwner(this)
    }

    /**
     * Records a lottery created by this owner.
     *
     * Saves the lottery to the database, records its id on this owner, then saves the
     * updated owner. Does nothing if the lottery is already recorded.
     *
     * @param lottery Lottery. The lottery to add.
     */
    suspend fun addOwnerLottery(lottery: Lottery) {
        if (lottery.id in _ownerLotteries) return
        Database.addLottery(lottery)
        _ownerLotteries.add(lottery.id)
        Database.addOwner(this)
    }

    /**
     * Records a reservation made at one of this owner's campsites.
     *
     * Saves the reservation to the database, records its id on this owner, then saves
     * the updated owner. Does nothing if the reservation is already recorded.
     *
     * @param reservation Reservation. The reservation to add.
     */
    suspend fun addOwnerReservation(reservation: Reservation) {
        if (reservation.id in _ownerReservations) return
        // Database.addReservation(reservation)
        _ownerReservations.add(reservation.id)
        Database.addOwner(this)
    }

    /**
     * Records a notification sent by this owner.
     *
     * Saves the notification to the database, records its id on this owner, then saves
     * the updated owner. Does nothing if the notification is already recorded.
     *
     * @param notification Notification. The notification that was sent.
     */
    /**
    suspend fun addSentNotification(notification: Notification) {
        if (notification.id in _sentNotifications) return
        Database.addNotification(notification)
        _sentNotifications.add(notification.id)
        Database.addOwner(this)
    }
    */


}