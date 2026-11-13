// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

/**
 * A user who owns campsites. Inherits the basic profile fields and functions from [User]
 * and adds the owner-side state: campsites, lotteries, reservations, sent notifications
 * and owner chats.
 *
 * An owner is a user who has registered campsites, so the owner's id, name, email,
 * phone number, notification setting and profile image are the user's own.
 *
 * Methods here only change the in-memory state. Saving is up to the caller (a ViewModel),
 * through [Database].
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
 * @param receivedNotifications List<Id>. The notifications sent to the owner when acting as a user.
 * @param ownerCampsites List<Id>. The ids of the campsites the owner has registered.
 * @param ownerReservations List<Id>. The ids of reservations made at the owner's campsites.
 * @param ownerLotteries List<Id>. The ids of lotteries the owner has created.
 * @param sentNotifications List<Id>. The ids of the notifications the owner has sent.
 * @param ownerChats List<Id>. The ids of the chats the owner has with users.
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
    receivedNotifications: List<Id> = emptyList(),
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
        receivedNotifications,
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
    val ownerChats: List<Id> get() = _ownerChats

    /**
     * Records [campsite] on this owner in memory. Save the owner with [Database.addOwner]
     * afterwards (the campsite itself must already be saved with [Database.addCampsite]);
     * if that fails, take it off again with [removeCampsite].
     *
     * @return false if the campsite was already recorded.
     */
    fun addCampsite(campsite: Campsite): Boolean {
        if (campsite.id in _ownerCampsites) return false
        return _ownerCampsites.add(campsite.id)
    }

    /** Forgets [campsite] in memory. Returns false if it wasn't recorded. */
    fun removeCampsite(campsite: Campsite): Boolean = _ownerCampsites.remove(campsite.id)

    /**
     * Records a lottery created by this owner in memory. Call it after [Database.createLottery],
     * which saves the lottery and records it on the owner in one write.
     *
     * @param lottery Lottery. Its [Lottery.ownerId] must be this owner.
     * @return false if the lottery was already recorded.
     */
    fun addOwnerLottery(lottery: Lottery): Boolean {
        if (lottery.id in _ownerLotteries) return false
        require(lottery.ownerId == id) { "Lottery ${lottery.id} belongs to another owner" }
        return _ownerLotteries.add(lottery.id)
    }

    /**
     * Records a reservation made at one of this owner's campsites, in memory. Save it with
     * [Database.addReservation] and the owner with [Database.addOwner].
     *
     * @return false if the reservation was already recorded.
     */
    fun addOwnerReservation(reservation: Reservation): Boolean {
        if (reservation.id in _ownerReservations) return false
        return _ownerReservations.add(reservation.id)
    }

    /**
     * Records a notification sent by this owner.
     */
    /**
     fun addSentNotification(notification: Notification): Boolean {
     if (notification.id in _sentNotifications) return false
     return _sentNotifications.add(notification.id)
     }
     */
}
