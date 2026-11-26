package com.example.daemon_expedite.voip.filters

import android.graphics.PointF
import android.os.SystemClock
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import org.webrtc.VideoFrame
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Where the caller's face is, for drawing face filters.
 *
 * All positions are in pixels of the upright frame. [angle] is the head's tilt in radians (0 =
 * level); [up] points from the eyes towards the top of the head, along that tilt.
 */
internal data class TrackedFace(
    val eyeCenter: PointF,
    val leftEye: PointF,
    val rightEye: PointF,
    val nose: PointF,
    val mouth: PointF,
    val eyeDistance: Float,
    val faceWidth: Float,
    val faceHeight: Float,
    val angle: Float,
) {
    val up: PointF get() = PointF(kotlin.math.sin(angle), -kotlin.math.cos(angle))
}

/**
 * Finds the caller's face in camera frames with ML Kit, on the phone (the model ships inside the
 * app; nothing is sent anywhere).
 *
 * Detection is slower than the camera, so it runs on its own thread on about 12 frames a second,
 * one at a time, while every frame draws the most recent result. Results are smoothed so the
 * stickers glide instead of jittering.
 */
internal class FaceTracker {
    private val detector =
        FaceDetection.getClient(
            FaceDetectorOptions
                .Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build(),
        )
    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private var lastSubmitAt = 0L

    @Volatile private var latest: TrackedFace? = null

    @Volatile private var latestAt = 0L

    @Volatile private var closed = false

    /** The face from the last detection, or null if none was seen recently. */
    fun current(): TrackedFace? = latest?.takeIf { SystemClock.elapsedRealtime() - latestAt < LOST_AFTER_MS }

    /**
     * Sends [frame] for detection if the detector is free and it's time for another one. The
     * frame is copied, so the caller may release it straight after.
     */
    fun submit(frame: VideoFrame.I420Buffer) {
        val now = SystemClock.elapsedRealtime()
        if (closed || now - lastSubmitAt < MIN_INTERVAL_MS || !busy.compareAndSet(false, true)) return
        lastSubmitAt = now
        val image =
            try {
                InputImage.fromByteArray(toNv21(frame), frame.width, frame.height, 0, InputImage.IMAGE_FORMAT_NV21)
            } catch (e: Exception) {
                busy.set(false)
                Log.w(TAG, "Couldn't prepare frame for face detection", e)
                return
            }
        detector
            .process(image)
            .addOnSuccessListener(executor) { faces ->
                val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                if (face != null) update(track(face))
            }.addOnFailureListener(executor) { Log.w(TAG, "Face detection failed", it) }
            .addOnCompleteListener(executor) { busy.set(false) }
    }

    fun close() {
        closed = true
        detector.close()
        executor.shutdown()
    }

    private fun track(face: Face): TrackedFace {
        val box = face.boundingBox
        fun mark(type: Int) = face.getLandmark(type)?.position

        // ML Kit's left/right are the person's own; sort by x so "left" is left in the image.
        val eyeA = mark(FaceLandmark.LEFT_EYE) ?: PointF(box.left + box.width() * 0.68f, box.top + box.height() * 0.4f)
        val eyeB = mark(FaceLandmark.RIGHT_EYE) ?: PointF(box.left + box.width() * 0.32f, box.top + box.height() * 0.4f)
        val (left, right) = if (eyeA.x <= eyeB.x) eyeA to eyeB else eyeB to eyeA
        val center = PointF((left.x + right.x) / 2, (left.y + right.y) / 2)
        val nose = mark(FaceLandmark.NOSE_BASE) ?: PointF(box.exactCenterX(), box.top + box.height() * 0.62f)
        val mouthL = mark(FaceLandmark.MOUTH_LEFT)
        val mouthR = mark(FaceLandmark.MOUTH_RIGHT)
        val mouth =
            if (mouthL != null && mouthR != null) {
                PointF((mouthL.x + mouthR.x) / 2, (mouthL.y + mouthR.y) / 2)
            } else {
                PointF(box.exactCenterX(), box.top + box.height() * 0.78f)
            }
        return TrackedFace(
            eyeCenter = center,
            leftEye = left,
            rightEye = right,
            nose = nose,
            mouth = mouth,
            eyeDistance = hypot(right.x - left.x, right.y - left.y),
            faceWidth = box.width().toFloat(),
            faceHeight = box.height().toFloat(),
            angle = atan2(right.y - left.y, right.x - left.x),
        )
    }

    /** Blends the new position with the last one, unless the face was lost or jumped a long way. */
    private fun update(next: TrackedFace) {
        val prev = current()
        latest =
            if (prev == null || hypot(next.eyeCenter.x - prev.eyeCenter.x, next.eyeCenter.y - prev.eyeCenter.y) > prev.faceWidth) {
                next
            } else {
                fun mix(a: Float, b: Float) = a + (b - a) * SMOOTHING
                fun mix(a: PointF, b: PointF) = PointF(mix(a.x, b.x), mix(a.y, b.y))
                TrackedFace(
                    eyeCenter = mix(prev.eyeCenter, next.eyeCenter),
                    leftEye = mix(prev.leftEye, next.leftEye),
                    rightEye = mix(prev.rightEye, next.rightEye),
                    nose = mix(prev.nose, next.nose),
                    mouth = mix(prev.mouth, next.mouth),
                    eyeDistance = mix(prev.eyeDistance, next.eyeDistance),
                    faceWidth = mix(prev.faceWidth, next.faceWidth),
                    faceHeight = mix(prev.faceHeight, next.faceHeight),
                    angle = mix(prev.angle, next.angle),
                )
            }
        latestAt = SystemClock.elapsedRealtime()
    }

    /** ML Kit takes NV21: the Y plane, then V and U interleaved. */
    private fun toNv21(buffer: VideoFrame.I420Buffer): ByteArray {
        val w = buffer.width
        val h = buffer.height
        val cw = (w + 1) / 2
        val ch = (h + 1) / 2
        val out = ByteArray(w * h + cw * ch * 2)
        val y = buffer.dataY
        for (r in 0 until h) {
            y.position(r * buffer.strideY)
            y.get(out, r * w, w)
        }
        val u = buffer.dataU
        val v = buffer.dataV
        var o = w * h
        for (r in 0 until ch) {
            val ur = r * buffer.strideU
            val vr = r * buffer.strideV
            for (c in 0 until cw) {
                out[o++] = v.get(vr + c)
                out[o++] = u.get(ur + c)
            }
        }
        return out
    }

    private companion object {
        const val TAG = "FaceTracker"
        const val MIN_INTERVAL_MS = 80L
        const val LOST_AFTER_MS = 700L
        const val SMOOTHING = 0.6f
    }
}
