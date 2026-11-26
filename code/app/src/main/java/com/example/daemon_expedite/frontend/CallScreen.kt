package com.example.daemon_expedite.frontend

import android.media.RingtoneManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.voip.CallManager
import com.example.daemon_expedite.voip.CallManager.CallState
import com.example.daemon_expedite.voip.filters.VideoFilter
import com.example.daemon_expedite.voip.rememberCallPermissionRequest
import kotlinx.coroutines.delay
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Full-screen call UI (voice or video), drawn over whatever screen is open while a call is in
 * progress. Shows nothing when there is no call. Placed once, in [AppNavigation], so calls can
 * ring and continue on any screen.
 */
@Composable
fun CallOverlay() {
    val state by CallManager.state.collectAsState()
    val muted by CallManager.muted.collectAsState()
    val speakerOn by CallManager.speakerOn.collectAsState()
    val cameraOn by CallManager.cameraOn.collectAsState()
    val localVideo by CallManager.localVideo.collectAsState()
    val remoteVideo by CallManager.remoteVideo.collectAsState()
    val filter by CallManager.filter.collectAsState()
    val context = LocalContext.current
    val requestPermissions = rememberCallPermissionRequest()

    // Ring while a call is coming in, using the phone's own ringtone.
    val ringing = state is CallState.Incoming
    DisposableEffect(ringing) {
        val ringtone =
            if (ringing) {
                RingtoneManager
                    .getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                    ?.also { it.play() }
            } else {
                null
            }
        onDispose { ringtone?.stop() }
    }

    // Keep the screen on during a call, so a video call doesn't go dark mid-conversation.
    val inCall = state !is CallState.Idle && state !is CallState.Ended
    val view = LocalView.current
    DisposableEffect(inCall) {
        if (inCall) view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    if (state is CallState.Idle) return
    // The screens underneath shouldn't react to Back while a call covers them.
    BackHandler {}

    CallScreenContent(
        state = state,
        muted = muted,
        speakerOn = speakerOn,
        cameraOn = cameraOn,
        localVideo = localVideo,
        remoteVideo = remoteVideo,
        onAccept = {
            val video = (state as? CallState.Incoming)?.video ?: false
            requestPermissions(video) { allowed ->
                if (allowed.mic) {
                    if (video && !allowed.camera) {
                        Toast.makeText(context, "Camera not allowed: they won't see you", Toast.LENGTH_SHORT).show()
                    }
                    CallManager.accept(cameraAllowed = allowed.camera)
                } else {
                    Toast.makeText(context, "Microphone access is needed to answer calls", Toast.LENGTH_SHORT).show()
                    CallManager.decline()
                }
            }
        },
        onDecline = CallManager::decline,
        onHangUp = CallManager::hangUp,
        onToggleMute = CallManager::toggleMute,
        onToggleSpeaker = CallManager::toggleSpeaker,
        onToggleCamera = CallManager::toggleCamera,
        onSwitchCamera = CallManager::switchCamera,
        filter = filter,
        onFilterSelected = CallManager::setFilter,
    )
}

@Composable
fun CallScreenContent(
    state: CallState,
    muted: Boolean,
    speakerOn: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    modifier: Modifier = Modifier,
    cameraOn: Boolean = false,
    localVideo: VideoTrack? = null,
    remoteVideo: VideoTrack? = null,
    onToggleCamera: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    filter: VideoFilter = VideoFilter.NONE,
    onFilterSelected: (VideoFilter) -> Unit = {},
) {
    val name = state.otherName()
    val isVideoCall = state.isVideo()
    // Once a video call is answered, the video layout takes over.
    val showVideo = isVideoCall && (state is CallState.Outgoing || state is CallState.Connecting || state is CallState.Active)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(if (showVideo) Color.Black else AppColors.Background)
                // Swallow taps so nothing underneath can be pressed.
                .clickable(enabled = true, onClick = {}, indication = null, interactionSource = null),
    ) {
        if (showVideo && remoteVideo != null) {
            VideoView(remoteVideo, mirror = false, onTop = false, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = if (showVideo) 24.dp else 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showVideo && remoteVideo != null) {
                // Their face fills the screen: just a small name and timer at the top.
                Text(name, style = AppTextStyles.HeadingMedium, color = Color.White)
                Text(statusText(state), style = AppTextStyles.BodyMedium, color = Color.White.copy(alpha = 0.8f))
            } else {
                Spacer(Modifier.height(48.dp))
                Avatar(name)
                Spacer(Modifier.height(24.dp))
                Text(name, style = AppTextStyles.PageTitle, color = AppColors.TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(statusText(state), style = AppTextStyles.BodyLarge, color = AppColors.TextMuted, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f))

            when {
                state is CallState.Incoming -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        CallButton(Icons.Default.CallEnd, "Decline", AppColors.Danger, onDecline)
                        CallButton(
                            if (state.video) Icons.Default.Videocam else Icons.Default.Call,
                            "Accept",
                            AppColors.Primary,
                            onAccept,
                        )
                    }
                }

                state is CallState.Ended -> {}

                isVideoCall -> {
                    // Filters only make sense while our camera is being sent.
                    if (localVideo != null && cameraOn) {
                        FilterPicker(filter, onFilterSelected)
                        Spacer(Modifier.height(16.dp))
                    }
                    // Five controls: smaller buttons so they fit on narrow phones.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MuteButton(muted, onToggleMute, 56.dp)
                        CallButton(
                            if (cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            if (cameraOn) "Camera" else "Camera off",
                            if (cameraOn) AppColors.Surface else AppColors.Secondary,
                            onToggleCamera,
                            56.dp,
                        )
                        CallButton(Icons.Default.CallEnd, "End", AppColors.Danger, onHangUp, 56.dp)
                        CallButton(Icons.Default.Cameraswitch, "Flip", AppColors.Surface, onSwitchCamera, 56.dp)
                        SpeakerButton(speakerOn, onToggleSpeaker, 56.dp)
                    }
                }

                else -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MuteButton(muted, onToggleMute)
                        CallButton(Icons.Default.CallEnd, "End", AppColors.Danger, onHangUp)
                        SpeakerButton(speakerOn, onToggleSpeaker)
                    }
                }
            }
        }

        // Our own camera, small, in the corner.
        if (showVideo && localVideo != null && cameraOn) {
            LocalPreview(localVideo)
        }
    }
}

