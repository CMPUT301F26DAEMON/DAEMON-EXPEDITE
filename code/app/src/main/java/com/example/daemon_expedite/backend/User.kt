// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

open class User(
    val id: Id = Id(),
    name: String = "",
    email: String = "",
    phoneNumber: String = "",
    notificationsEnabled: Boolean,
    var profileImage: Image? = null,
    lotteries: List<Id> = emptyList(),
    reservations: List<Id> = emptyList(),
    chats: List<Id> = emptyList(),
) {
    var name: String = name
        private set
    var email: String = email
        private set
    var phoneNumber: String = phoneNumber
        private set
    var notificationsEnabled: Boolean = notificationsEnabled
        private set

    fun isProfileComplete(): Boolean = name.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank()

    private val _lotteries = lotteries.toMutableList()
    private val _reservations = reservations.toMutableList()
    private val _chats = chats.toMutableList()

    val lotteryIds: List<Id> get() = _lotteries
    val reservationIds: List<Id> get() = _reservations
    val chatIds: List<Id> get() = _chats

    suspend fun updateProfile(
        name: String,
        email: String,
        phoneNumber: String,
    ) {
        this.name = name
        this.email = email
        this.phoneNumber = phoneNumber
        Database.updateUser(this)
    }

    suspend fun updateProfileImage(image: Image) {
        profileImage = image
        Database.updateUser(this)
    }

    suspend fun enableOrDisableNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        Database.updateUser(this)
    }

    suspend fun addLottery(lottery: Lottery) {
        if (lottery.id !in _lotteries) {
            _lotteries.add(lottery.id)
            Database.updateUser(this)
        }
    }

    /**
     * Joins [lotteryId]'s waiting list. [Database.addUserToLottery] saves both sides (the lottery's
     * list and this user's `lotteries`) in a transaction that checks the lottery is open and not
     * full; then the lottery is remembered here too.
     *
     * @throws LotteryClosedException if entries aren't open.
     * @throws LotteryFullException if the waiting list is full.
     */
    suspend fun joinLottery(lotteryId: Id) {
        Database.addUserToLottery(lotteryId, id)
        if (lotteryId !in _lotteries) _lotteries.add(lotteryId)
    }

    /**
     * Leaves [lotteryId]'s waiting list. Saved by [Database.removeUserFromLottery], which only
     * allows it while entries are open.
     *
     * @throws LotteryClosedException if entries aren't open.
     */
    suspend fun leaveLottery(lotteryId: Id) {
        Database.removeUserFromLottery(lotteryId, id)
        _lotteries.remove(lotteryId)
    }

    /**
     * Books [period] at [campsiteId] as a winner of [lotteryId]. [Database.bookLotteryStay] saves
     * the booking, the reservation and this user's `reservations` in one transaction; then the
     * reservation is remembered here too.
     *
     * @throws BookingRejectedException if the booking breaks a rule (for example the nights
     *   were just taken); its message says why.
     */
    suspend fun bookLotteryStay(lotteryId: Id, campsiteId: Id, period: DatePeriod): Reservation {
        val reservation = Database.bookLotteryStay(lotteryId, id, campsiteId, period)
        if (reservation.id !in _reservations) _reservations.add(reservation.id)
        return reservation
    }

    suspend fun removeLottery(lottery: Lottery) {
        if (_lotteries.remove(lottery.id)) {
            Database.updateUser(this)
        }
    }

    suspend fun addReservation(reservation: Reservation) {
        if (reservation.id !in _reservations) {
            _reservations.add(reservation.id)
            Database.updateUser(this)
        }
    }

    suspend fun addChat(chat: Chat) {
        if (chat.id !in _chats) {
            _chats.add(chat.id)
            Database.updateUser(this)
        }
    }

    /**
     suspend fun fetchLotteries(): List<Lottery> = coroutineScope {
     _lotteries.toList().map { async { Database.fetchLottery(it) } }.awaitAll()
     }

     suspend fun fetchReservations(): List<Reservation> = coroutineScope {
     _reservations.toList().map { async { Database.fetchReservation(it) } }.awaitAll()
     }

     suspend fun fetchChats(): List<Chat> = coroutineScope {
     _chats.toList().map { async { Database.fetchChat(it) } }.awaitAll()
     }
     */

    suspend fun deleteProfile() {
        Database.deleteUser(id)
        _lotteries.clear()
        _reservations.clear()
        _chats.clear()
    }
}
