package com.example.daemon_expedite.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * True if the app may show notifications.
 *
 * Before Android 13 (API 33) there was no permission to ask for: notifications were allowed
 * unless the user turned them off in Settings, so this is always true there.
 */
fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Returns a function that asks the user to allow notifications, then calls `onResult` with
 * whether they're allowed.
 *
 * If they're already allowed, `onResult(true)` is called straight away without showing
 * anything. Otherwise Android's "Allow DAEMON-EXPEDITE to send you notifications?" dialog is
 * shown. Android only shows that dialog twice: after the user refuses twice, it answers "no"
 * immediately, and only the phone's Settings can turn notifications back on.
 *
 * Must be called unconditionally during composition (it registers with the activity), like any
 * `remember` function.
 */
@Composable
fun rememberNotificationPermissionRequest(): (onResult: (granted: Boolean) -> Unit) -> Unit {
    val context = LocalContext.current
    // The callback of the request in progress. A plain holder, not Compose state: changing it
    // doesn't need to redraw anything.
    val pending = remember { PendingResult() }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending.callback?.invoke(granted)
            pending.callback = null
        }
    return remember(context, launcher) {
        { onResult ->
            if (hasNotificationPermission(context)) {
                onResult(true)
            } else {
                pending.callback = onResult
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private class PendingResult {
    var callback: ((Boolean) -> Unit)? = null
}