/** A swipeable row of filters, like a camera app's lens carousel. */
@Composable
private fun FilterPicker(
    selected: VideoFilter,
    onSelect: (VideoFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(VideoFilter.entries) { f ->
            val isSelected = f == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) AppColors.Accent else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ).clickable { onSelect(f) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(f.badge, fontSize = 22.sp, color = Color.White)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    f.label,
                    style = AppTextStyles.Caption,
                    color = if (isSelected) AppColors.Accent else Color.White,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.LocalPreview(track: VideoTrack) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 72.dp, end = 16.dp)
                .size(width = 108.dp, height = 160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceDeep),
    ) {
        // Mirrored, like a mirror, which is what people expect of their own front camera.
        VideoView(track, mirror = true, onTop = true, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Draws [track] with WebRTC's own view. [onTop] must be true for a view drawn over another
 * video view (the small self-preview), or Android may draw it underneath.
 */
@Composable
private fun VideoView(
    track: VideoTrack,
    mirror: Boolean,
    onTop: Boolean,
    modifier: Modifier = Modifier,
) {
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(CallManager.eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                if (onTop) setZOrderMediaOverlay(true)
                renderer = this
            }
        },
        update = { it.setMirror(mirror) },
        onRelease = { it.release() },
    )
    val view = renderer
    DisposableEffect(track, view) {
        if (view != null) track.addSink(view)
        onDispose {
            // The call may already have disposed the track when it ended.
            if (view != null) runCatching { track.removeSink(view) }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(
        modifier =
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(AppColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = AppTextStyles.PageTitleLarge,
            color = AppColors.TextPrimary,
        )
    }
}

@Composable
private fun MuteButton(
    muted: Boolean,
    onClick: () -> Unit,
    size: Dp = 68.dp,
) = CallButton(
    if (muted) Icons.Default.MicOff else Icons.Default.Mic,
    if (muted) "Unmute" else "Mute",
    if (muted) AppColors.Secondary else AppColors.Surface,
    onClick,
    size,
)

@Composable
private fun SpeakerButton(
    speakerOn: Boolean,
    onClick: () -> Unit,
    size: Dp = 68.dp,
) = CallButton(
    Icons.AutoMirrored.Filled.VolumeUp,
    "Speaker",
    if (speakerOn) AppColors.Secondary else AppColors.Surface,
    onClick,
    size,
)

private fun CallState.otherName(): String =
    when (this) {
        is CallState.Outgoing -> otherName
        is CallState.Incoming -> otherName
        is CallState.Connecting -> otherName
        is CallState.Active -> otherName
        is CallState.Ended -> otherName
        CallState.Idle -> ""
    }

private fun CallState.isVideo(): Boolean =
    when (this) {
        is CallState.Outgoing -> video
        is CallState.Incoming -> video
        is CallState.Connecting -> video
        is CallState.Active -> video
        else -> false
    }

@Composable
private fun statusText(state: CallState): String =
    when (state) {
        is CallState.Outgoing -> "Calling…"
        is CallState.Incoming -> if (state.video) "Incoming video call" else "Incoming voice call"
        is CallState.Connecting -> "Connecting…"
        is CallState.Active -> callTimer(state.startedAtMillis)
        is CallState.Ended -> state.message
        CallState.Idle -> ""
    }

/** "m:ss" since [startedAtMillis], ticking every second. */
@Composable
private fun callTimer(startedAtMillis: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val seconds = ((now - startedAtMillis) / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun CallButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    size: Dp = 68.dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = AppColors.TextOnPrimary, modifier = Modifier.size(size * 0.44f))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = AppTextStyles.Caption, color = AppColors.TextSecondary)
    }
}

// region Previews

@Preview(name = "Incoming video call", showBackground = true, showSystemUi = true)
@Composable
private fun IncomingCallPreview() {
    CallScreenContent(CallState.Incoming("1", "Maya Bennett", video = true), false, false, {}, {}, {}, {}, {})
}

@Preview(name = "Active voice call", showBackground = true, showSystemUi = true)
@Composable
private fun ActiveCallPreview() {
    CallScreenContent(CallState.Active("1", "Maya Bennett", false, System.currentTimeMillis() - 83_000), true, false, {}, {}, {}, {}, {})
}

@Preview(name = "Video call, waiting for video", showBackground = true, showSystemUi = true)
@Composable
private fun VideoCallPreview() {
    CallScreenContent(
        CallState.Connecting("1", "Maya Bennett", video = true),
        muted = false,
        speakerOn = true,
        onAccept = {},
        onDecline = {},
        onHangUp = {},
        onToggleMute = {},
        onToggleSpeaker = {},
        cameraOn = true,
    )
}

// endregion
