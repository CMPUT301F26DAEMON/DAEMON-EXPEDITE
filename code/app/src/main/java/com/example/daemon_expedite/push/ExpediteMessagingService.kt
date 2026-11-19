package com.example.daemon_expedite.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.daemon_expedite.MainActivity
import com.example.daemon_expedite.R
import com.example.daemon_expedite.backend.Database
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives push messages from Firebase Cloud Messaging (FCM).
 *
 * Registered in AndroidManifest.xml. Android starts it when a push arrives, even if the app
 * isn't running.
 *
 * Which code shows a push depends on whether the app is open:
 * - App in the background or closed: if the push has a `notification` part, the system shows
 *   it itself, using the channel and icon set in the manifest. [onMessageReceived] isn't called.
 * - App in the foreground: Android doesn't show anything. [onMessageReceived] is called and
 *   shows the notification itself, so the user still sees the card.
 */
class ExpediteMessagingService : FirebaseMessagingService() {

    /**
     * Called when this install gets a new FCM token: on first launch, after reinstalling or
     * clearing app data, and occasionally when Firebase rotates it. The token is this device's
     * address; the Cloud Functions need it to send a push here, so it is saved on the user.
     *
     * Firebase calls this on a background thread and may stop the service as soon as it
     * returns, so the save is waited for with [runBlocking] (safe here: not the main thread)
     * instead of being launched and forgotten. The timeout keeps a slow connection from holding
     * the service; if the save doesn't happen, SessionViewModel saves the token at next start-up.
     *
     * The token is also logged, for sending test pushes from the Firebase console
     * (Messaging > Send test message).
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        runBlocking {
            try {
                val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) { Database.saveOwnFcmToken(token) }
                if (saved != true) Log.i(TAG, "Token not saved yet; it will be at next start-up")
            } catch (e: Exception) {
                Log.w(TAG, "Saving FCM token failed; it will be retried at next start-up", e)
            }
        }
    }

    /** Called for every push while the app is in the foreground, and for data-only pushes. */
    override fun onMessageReceived(message: RemoteMessage) {
        // A push can carry its text in its `notification` part, its `data` part, or both.
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"] ?: return
        show(message.messageId.hashCode(), title, body)
    }

    /**
     * Shows a notification in [NotificationChannels.LOTTERY_UPDATES]. [id] identifies it:
     * showing another with the same id replaces it, so each push gets its own.
     */
    private fun show(id: Int, title: String?, body: String) {
        // From Android 13 the user must allow notifications. Without that, notify() does nothing,
        // so skip it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // What happens when the notification is tapped: open the app. FLAG_IMMUTABLE is required
        // from Android 12 and stops other apps from changing the intent.
        val openApp =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(this, NotificationChannels.LOTTERY_UPDATES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                // Long messages expand instead of being cut off after one line.
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(openApp)
                .setAutoCancel(true) // dismiss it when tapped
                .build()

        NotificationManagerCompat.from(this).notify(id, notification)
    }

    private companion object {
        const val TAG = "ExpediteMessaging"
        const val SAVE_TIMEOUT_MS = 10_000L
    }
}
