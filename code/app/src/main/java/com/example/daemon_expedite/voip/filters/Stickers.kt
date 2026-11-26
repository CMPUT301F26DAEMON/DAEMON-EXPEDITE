package com.example.daemon_expedite.voip.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import org.webrtc.VideoFrame
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The face filters' stickers: drawn once with Android's Canvas (our own artwork, no image
 * files), converted to YUV, then blended onto every frame where the tracked face is.
 */
internal object Stickers {
    /** A sticker ready for blending: per-pixel Y, U, V and opacity (0–255). */
    private class Sticker(
        val width: Int,
        val height: Int,
        val y: ByteArray,
        val u: ByteArray,
        val v: ByteArray,
        val alpha: ByteArray,
    )

    private val shades by lazy { sticker(400, 150, ::drawShades) }
    private val crown by lazy { sticker(400, 260, ::drawCrown) }
    private val ears by lazy { sticker(600, 260, ::drawEars) }
    private val nose by lazy { sticker(220, 160, ::drawNose) }
    private val stache by lazy { sticker(400, 150, ::drawStache) }
    private val heart by lazy { sticker(200, 180, ::drawHeart) }

    /** Draws [filter]'s sticker(s) on [face] in [buffer]. Does nothing for colour filters. */
    fun draw(
        filter: VideoFilter,
        face: TrackedFace,
        buffer: VideoFrame.I420Buffer,
    ) {
        val up = face.up
        fun above(from: PointF, distance: Float) = PointF(from.x + up.x * distance, from.y + up.y * distance)

        when (filter) {
            VideoFilter.SHADES -> {
                place(buffer, shades, face.eyeCenter, face.eyeDistance * 2.3f, face.angle)
            }

            VideoFilter.CROWN -> {
                val w = face.faceWidth * 0.9f
                val h = w * crown.height / crown.width
                place(buffer, crown, above(face.eyeCenter, face.faceHeight * 0.6f + h * 0.4f), w, face.angle)
            }

            VideoFilter.PUPPY -> {
                place(buffer, ears, above(face.eyeCenter, face.faceHeight * 0.42f), face.faceWidth * 1.4f, face.angle)
                place(buffer, nose, above(face.nose, face.eyeDistance * 0.12f), face.eyeDistance * 0.8f, face.angle)
            }

            VideoFilter.STACHE -> {
                val at = PointF((face.nose.x + face.mouth.x) / 2, (face.nose.y + face.mouth.y) / 2)
                place(buffer, stache, at, face.eyeDistance * 1.5f, face.angle)
            }

            VideoFilter.LOVE -> {
                place(buffer, heart, face.leftEye, face.eyeDistance * 0.85f, face.angle)
                place(buffer, heart, face.rightEye, face.eyeDistance * 0.85f, face.angle)
            }

            else -> {}
        }
    }

    /**
     * Blends [s] onto [buffer], centred on [center], [width] pixels wide and turned by [angle]
     * radians. Each frame pixel inside the rotated sticker looks up the sticker pixel it covers.
     */
    private fun place(
        buffer: VideoFrame.I420Buffer,
        s: Sticker,
        center: PointF,
        width: Float,
        angle: Float,
    ) {
        if (width < 4f) return
        val scale = width / s.width // frame pixels per sticker pixel
        val halfW = width / 2
        val halfH = s.height * scale / 2
        val c = cos(angle)
        val sn = sin(angle)
        val extentX = abs(halfW * c) + abs(halfH * sn)
        val extentY = abs(halfW * sn) + abs(halfH * c)
        val x0 = max(0, (center.x - extentX).toInt())
        val x1 = min(buffer.width - 1, (center.x + extentX).toInt())
        val y0 = max(0, (center.y - extentY).toInt())
        val y1 = min(buffer.height - 1, (center.y + extentY).toInt())
        if (x0 > x1 || y0 > y1) return

        val yPlane = buffer.dataY
        val uPlane = buffer.dataU
        val vPlane = buffer.dataV
        val inv = 1f / scale
        for (py in y0..y1) {
            val dy = py - center.y
            for (px in x0..x1) {
                val dx = px - center.x
                // Undo the rotation and scale to find the sticker pixel under this frame pixel.
                val sx = ((dx * c + dy * sn) * inv + s.width / 2f).toInt()
                val sy = ((-dx * sn + dy * c) * inv + s.height / 2f).toInt()
                if (sx < 0 || sy < 0 || sx >= s.width || sy >= s.height) continue
                val i = sy * s.width + sx
                val a = s.alpha[i].toInt() and 0xFF
                if (a == 0) continue
                blend(yPlane, py * buffer.strideY + px, s.y[i], a)
                if (px % 2 == 0 && py % 2 == 0) {
                    blend(uPlane, (py / 2) * buffer.strideU + px / 2, s.u[i], a)
                    blend(vPlane, (py / 2) * buffer.strideV + px / 2, s.v[i], a)
                }
            }
        }
    }

    private fun blend(
        plane: ByteBuffer,
        index: Int,
        value: Byte,
        alpha: Int,
    ) {
        val under = plane.get(index).toInt() and 0xFF
        val over = value.toInt() and 0xFF
        plane.put(index, ((over * alpha + under * (255 - alpha)) / 255).toByte())
    }

