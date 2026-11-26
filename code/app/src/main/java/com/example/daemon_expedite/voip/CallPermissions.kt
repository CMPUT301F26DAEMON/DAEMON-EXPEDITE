package com.example.daemon_expedite.voip

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** What the user allowed for a call. The microphone is required; the camera is optional. */
data class CallPermissions(
    val mic: Boolean,
    val camera: Boolean,
)

/**
 * Returns a function that makes sure the app may use the microphone (and the camera, for a video
 * call), then calls `onResult` with what is allowed. Asks the user only for what is missing.
 * Calls need these on both phones: the caller before calling, the callee before answering.
 *
 * Must be called unconditionally during composition, like any `remember` function.
 */
@Composable
fun rememberCallPermissionRequest(): (video: Boolean, onResult: (CallPermissions) -> Unit) -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            pending[0]?.invoke()
            pending[0] = null
        }
    return remember(context, launcher) {
        { video, onResult ->
            val wanted =
                buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (video) add(Manifest.permission.CAMERA)
                }
            val report = { onResult(CallPermissions(context.has(Manifest.permission.RECORD_AUDIO), video && context.has(Manifest.permission.CAMERA))) }
            val missing = wanted.filterNot { context.has(it) }
            if (missing.isEmpty()) {
                report()
            } else {
                pending[0] = report
                launcher.launch(missing.toTypedArray())
            }
        }
    }
}

private fun Context.has(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
