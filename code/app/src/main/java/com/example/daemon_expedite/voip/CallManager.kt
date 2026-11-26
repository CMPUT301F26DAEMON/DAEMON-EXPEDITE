package com.example.daemon_expedite.voip

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.daemon_expedite.BuildConfig
import com.example.daemon_expedite.voip.filters.FilterProcessor
import com.example.daemon_expedite.voip.filters.VideoFilter
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs voice and video calls between two users of a chat (an entrant and a campsite owner), one
 * at a time.
 *
 * Audio and video go phone to phone with WebRTC. The phones find each other through Firestore
 * ([CallSignaling]):
 * 1. The caller creates an offer (what audio/video it can send and receive) and writes it to a
 *    new `calls/<id>` document with status "ringing".
 * 2. The callee's phone sees the call (Firestore listener while the app is open, push from the
 *    `notifyIncomingCall` Cloud Function otherwise) and shows [CallState.Incoming].
 * 3. On accept, the callee writes its answer; both phones then trade network addresses (ICE
 *    candidates) until WebRTC connects and the call becomes [CallState.Active].
 *
 * Screens read [state], [muted], [speakerOn], [cameraOn], [localVideo] and [remoteVideo] and call
 * the functions below. Everything here runs on the main thread; WebRTC's callbacks are moved
 * onto it before touching any state.
 *
 * Network note: phones must be able to reach each other directly (home Wi-Fi, a hotspot). Campus
 * Wi-Fi usually blocks that; see [ICE_SERVERS] for adding a TURN relay.
 */
object CallManager {
    private const val TAG = "CallManager"

    /** How long the caller waits for an answer before giving up. */
    private const val RING_TIMEOUT_MS = 45_000L

    /** Ringing calls older than this are ignored (the caller's app probably died mid-call). */
    private const val STALE_RING_MS = 60_000L

    /** How long the "Call ended" screen stays up before going away. */
    private const val ENDED_SCREEN_MS = 1_500L

    // Camera capture: 1080p at 60 fps. The camera picks the closest size and frame rate it
    // supports (some front cameras only do 30 fps at 1080p); the log line "Camera ..." says what
    // it offers. WebRTC scales down by itself when the connection can't carry this.
    private const val VIDEO_WIDTH = 1920
    private const val VIDEO_HEIGHT = 1080
    private const val VIDEO_FPS = 60

    /** Upper limit for our video's bitrate. 1080p60 needs about 6–8 Mbps to look clean. */
    private const val VIDEO_MAX_BITRATE_BPS = 8_000_000

    /** Where WebRTC's bandwidth estimate starts, so the call doesn't open blurry and ramp up. */
    private const val VIDEO_START_BITRATE_BPS = 2_500_000

    // While a filter is on, frames are processed on the phone's CPU, so they are scaled down and
    // capped at 30 fps to keep the call smooth.
    private const val FILTERED_WIDTH = 960
    private const val FILTERED_HEIGHT = 540
    private const val FILTERED_FPS = 30

