package com.example.daemon_expedite.backend

import java.time.format.DateTimeFormatter
import java.util.Locale

private val CONFIRMATION_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

/**
 * The message of a reservation confirmation, for example
 * "Your stay at Pine Lake is booked: 2 nights, Oct 16, 2026 to check-out Oct 18, 2026."
 *
 * @param campsiteName The campsite's name, or null if it has none.
 */
fun reservationConfirmedText(campsiteName: String?, period: DatePeriod): String {
    val place = campsiteName?.trim()?.takeIf { it.isNotEmpty() } ?: "your campsite"
    val nights = if (period.nights == 1) "1 night" else "${period.nights} nights"
    return "Your stay at $place is booked: $nights, " +
        "${period.start.format(CONFIRMATION_DATE)} to check-out ${period.end.format(CONFIRMATION_DATE)}."
}

/**
 * The message of a cancellation confirmation, for example
 * "Your stay at Pine Lake for 2 nights, Oct 16, 2026 to check-out Oct 18, 2026, is cancelled."
 *
 * @param campsiteName The campsite's name, or null if it has none.
 */
fun reservationCancelledText(campsiteName: String?, period: DatePeriod): String {
    val place = campsiteName?.trim()?.takeIf { it.isNotEmpty() } ?: "your campsite"
    val nights = if (period.nights == 1) "1 night" else "${period.nights} nights"
    return "Your stay at $place for $nights, " +
        "${period.start.format(CONFIRMATION_DATE)} to check-out ${period.end.format(CONFIRMATION_DATE)}, is cancelled."
}

/**
 * What a [Notification] is about. Stored as the `kind` field of `notifications/<id>`;
 * documents written before the field existed have none and are [OWNER_MESSAGE]s.
 *
 * @property stored The value saved in Firestore. Also the push's `type` (see functions/messages.js).
 */
enum class NotificationKind(val stored: String) {
    /** A message an owner sent to everyone on one of their lotteries' waiting lists. */
    OWNER_MESSAGE("owner_message"),

    /** Sent to an entrant when they book their stay (see [Database.bookLotteryStay]). */
    RESERVATION_CONFIRMED("reservation_confirmed"),

    /** Sent to an entrant when they cancel their stay (see [Database.cancelReservation]). */
    RESERVATION_CANCELLED("reservation_cancelled"),

    /**
     * A lottery's draw results, saved by the `notifyLotteryResults` Cloud Function: one for the
     * winners and one for everyone else on the waiting list. Its sender is the lottery's owner.
     */
    LOTTERY_RESULT("lottery_result"),
    ;

    companion object {
        /** The kind saved as [value]; missing or unknown values are [OWNER_MESSAGE]. */
        fun fromStored(value: String?): NotificationKind = entries.firstOrNull { it.stored == value } ?: OWNER_MESSAGE
    }
}

/**
 * A notification shown in a user's Notifications tab.
 *
 * Usually a message an owner sends to everyone on one of their lotteries' waiting lists
 * ([NotificationKind.OWNER_MESSAGE]). It can also confirm an entrant's reservation
 * ([NotificationKind.RESERVATION_CONFIRMED]); then it goes to that entrant only, its sender is
 * the campsite's owner and [reservationId] is the reservation. A cancellation
 * ([NotificationKind.RESERVATION_CANCELLED]) works the same way.
 *
 * One notification is stored once (in `notifications/<id>`) and its id is added to each
 * recipient's `receivedNotifications`. Owner messages are also added to the sender's
 * `ownerNotifications`. When it was sent comes from the id itself: see [Id.getTime].
 *
 * @property id The notification's id. Also gives the time it was sent.
 * @property text The message. Required.
 * @property title An optional short heading shown above the message.
 * @property receiversOfLottery The lottery whose waiting list it was sent to, or the lottery the
 *   confirmed reservation was won in.
 * @property senderId The owner who sent it, or the campsite's owner for a confirmation.
 * @property kind What it is about.
 * @property reservationId The confirmed reservation, for [NotificationKind.RESERVATION_CONFIRMED].
 * @property recipientIds Who it went to, when the notification records that: the entrant for a
 *   confirmation, or the winners (or everyone else) for a [NotificationKind.LOTTERY_RESULT].
 *   Empty for owner messages, which go to whoever was on the waiting list at the time.
 * @param isRead Whether the user *viewing* it has read it. Read state is per recipient (the
 *   database keeps a `readBy` list), so this is filled in for whoever loaded the notification.
 */
class Notification(
    val id: Id,
    val text: String,
    val receiversOfLottery: Id, // Lottery Id
    val senderId: Id,
    isRead: Boolean = false,
    val title: String? = null,
    val kind: NotificationKind = NotificationKind.OWNER_MESSAGE,
    val reservationId: Id? = null,
    val recipientIds: List<Id> = emptyList(),
) {
    var isRead: Boolean = isRead
        private set

    fun read(){
        this.isRead = true
    }
}
