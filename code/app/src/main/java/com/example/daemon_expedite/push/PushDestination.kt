package com.example.daemon_expedite.push

import android.content.Intent

/**
 * The screen a tapped push notification opens.
 *
 * The Cloud Functions (functions/messages.js) put a `type` in every push's data. Tapping the
 * notification starts MainActivity with that data as intent extras, whether Android showed the
 * notification itself (app in the background) or [ExpediteMessagingService] did (app open).
 */
enum class PushDestination {
    /** A lottery result ("You won" / "You weren't selected"): the entrant's Lotteries tab. */
    LOTTERIES,

    /**
     * A message a campsite owner sent to their waiting list, or the confirmation of a stay the
     * entrant just booked: the entrant's Notifications tab.
     */
    NOTIFICATIONS,
    ;

    companion object {
        /** The intent extra holding the push's type. Matches the `type` key in messages.js. */
        const val EXTRA_TYPE = "type"

        /** Where [intent] should go, or null if it didn't come from a known push. */
        fun from(intent: Intent?): PushDestination? =
            when (intent?.getStringExtra(EXTRA_TYPE)) {
                "lottery_result" -> LOTTERIES
                "owner_message", "reservation_confirmed" -> NOTIFICATIONS
                else -> null
            }
    }
}