    /**
     * STUN finds each phone's public address; TURN relays the media when the phones can't reach
     * each other directly (always the case for the Android emulator, which sits behind its own
     * private network). TURN comes from local.properties (`turn.urls`, comma-separated,
     * `turn.username`, `turn.password`), via BuildConfig, so credentials stay out of git.
     */
    private val ICE_SERVERS: List<PeerConnection.IceServer> =
        buildList {
            add(
                PeerConnection.IceServer
                    .builder(listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"))
                    .createIceServer(),
            )
            val turnUrls = BuildConfig.TURN_URLS.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (turnUrls.isNotEmpty()) {
                add(
                    PeerConnection.IceServer
                        .builder(turnUrls)
                        .setUsername(BuildConfig.TURN_USERNAME)
                        .setPassword(BuildConfig.TURN_PASSWORD)
                        .createIceServer(),
                )
            }
        }

    /** What the call screen shows. [video] is true for video calls. */
    sealed interface CallState {
        /** No call. The call screen is hidden. */
        data object Idle : CallState

        /** We are calling [otherName]; their phone is ringing. */
        data class Outgoing(
            val callId: String,
            val otherName: String,
            val video: Boolean,
        ) : CallState

        /** [otherName] is calling us. */
        data class Incoming(
            val callId: String,
            val otherName: String,
            val video: Boolean,
        ) : CallState

        /** The call was answered and the phones are connecting. */
        data class Connecting(
            val callId: String,
            val otherName: String,
            val video: Boolean,
        ) : CallState

        /** Audio (and video) is flowing. [startedAtMillis] is for the call timer. */
        data class Active(
            val callId: String,
            val otherName: String,
            val video: Boolean,
            val startedAtMillis: Long,
        ) : CallState

        /** The call just ended; [message] says why. Shown briefly, then back to [Idle]. */
        data class Ended(
            val otherName: String,
            val message: String,
        ) : CallState
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _speakerOn = MutableStateFlow(false)
    val speakerOn: StateFlow<Boolean> = _speakerOn.asStateFlow()

    private val _cameraOn = MutableStateFlow(false)

    /** Whether our camera is being sent. Only meaningful on video calls. */
    val cameraOn: StateFlow<Boolean> = _cameraOn.asStateFlow()

    private val _localVideo = MutableStateFlow<VideoTrack?>(null)

    /** Our own camera, for the small preview. Null when not sending video. */
    val localVideo: StateFlow<VideoTrack?> = _localVideo.asStateFlow()

    private val _filter = MutableStateFlow(VideoFilter.NONE)

    /** The filter on our camera. The other person sees it too. */
    val filter: StateFlow<VideoFilter> = _filter.asStateFlow()

    private val _remoteVideo = MutableStateFlow<VideoTrack?>(null)

    /** The other person's camera. Null until their video arrives. */
    val remoteVideo: StateFlow<VideoTrack?> = _remoteVideo.asStateFlow()

    private lateinit var appContext: Context
    private var factory: PeerConnectionFactory? = null
    private var eglBase: EglBase? = null

    /** The OpenGL context video views must be set up with (see CallScreen's VideoView). */
    val eglContext: EglBase.Context get() = egl().eglBaseContext

    // The current call. All null / empty when there is none.
    private var callId: String? = null
    private var otherName: String = ""
    private var isCaller = false
    private var isVideo = false
    private var peer: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var textureHelper: SurfaceTextureHelper? = null
    private var filterProcessor: FilterProcessor? = null
    private val callListeners = mutableListOf<ListenerRegistration>()
    private var ringTimeout: Job? = null

    /**
     * Candidates from the other phone that arrived before its offer/answer was applied. WebRTC
     * rejects candidates until then, so they wait here.
     */
    private var remoteDescriptionSet = false
    private val pendingCandidates = mutableListOf<IceCandidate>()

    private var incomingListener: ListenerRegistration? = null
    private var watchedUserId: String? = null

    /** Called once from ExpediteApp. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val isBusy get() = _state.value !is CallState.Idle && _state.value !is CallState.Ended

    // region Public actions

    /**
     * Starts listening for calls to [userId] while the app runs. Safe to call repeatedly; only a
     * change of user restarts the listener.
     */
    fun watchIncoming(userId: String) {
        if (watchedUserId == userId) return
        incomingListener?.remove()
        watchedUserId = userId
        incomingListener = CallSignaling.listenForIncoming(userId, ::onIncomingCall)
    }

    /**
     * Calls [otherId]. Needs RECORD_AUDIO, and CAMERA for [video] (without it the call still
     * goes ahead, receiving their video but not sending ours).
     *
     * @param myName and [otherNameArg] are shown on each side's call screen and in the push.
     */
    fun startCall(
        myId: String,
        myName: String,
        otherId: String,
        otherNameArg: String,
        video: Boolean,
        cameraAllowed: Boolean,
    ) {
        if (isBusy) return
        val id = UUID.randomUUID().toString()
        beginCall(id, otherNameArg, caller = true, video = video)
        _state.value = CallState.Outgoing(id, otherNameArg, video)

        scope.launch {
            try {
                val pc = createPeer(id, sendCamera = video && cameraAllowed)
                val offer = pc.createSdp(offer = true)
                pc.setLocal(offer)
                CallSignaling.createCall(id, myId, myName, otherId, otherNameArg, video, offer)
                if (callId != id) return@launch // hung up while writing
                callListeners += CallSignaling.listenToCall(id, ::onCallChanged)
                callListeners += CallSignaling.listenToCandidates(id, fromCaller = false, ::addRemoteCandidate)
                ringTimeout =
                    scope.launch {
                        delay(RING_TIMEOUT_MS)
                        if (callId == id && _state.value is CallState.Outgoing) {
                            finish("No answer")
                            updateStatus(id, CallSignaling.STATUS_MISSED)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Starting call failed", e)
                if (callId == id) {
                    finish("Call failed")
                    updateStatus(id, CallSignaling.STATUS_ENDED)
                }
            }
        }
    }

    /**
     * Answers the ringing call. Needs RECORD_AUDIO; on a video call our camera is sent only if
     * [cameraAllowed].
     */
    fun accept(cameraAllowed: Boolean) {
        val incoming = _state.value as? CallState.Incoming ?: return
        val id = incoming.callId
        _state.value = CallState.Connecting(id, incoming.otherName, incoming.video)

        scope.launch {
            try {
                val offer = CallSignaling.fetchCall(id)?.offer ?: error("Call $id has no offer")
                val pc = createPeer(id, sendCamera = incoming.video && cameraAllowed)
                pc.setRemote(offer)
                onRemoteDescriptionSet()
                val answer = pc.createSdp(offer = false)
                pc.setLocal(answer)
                CallSignaling.answerCall(id, answer)
                if (callId != id) return@launch
                callListeners += CallSignaling.listenToCandidates(id, fromCaller = true, ::addRemoteCandidate)
            } catch (e: Exception) {
                Log.e(TAG, "Answering call failed", e)
                if (callId == id) {
                    finish("Call failed")
                    updateStatus(id, CallSignaling.STATUS_ENDED)
                }
            }
        }
    }

    /** Turns down the ringing call. */
    fun decline() {
        val incoming = _state.value as? CallState.Incoming ?: return
        finish(null)
        updateStatus(incoming.callId, CallSignaling.STATUS_DECLINED)
    }

    /** Hangs up (or cancels an outgoing call that hasn't been answered). */
    fun hangUp() {
        val id = callId ?: return
        if (_state.value is CallState.Incoming) return decline()
        finish("Call ended")
        updateStatus(id, CallSignaling.STATUS_ENDED)
    }

    fun toggleMute() {
        _muted.value = !_muted.value
        audioTrack?.setEnabled(!_muted.value)
    }

    fun toggleSpeaker() {
        _speakerOn.value = !_speakerOn.value
        routeToSpeaker(_speakerOn.value)
    }

    /** Stops or resumes sending our camera. The other side sees a frozen/black picture meanwhile. */
    fun toggleCamera() {
        val track = videoTrack ?: return
        val on = !_cameraOn.value
        _cameraOn.value = on
        track.setEnabled(on)
        try {
            if (on) videoCapturer?.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS) else videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't ${if (on) "start" else "stop"} the camera", e)
        }
    }

    /** Puts [newFilter] on our camera (see [VideoFilter]). */
    fun setFilter(newFilter: VideoFilter) {
        _filter.value = newFilter
        filterProcessor?.filter = newFilter
        if (newFilter == VideoFilter.NONE) {
            videoSource?.adaptOutputFormat(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
        } else {
            videoSource?.adaptOutputFormat(FILTERED_WIDTH, FILTERED_HEIGHT, FILTERED_FPS)
        }
    }

    /** Swaps between the front and back camera. */
    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    // endregion

    // region Firestore events

    private fun onIncomingCall(call: CallSignaling.CallDoc) {
        val created = call.createdAtMillis
        if (created != null && System.currentTimeMillis() - created > STALE_RING_MS) return
        if (call.id == callId) return
        if (isBusy) {
            // Already on a call: tell the caller we can't pick up.
            updateStatus(call.id, CallSignaling.STATUS_DECLINED)
            return
        }
        val name = call.callerName.ifBlank { "Someone" }
        beginCall(call.id, name, caller = false, video = call.video)
        _state.value = CallState.Incoming(call.id, name, call.video)
        // Notices the caller hanging up before we answer, or hanging up later.
        callListeners += CallSignaling.listenToCall(call.id, ::onCallChanged)
    }

    private fun onCallChanged(call: CallSignaling.CallDoc) {
        if (call.id != callId) return
        when (call.status) {
            CallSignaling.STATUS_ACCEPTED -> {
                val answer = call.answer
                if (isCaller && !remoteDescriptionSet && answer != null) {
                    ringTimeout?.cancel()
                    _state.value = CallState.Connecting(call.id, otherName, isVideo)
                    val pc = peer ?: return
                    scope.launch {
                        try {
                            pc.setRemote(answer)
                            onRemoteDescriptionSet()
                        } catch (e: Exception) {
                            Log.e(TAG, "Applying answer failed", e)
                            hangUp()
                        }
                    }
                }
            }

            CallSignaling.STATUS_DECLINED -> {
                finish(if (isCaller) "$otherName is unavailable" else "Call ended")
            }

            CallSignaling.STATUS_MISSED -> {
                finish(if (isCaller) "No answer" else "Missed call")
            }

            CallSignaling.STATUS_ENDED -> {
                finish("Call ended")
            }
        }
    }

    private fun addRemoteCandidate(candidate: IceCandidate) {
        Log.d(TAG, "Remote candidate (${candidateType(candidate)}), applied now: $remoteDescriptionSet")
        if (remoteDescriptionSet) peer?.addIceCandidate(candidate) else pendingCandidates += candidate
    }

    private fun onRemoteDescriptionSet() {
        remoteDescriptionSet = true
        pendingCandidates.forEach { peer?.addIceCandidate(it) }
        pendingCandidates.clear()
    }

    private fun updateStatus(
        id: String,
        status: String,
    ) {
        scope.launch {
            try {
                CallSignaling.setStatus(id, status)
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't mark call $id $status", e)
            }
        }
    }

    // endregion

    // region WebRTC

    private fun egl(): EglBase = eglBase ?: EglBase.create().also { eglBase = it }

    private fun factory(): PeerConnectionFactory =
        factory ?: run {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext).createInitializationOptions(),
            )
            val audioModule =
                JavaAudioDeviceModule
                    .builder(appContext)
                    .setUseHardwareAcousticEchoCanceler(true)
                    .setUseHardwareNoiseSuppressor(true)
                    .createAudioDeviceModule()
            PeerConnectionFactory
                .builder()
                .setAudioDeviceModule(audioModule)
                // Hardware video codecs where the phone has them, software otherwise.
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl().eglBaseContext, true, true))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl().eglBaseContext))
                .createPeerConnectionFactory()
                .also { factory = it }
        }

    /**
     * Creates the connection for call [id] with our microphone attached, and our camera too if
     * [sendCamera]. On a video call without our camera, a receive-only video slot is added so
     * the other side's video still arrives.
     */
    private fun createPeer(
        id: String,
        sendCamera: Boolean,
    ): PeerConnection {
        val config =
            PeerConnection.RTCConfiguration(ICE_SERVERS).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }
        val pc = factory().createPeerConnection(config, PeerObserver(id)) ?: error("Couldn't create a WebRTC connection")
        peer = pc

        val source = factory().createAudioSource(MediaConstraints())
        val track = factory().createAudioTrack("audio0", source)
        track.setEnabled(!_muted.value)
        pc.addTrack(track, listOf("stream0"))
        audioSource = source
        audioTrack = track

        if (isVideo) {
            val camera = if (sendCamera) startCamera() else null
            if (camera != null) {
                tuneVideoSender(pc.addTrack(camera, listOf("stream0")))
            } else {
                pc.addTransceiver(
                    org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
                    RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY),
                )
            }
            preferH264(pc)
        }

        startCallAudio()
        return pc
    }

    /**
     * Lets our video go up to [VIDEO_FPS] and [VIDEO_MAX_BITRATE_BPS]. When the connection can't
     * carry that, WebRTC lowers the resolution first and keeps the frame rate, so motion stays
     * smooth (its default would drop frames instead).
     */
    private fun tuneVideoSender(sender: org.webrtc.RtpSender) {
        val params = sender.parameters
        params.degradationPreference = org.webrtc.RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
        params.encodings.forEach {
            it.maxFramerate = VIDEO_FPS
            it.maxBitrateBps = VIDEO_MAX_BITRATE_BPS
        }
        if (!sender.setParameters(params)) Log.w(TAG, "Couldn't raise video frame rate/bitrate limits")
        // Start the bandwidth estimate high enough for HD instead of WebRTC's cautious default.
        peer?.setBitrate(null, VIDEO_START_BITRATE_BPS, VIDEO_MAX_BITRATE_BPS)
    }

    /**
     * Puts H.264 first in the video codec list, on both phones before the offer/answer is made.
     * Phones encode H.264 in hardware; WebRTC's default, VP8, is often done in software, which
     * can't keep up with 1080p60. The other codecs stay as fallbacks.
     */
    private fun preferH264(pc: PeerConnection) {
        val codecs =
            factory()
                .getRtpReceiverCapabilities(org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO)
                .codecs
        if (codecs.none { it.name.equals("H264", ignoreCase = true) }) {
            Log.w(TAG, "No H.264 on this phone; using the default codec")
            return
        }
        val ordered = codecs.sortedBy { if (it.name.equals("H264", ignoreCase = true)) 0 else 1 }
        pc.transceivers
            .filter { it.mediaType == org.webrtc.MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO }
            .forEach {
                try {
                    it.setCodecPreferences(ordered)
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't prefer H.264", e)
                }
            }
    }

    /** Opens the front camera (or any camera) and returns its track, or null if there is none. */
    private fun startCamera(): VideoTrack? {
        val enumerator = Camera2Enumerator(appContext)
        val names = enumerator.deviceNames
        val name = names.firstOrNull { enumerator.isFrontFacing(it) } ?: names.firstOrNull()
        if (name == null) {
            Log.w(TAG, "No camera found; sending audio only")
            return null
        }
        // Camera2 reports frame rates in thousandths of a frame per second.
        val maxFps =
            enumerator
                .getSupportedFormats(name)
                ?.maxOfOrNull { it.framerate.max }
                ?.let { if (it > 1000) it / 1000 else it }
        val formats = enumerator.getSupportedFormats(name).orEmpty()
        val best1080 =
            formats
                .filter { it.width >= VIDEO_WIDTH && it.height >= VIDEO_HEIGHT || it.width >= VIDEO_HEIGHT && it.height >= VIDEO_WIDTH }
                .maxOfOrNull { it.framerate.max }
                ?.let { if (it > 1000) it / 1000 else it }
        Log.i(
            TAG,
            "Camera $name offers up to $maxFps fps overall, ${best1080 ?: "no"} fps at 1080p; " +
                "asking for ${VIDEO_WIDTH}x$VIDEO_HEIGHT@$VIDEO_FPS",
        )
        val capturer = enumerator.createCapturer(name, null) ?: return null
        val helper = SurfaceTextureHelper.create("CameraCapture", egl().eglBaseContext)
        val source = factory().createVideoSource(false)
        val processor = FilterProcessor()
        source.setVideoProcessor(processor)
        capturer.initialize(helper, appContext, source.capturerObserver)
        capturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
        val track = factory().createVideoTrack("video0", source)

        videoCapturer = capturer
        textureHelper = helper
        videoSource = source
        videoTrack = track
        filterProcessor = processor
        setFilter(VideoFilter.NONE)
        _cameraOn.value = true
        _localVideo.value = track
        return track
    }

    /** Reports connection changes for call [id]; ignores them once that call is over. */
    private class PeerObserver(
        private val id: String,
    ) : PeerConnection.Observer {
        private fun onMain(block: () -> Unit) {
            scope.launch { if (callId == id) block() }
        }

        override fun onIceCandidate(candidate: IceCandidate) {
            Log.d(TAG, "Local candidate (${candidateType(candidate)})")
            val fromCaller = isCaller
            scope.launch {
                try {
                    CallSignaling.addCandidate(id, fromCaller, candidate)
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't send ICE candidate", e)
                }
            }
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver.track() as? VideoTrack ?: return // audio plays by itself
            onMain { _remoteVideo.value = track }
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) =
            onMain {
                Log.i(TAG, "Connection $newState")
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        if (_state.value !is CallState.Active) {
                            _state.value = CallState.Active(id, otherName, isVideo, System.currentTimeMillis())
                        }
                    }

                    PeerConnection.PeerConnectionState.FAILED -> {
                        Log.w(
                            TAG,
                            "Call failed to connect. TURN configured: ${ICE_SERVERS.size > 1}. " +
                                "Without TURN, emulators and many mobile/campus networks can't connect.",
                        )
                        finish("Couldn't connect")
                        updateStatus(id, CallSignaling.STATUS_ENDED)
                    }

                    else -> {} // DISCONNECTED often recovers by itself; FAILED follows if it doesn't.
                }
            }

        override fun onSignalingChange(state: PeerConnection.SignalingState) {}

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            Log.d(TAG, "ICE $state")
        }

        override fun onIceCandidateError(event: IceCandidateErrorEvent) {
            Log.w(TAG, "ICE server error ${event.errorCode} ${event.errorText} (${event.url})")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {}

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
            Log.d(TAG, "ICE gathering $state")
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}

        override fun onAddStream(stream: MediaStream) {}

        override fun onRemoveStream(stream: MediaStream) {}

        override fun onDataChannel(channel: DataChannel) {}

        override fun onRenegotiationNeeded() {}
    }

    /** "host" (own address), "srflx" (public address via STUN) or "relay" (via TURN). */
    private fun candidateType(candidate: IceCandidate): String = candidate.sdp.substringAfter(" typ ", "?").substringBefore(' ')

    /** An [SdpObserver] that does nothing, for overriding just the callbacks needed. */
    private open class SdpCallbacks : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {}

        override fun onSetSuccess() {}

        override fun onCreateFailure(error: String?) {}

        override fun onSetFailure(error: String?) {}
    }

    private suspend fun PeerConnection.createSdp(offer: Boolean): SessionDescription =
        suspendCancellableCoroutine { cont ->
            val callbacks =
                object : SdpCallbacks() {
                    override fun onCreateSuccess(sdp: SessionDescription) = cont.resume(sdp)

                    override fun onCreateFailure(error: String?) = cont.resumeWithException(IllegalStateException(error))
                }
            val constraints =
                MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideo.toString()))
                }
            if (offer) createOffer(callbacks, constraints) else createAnswer(callbacks, constraints)
        }

    private suspend fun PeerConnection.setLocal(sdp: SessionDescription) = setSdp(sdp, local = true)

    private suspend fun PeerConnection.setRemote(sdp: SessionDescription) = setSdp(sdp, local = false)

    private suspend fun PeerConnection.setSdp(
        sdp: SessionDescription,
        local: Boolean,
    ): Unit =
        suspendCancellableCoroutine { cont ->
            val callbacks =
                object : SdpCallbacks() {
                    override fun onSetSuccess() = cont.resume(Unit)

                    override fun onSetFailure(error: String?) = cont.resumeWithException(IllegalStateException(error))
                }
            if (local) setLocalDescription(callbacks, sdp) else setRemoteDescription(callbacks, sdp)
        }

    // endregion

    // region Call lifecycle and audio routing

    private fun beginCall(
        id: String,
        name: String,
        caller: Boolean,
        video: Boolean,
    ) {
        callId = id
        otherName = name
        isCaller = caller
        isVideo = video
        remoteDescriptionSet = false
        pendingCandidates.clear()
        _muted.value = false
        // Video calls are held away from the ear, so they start on the loudspeaker.
        _speakerOn.value = video
        _cameraOn.value = false
    }

    /**
     * Tears the current call down and shows [message] briefly (or nothing, if null). Doesn't
     * write to Firestore; callers that need to tell the other phone call [updateStatus] too.
     */
    private fun finish(message: String?) {
        if (callId == null) return
        ringTimeout?.cancel()
        ringTimeout = null
        callListeners.forEach { it.remove() }
        callListeners.clear()

        // Views stop drawing the tracks before they are disposed.
        _localVideo.value = null
        _remoteVideo.value = null
        _cameraOn.value = false

        try {
            videoCapturer?.stopCapture()
        } catch (e: InterruptedException) {
            Log.w(TAG, "Stopping the camera was interrupted", e)
        }
        videoCapturer?.dispose()
        videoCapturer = null
        videoSource?.setVideoProcessor(null)
        filterProcessor?.release()
        filterProcessor = null
        _filter.value = VideoFilter.NONE
        peer?.dispose() // also closes it, and disposes the remote tracks
        peer = null
        videoTrack?.dispose()
        videoTrack = null
        videoSource?.dispose()
        videoSource = null
        textureHelper?.dispose()
        textureHelper = null
        audioTrack?.dispose()
        audioTrack = null
        audioSource?.dispose()
        audioSource = null
        pendingCandidates.clear()
        remoteDescriptionSet = false
        stopCallAudio()

        val name = otherName
        callId = null
        isVideo = false
        if (message == null) {
            _state.value = CallState.Idle
        } else {
            val ended = CallState.Ended(name, message)
            _state.value = ended
            scope.launch {
                delay(ENDED_SCREEN_MS)
                if (_state.value === ended) _state.value = CallState.Idle
            }
        }
    }

    private val audioManager get() = appContext.getSystemService(AudioManager::class.java)

    private fun startCallAudio() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        routeToSpeaker(_speakerOn.value)
    }

    private fun stopCallAudio() {
        routeToSpeaker(false)
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun routeToSpeaker(on: Boolean) {
        val am = audioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (on) {
                am.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    ?.let { am.setCommunicationDevice(it) }
            } else {
                am.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = on
        }
    }

    // endregion
}
