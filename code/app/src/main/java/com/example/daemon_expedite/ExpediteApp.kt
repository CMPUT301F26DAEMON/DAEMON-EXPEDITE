package com.example.daemon_expedite

import android.app.Application
import com.example.daemon_expedite.push.NotificationChannels

/**
 * Runs once when the app's process starts, before any activity or service.
 *
 * Registered with `android:name` on `<application>` in AndroidManifest.xml. The notification
 * channels are created here (rather than in MainActivity) because a push can start the app's
 * process with no activity at all, and a notification can't be shown in a channel that
 * doesn't exist yet.
 */
class ExpediteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }
}
