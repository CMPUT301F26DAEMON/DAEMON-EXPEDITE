package com.example.daemon_expedite.backend

/**
 * Basic owner object. Inherits basic fields and functions form user and extends for owner functionality.
 *
 * @param ownerId Id. The id of the owner. Same as the user id.
 * @param ownerName String. The name of the owner. Same as the username.
 * @param ownerEmail String. The email of the owner when acting as a user.
 * @param ownerPhoneNumber String. The phone number of the owner when acting as a user.
 * @param ownerProfileImage Id. The profile image of the owner when acting as a user.
 * @param userLotteries List<Id>. The lotteries of the owner when acting as a user.
 * @param userReservations List<Id>. The reservations of the owner when acting as a user.
 * @param userChats List<Id>. The chats of the owner acting as a user.
 * @param ownerCampsites List<Id>. The campsites of the owner.
 * @param ownerNotifications List<Id>. The notifications of the owner.
 * @param ownerChats List<Id>. The chats of the owner.
 * @param ownerReservations List<Id>. The reservations of the owner.
 * @param ownerLotteries List<Id>. The lotteries of the owner.
 * */
class Owner(
    val ownerId: Id,
    val ownerName: String,
    val ownerEmail: String,
    val ownerPhoneNumber: String,
    val ownerProfileImage: Id,
    val userLotteries: List<Id>,
    val userReservations: List<Id>,
    val userChats: List<Id>,
    val ownerCampsites: List<Id>,
    val ownerNotifications: List<Id>,
    val ownerChats: List<Id>,
    val ownerReservations: List<Id>,
    val ownerLotteries: List<Id>,
) : User(
        ownerId,
        ownerName,
        ownerEmail,
        ownerPhoneNumber,
        ownerProfileImage,
        userLotteries,
        userReservations,
        userChats,
    )
