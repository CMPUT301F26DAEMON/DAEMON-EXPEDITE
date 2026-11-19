package com.example.daemon_expedite.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.daemon_expedite.R

/**
 * The app's notification channel.
 *
 * Every Android notification belongs to a channel. The channel (not the notification) decides
 * how loud it is, and users can turn it on or off in system settings. The app has one channel,
 * [LOTTERY_UPDATES], for both kinds of notification: lottery results and messages from
 * campsite owners.
 *
 * It is [NotificationManager.IMPORTANCE_HIGH], which is what makes a notification pop up as a
 * card over whatever the user is doing (a "heads-up" notification).
 */
object NotificationChannels {
    /**
     * Lottery results ("You won" / "You weren't selected") and campsite owner messages.
     * Also set as FCM's default channel in AndroidManifest.xml; keep the two in sync.
     */
    const val LOTTERY_UPDATES = "lottery_updates"

    /**
     * Creates the channel. Safe to call on every app start: creating a channel that already
     * exists only updates its name and description, never the importance the user chose.
     */
    fun create(context: Context) {
        val channel =
            NotificationChannel(
                LOTTERY_UPDATES,
                context.getString(R.string.channel_lottery_updates_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_lottery_updates_description)
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
