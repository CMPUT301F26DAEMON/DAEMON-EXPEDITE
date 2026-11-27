package com.example.daemon_expedite.ui_components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** How long one shower of marshmallows lasts, in milliseconds. */
private const val SHOWER_MILLIS = 3600

/** How many marshmallows fall in one shower. */
private const val MARSHMALLOW_COUNT = 90

/**
 * Easter egg: true when [query] spells "s'mores", ignoring case, spaces and the apostrophe
 * (so "smores", "S'mores" and "s mores" all count).
 */
fun isSmoresQuery(query: String): Boolean =
    query.lowercase().filter { it.isLetter() } == "smores"

/**
 * One marshmallow in the shower. Every value is a fraction so the shower fits any screen size.
 *
 * @property x Horizontal position, as a fraction of the screen width.
 * @property delay When it starts falling, as a fraction of the whole shower.
 * @property fall How long it takes to cross the screen, as a fraction of the whole shower.
 * @property size Width in dp.
 * @property startAngle Rotation when it appears, in degrees.
 * @property spin Total rotation while it falls, in degrees.
 */
private data class Marshmallow(
    val x: Float,
    val delay: Float,
    val fall: Float,
    val size: Float,
    val startAngle: Float,
    val spin: Float,
)

private fun randomMarshmallow() = Marshmallow(
    x = Random.nextFloat(),
    delay = Random.nextFloat() * 0.725f,
    fall = 0.225f + Random.nextFloat() * 0.05f,
    size = 18f + Random.nextFloat() * 14f,
    startAngle = Random.nextFloat() * 360f,
    spin = (Random.nextFloat() - 0.5f) * 540f,
)

/**
 * Full-screen overlay that rains toasted marshmallows once each time [trigger] turns true.
 * It only draws, so taps still reach the screen underneath.
 *
 * @param trigger Plays a new shower every time this changes from false to true.
 */
@Composable
fun SmoresRain(trigger: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    var marshmallows by remember { mutableStateOf(emptyList<Marshmallow>()) }

    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        marshmallows = List(MARSHMALLOW_COUNT) { randomMarshmallow() }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(SHOWER_MILLIS, easing = LinearEasing))
    }

    if (progress.value >= 1f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        marshmallows.forEach { m ->
            val t = (progress.value - m.delay) / m.fall
            if (t !in 0f..1f) return@forEach

            val width = m.size.dp.toPx()
            val height = width * 0.85f
            val left = m.x * (size.width - width)
            val top = -height + t * (size.height + 2 * height)
            val center = Offset(left + width / 2, top + height / 2)

            rotate(degrees = m.startAngle + m.spin * t, pivot = center) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to AppColors.MarshmallowToasted,
                        0.4f to AppColors.Marshmallow,
                        startY = top,
                        endY = top + height,
                    ),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(width * 0.3f),
                )
            }
        }
    }
}