    /** Renders [draw] into a [width]×[height] bitmap and converts it to YUV (BT.601, as WebRTC uses). */
    private fun sticker(
        width: Int,
        height: Int,
        draw: (Canvas) -> Unit,
    ): Sticker {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        val n = pixels.size
        val y = ByteArray(n)
        val u = ByteArray(n)
        val v = ByteArray(n)
        val alpha = ByteArray(n)
        for (i in 0 until n) {
            val p = pixels[i]
            val a = p ushr 24
            alpha[i] = a.toByte()
            if (a == 0) continue
            // getPixels gives un-premultiplied colours.
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            y[i] = (0.257 * r + 0.504 * g + 0.098 * b + 16).toInt().coerceIn(0, 255).toByte()
            u[i] = (-0.148 * r - 0.291 * g + 0.439 * b + 128).toInt().coerceIn(0, 255).toByte()
            v[i] = (0.439 * r - 0.368 * g - 0.071 * b + 128).toInt().coerceIn(0, 255).toByte()
        }
        return Sticker(width, height, y, u, v, alpha)
    }

    // region Artwork

    private fun paint(color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

    private fun drawShades(c: Canvas) {
        val frame = paint(Color.rgb(15, 15, 18))
        c.drawRoundRect(RectF(8f, 16f, 392f, 36f), 10f, 10f, frame)
        c.drawRect(RectF(178f, 40f, 222f, 58f), frame)
        val lensPaint =
            paint(Color.BLACK).apply {
                shader = LinearGradient(0f, 20f, 0f, 135f, Color.rgb(60, 60, 70), Color.rgb(5, 5, 8), Shader.TileMode.CLAMP)
            }
        val shine = paint(Color.argb(110, 255, 255, 255))
        for (left in floatArrayOf(18f, 214f)) {
            val lens = RectF(left, 20f, left + 168f, 135f)
            c.drawRoundRect(lens, 44f, 44f, frame)
            c.drawRoundRect(RectF(lens.left + 7, lens.top + 7, lens.right - 7, lens.bottom - 7), 38f, 38f, lensPaint)
            c.drawOval(RectF(left + 26f, 36f, left + 74f, 60f), shine)
        }
    }

    private fun drawCrown(c: Canvas) {
        val outline =
            Path().apply {
                moveTo(20f, 245f)
                lineTo(20f, 55f)
                lineTo(110f, 150f)
                lineTo(200f, 22f)
                lineTo(290f, 150f)
                lineTo(380f, 55f)
                lineTo(380f, 245f)
                close()
            }
        val gold =
            paint(Color.YELLOW).apply {
                shader = LinearGradient(0f, 20f, 0f, 245f, Color.rgb(255, 222, 89), Color.rgb(222, 160, 0), Shader.TileMode.CLAMP)
            }
        c.drawPath(outline, gold)
        val edge =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(160, 105, 0)
                style = Paint.Style.STROKE
                strokeWidth = 8f
                strokeJoin = Paint.Join.ROUND
            }
        c.drawPath(outline, edge)
        c.drawRect(RectF(24f, 192f, 376f, 241f), paint(Color.rgb(205, 140, 0)))
        val jewels = intArrayOf(Color.rgb(230, 40, 60), Color.rgb(40, 110, 230), Color.rgb(30, 180, 90))
        floatArrayOf(20f, 200f, 380f).forEachIndexed { i, x ->
            c.drawCircle(x, if (i == 1) 26f else 58f, 17f, paint(jewels[i]))
        }
        floatArrayOf(90f, 200f, 310f).forEachIndexed { i, x ->
            c.drawCircle(x, 216f, 15f, paint(jewels[(i + 1) % 3]))
        }
    }

    private fun drawEars(c: Canvas) {
        val fur = paint(Color.rgb(140, 92, 50))
        val inner = paint(Color.rgb(235, 160, 165))
        for ((x, tilt) in listOf(110f to -28f, 490f to 28f)) {
            c.save()
            c.rotate(tilt, x, 140f)
            c.drawOval(RectF(x - 75f, 20f, x + 75f, 255f), fur)
            c.drawOval(RectF(x - 42f, 60f, x + 42f, 220f), inner)
            c.restore()
        }
    }

    private fun drawNose(c: Canvas) {
        c.drawOval(RectF(18f, 18f, 202f, 142f), paint(Color.rgb(25, 20, 22)))
        c.drawOval(RectF(55f, 34f, 110f, 62f), paint(Color.argb(120, 255, 255, 255)))
    }

    private fun drawStache(c: Canvas) {
        val half =
            Path().apply {
                moveTo(200f, 45f)
                cubicTo(170f, 25f, 110f, 30f, 70f, 75f)
                cubicTo(50f, 98f, 18f, 98f, 8f, 62f)
                cubicTo(28f, 88f, 55f, 90f, 85f, 100f)
                cubicTo(130f, 118f, 175f, 110f, 200f, 90f)
                close()
            }
        val whole = Path(half).apply { addPath(half, Matrix().apply { setScale(-1f, 1f, 200f, 0f) }) }
        c.drawPath(whole, paint(Color.rgb(55, 34, 20)))
    }

    private fun drawHeart(c: Canvas) {
        val heart =
            Path().apply {
                moveTo(100f, 172f)
                cubicTo(-45f, 80f, 25f, -25f, 100f, 45f)
                cubicTo(175f, -25f, 245f, 80f, 100f, 172f)
                close()
            }
        c.drawPath(heart, paint(Color.rgb(255, 50, 90)))
        c.drawOval(RectF(42f, 38f, 80f, 64f), paint(Color.argb(130, 255, 255, 255)))
    }

    // endregion
}
