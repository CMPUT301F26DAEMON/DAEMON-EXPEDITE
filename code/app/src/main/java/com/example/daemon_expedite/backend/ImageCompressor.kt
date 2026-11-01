package com.example.daemon_expedite.backend

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Shrinks and compresses photos before they are uploaded, to save Storage space and data.
 *
 * Phone photos are often 3–5 MB at 4000 px wide, but the app shows them much smaller, so most
 * of that is wasted. Each photo is scaled down so its longest side is at most a given size, then
 * saved as a JPEG. A 4000 px photo typically ends up around 50–150 KB.
 */
object ImageCompressor {

    /** Profile photos are shown at up to 124dp (~370 px on a high-density screen). */
    const val PROFILE_MAX_SIDE_PX = 512

    /** Campsite photos fill the width of the screen (~1080 px on most phones). */
    const val CAMPSITE_MAX_SIDE_PX = 1280

    /** 0–100. 80 looks the same as the original to the eye at a fraction of the size. */
    const val JPEG_QUALITY = 80

    /**
     * Reads the image at [uri], scales it so its longest side is at most [maxSidePx] (never
     * scales up), and returns it as JPEG bytes.
     *
     * Runs on a background thread, because decoding and compressing are slow enough to freeze
     * the screen. Works with photo-picker `content://` URIs and `file://` URIs.
     *
     * The image is decoded straight to the smaller size, so a large photo is never fully loaded
     * into memory. The camera's rotation (EXIF orientation) is applied, so photos stay upright.
     */
    suspend fun compressToJpeg(
        resolver: ContentResolver,
        uri: Uri,
        maxSidePx: Int,
        quality: Int = JPEG_QUALITY,
    ): ByteArray = withContext(Dispatchers.Default) {
        val source = ImageDecoder.createSource(resolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val scale = maxSidePx.toFloat() / max(width, height)
            if (scale < 1f) {
                decoder.setTargetSize(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1),
                )
            }
            // Software bitmaps can be drawn on and compressed; hardware ones can't be read back.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

        // JPEG has no transparency, so transparent areas (e.g. in a PNG) would turn black.
        // Draw onto white first instead.
        val opaque = if (decoded.hasAlpha()) onWhiteBackground(decoded) else decoded

        ByteArrayOutputStream().use { out ->
            opaque.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (opaque !== decoded) opaque.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }

    private fun onWhiteBackground(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        return result
    }
}
