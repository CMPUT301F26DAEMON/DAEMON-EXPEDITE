package com.example.daemon_expedite.voip.filters

import android.util.Log
import org.webrtc.JavaI420Buffer
import org.webrtc.VideoFrame
import org.webrtc.VideoProcessor
import org.webrtc.VideoSink
import org.webrtc.YuvHelper

/**
 * Sits between the camera and the video call: every camera frame passes through here before it
 * is shown in the self-preview and sent to the other person.
 *
 * With no filter, frames go straight through untouched (no cost). With a filter, each frame is
 * copied into an upright YUV image (cameras deliver frames sideways plus a rotation), the filter
 * is applied to it, and the result is sent on instead. Runs on the camera's thread.
 */
internal class FilterProcessor : VideoProcessor {
    @Volatile var filter: VideoFilter = VideoFilter.NONE

    @Volatile private var sink: VideoSink? = null
    private var faceTracker: FaceTracker? = null

    override fun setSink(sink: VideoSink?) {
        this.sink = sink
    }

    override fun onCapturerStarted(success: Boolean) {}

    override fun onCapturerStopped() {}

    override fun onFrameCaptured(frame: VideoFrame) {
        val out = sink ?: return
        val current = filter
        if (current == VideoFilter.NONE) {
            out.onFrame(frame)
            return
        }

        val upright =
            try {
                upright(frame)
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't filter frame; sending it unfiltered", e)
                out.onFrame(frame)
                return
            }
        try {
            ColorFilters.apply(current, upright)
            if (current.tracksFace) {
                val tracker = faceTracker ?: FaceTracker().also { faceTracker = it }
                tracker.submit(upright)
                tracker.current()?.let { Stickers.draw(current, it, upright) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Filter ${current.name} failed on a frame", e)
        }
        val filtered = VideoFrame(upright, 0, frame.timestampNs)
        out.onFrame(filtered)
        filtered.release() // also releases upright
    }

    /** Stops face tracking. Call when the camera is shut down. */
    fun release() {
        faceTracker?.close()
        faceTracker = null
    }

    /** Copies [frame] into a new I420 image, turned the right way up. The caller releases it. */
    private fun upright(frame: VideoFrame): JavaI420Buffer {
        val source = frame.buffer.toI420() ?: error("Frame has no pixels")
        try {
            val w = source.width
            val h = source.height
            val turned = frame.rotation % 180 != 0
            val out = JavaI420Buffer.allocate(if (turned) h else w, if (turned) w else h)
            YuvHelper.I420Rotate(
                source.dataY,
                source.strideY,
                source.dataU,
                source.strideU,
                source.dataV,
                source.strideV,
                out.dataY,
                out.strideY,
                out.dataU,
                out.strideU,
                out.dataV,
                out.strideV,
                w,
                h,
                frame.rotation,
            )
            return out
        } finally {
            source.release()
        }
    }

    private companion object {
        const val TAG = "FilterProcessor"
    }
}
