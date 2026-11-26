package com.example.daemon_expedite.voip.filters

import org.webrtc.VideoFrame
import java.nio.ByteBuffer

/**
 * Colour filters, done directly on the camera's YUV frames.
 *
 * In YUV, Y is brightness and U/V are colour (128 = no colour). So "black and white" is just
 * setting U and V to 128, a tint is shifting them, and saturation is scaling their distance from
 * 128. Each filter is three lookup tables (one per plane), so a frame costs one table lookup per
 * byte, cheap enough for every frame of a call.
 */
internal object ColorFilters {
    private class Tables(
        val y: ByteArray?,
        val u: ByteArray?,
        val v: ByteArray?,
    )

    private fun table(f: (Int) -> Int) = ByteArray(256) { f(it).coerceIn(0, 255).toByte() }

    private fun constant(value: Int) = table { value }

    private fun contrast(amount: Double) = table { ((it - 128) * amount + 128).toInt() }

    private val tables: Map<VideoFilter, Tables> =
        mapOf(
            VideoFilter.MONO to Tables(null, constant(128), constant(128)),
            VideoFilter.NOIR to Tables(contrast(1.45), constant(128), constant(128)),
            VideoFilter.SEPIA to Tables(table { (it * 0.95 + 8).toInt() }, constant(112), constant(146)),
            VideoFilter.WARM to Tables(table { it + 4 }, table { it - 12 }, table { it + 16 }),
            VideoFilter.COOL to Tables(null, table { it + 18 }, table { it - 12 }),
            VideoFilter.VIVID to Tables(contrast(1.12), table { ((it - 128) * 1.7 + 128).toInt() }, table { ((it - 128) * 1.7 + 128).toInt() }),
        )

    /** A row of pixels, reused to avoid allocating for every row of every frame. */
    private val row = ThreadLocal.withInitial { ByteArray(0) }

    /** Applies [filter] to [buffer] in place. Face filters and [VideoFilter.NONE] change nothing. */
    fun apply(
        filter: VideoFilter,
        buffer: VideoFrame.I420Buffer,
    ) {
        val t = tables[filter] ?: return
        val chromaW = (buffer.width + 1) / 2
        val chromaH = (buffer.height + 1) / 2
        t.y?.let { map(buffer.dataY, buffer.strideY, buffer.width, buffer.height, it) }
        t.u?.let { map(buffer.dataU, buffer.strideU, chromaW, chromaH, it) }
        t.v?.let { map(buffer.dataV, buffer.strideV, chromaW, chromaH, it) }
    }

    private fun map(
        plane: ByteBuffer,
        stride: Int,
        width: Int,
        height: Int,
        lut: ByteArray,
    ) {
        var line = row.get()!!
        if (line.size < width) {
            line = ByteArray(width)
            row.set(line)
        }
        for (r in 0 until height) {
            val start = r * stride
            plane.position(start)
            plane.get(line, 0, width)
            for (i in 0 until width) line[i] = lut[line[i].toInt() and 0xFF]
            plane.position(start)
            plane.put(line, 0, width)
        }
        plane.rewind()
    }
}
