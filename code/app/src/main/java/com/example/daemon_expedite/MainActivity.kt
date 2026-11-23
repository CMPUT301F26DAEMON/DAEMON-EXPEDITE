package com.example.daemon_expedite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.daemon_expedite.frontend.AppNavigation
import com.example.daemon_expedite.push.PushDestination
import com.example.daemon_expedite.ui.theme.DAEMONEXPEDITETheme

class MainActivity : ComponentActivity() {
    /** The screen a tapped push notification asked for, until AppNavigation has opened it. */
    private var pushDestination by mutableStateOf<PushDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after a rotation the activity gets the same intent again, and
        // the push has already been handled.
        if (savedInstanceState == null) pushDestination = PushDestination.from(intent)
        enableEdgeToEdge()
        setContent {
            DAEMONEXPEDITETheme {
                AppNavigation(
                    pushDestination = pushDestination,
                    onPushDestinationOpened = { pushDestination = null },
                )
            }
        }
    }

    /** A notification tapped while the app is already open (see ExpediteMessagingService). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushDestination.from(intent)?.let { pushDestination = it }
    }
}
