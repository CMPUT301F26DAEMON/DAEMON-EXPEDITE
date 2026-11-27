package com.example.daemon_expedite.ui_components

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import com.example.daemon_expedite.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// The bear is "The Boss" (Bear 122), the huge grizzly that rules Banff National Park in
// Alberta. He is drawn side-on, facing right, in his own grid of BEAR_WIDTH x BEAR_HEIGHT
// units, scaled up by BOSS_SCALE on screen because he is the biggest bear around. His look:
// long low body, big shoulder hump, head carried low with a long snout, thick legs, a
// scarred face and a yellow ear tag.
private const val BEAR_WIDTH = 160f
private const val BEAR_HEIGHT = 96f

/** How many dp one bear unit takes on screen. */
private const val BOSS_SCALE = 1.35f

/** The bear's width on screen, in dp. */
private const val WIDTH_DP = BEAR_WIDTH * BOSS_SCALE

/** Where the legs join the body: back legs at HIP_X, front legs at SHOULDER_X. */
private const val HIP_X = 42f
private const val SHOULDER_X = 100f
private const val HIP_Y = 62f

private const val LEG_LENGTH = 28f

/** How far each leg swings forward and back from straight down, in degrees. */
private const val SWING_DEGREES = 20f

/** How much a leg shortens (bends) while it swings forward off the ground. */
private const val LEG_LIFT = 6f

/** The feet rest on this line when the leg is planted. */
private const val GROUND_Y = HIP_Y + LEG_LENGTH

/** Thickness of the dark outline around the bear. */
private const val OUTLINE = 2.4f

/** The head turns around this point, roughly where the neck meets the shoulders. */
private val NECK = Offset(112f, 46f)

/**
 * How far the bear moves in one full walk cycle. Each leg is planted for half a cycle and
 * sweeps back by 2 * L * sin(swing) while it is, so tying movement to this keeps the feet
 * from sliding along the ground.
 */
private val STRIDE = 4 * LEG_LENGTH * sin(SWING_DEGREES * PI.toFloat() / 180f)

/** [STRIDE] on screen, in dp. */
private val STRIDE_DP = STRIDE * BOSS_SCALE

/** How long one walk cycle takes at full speed, in milliseconds. */
private const val CYCLE_MILLIS = 520

/**
 * Walk-in easing: starts at the normal walking pace (no rush off the edge of the screen) and
 * only slows gently over the last stretch as the bear comes to a stop.
 */
private val WalkInEasing = CubicBezierEasing(0.25f, 0.25f, 0.4f, 1f)

/** How long the bear stops in the middle to sniff the air, in milliseconds. */
private const val SNIFF_MILLIS = 3000

/** Rump, back, the big grizzly shoulder hump and belly. The head is drawn separately so it can move. */
private val BodyPath = Path().apply {
    moveTo(15f, 54f)
    cubicTo(11f, 40f, 20f, 26f, 38f, 27f) // big round rump
    cubicTo(52f, 28f, 62f, 32f, 74f, 30f) // back, dipping slightly
    cubicTo(82f, 27f, 88f, 16f, 100f, 17f) // big shoulder hump
    cubicTo(108f, 18f, 112f, 28f, 116f, 40f) // down the neck
    lineTo(118f, 58f)
    cubicTo(114f, 64f, 112f, 66f, 108f, 68f) // chest
    cubicTo(90f, 72f, 60f, 72f, 40f, 71f) // belly
    cubicTo(24f, 70f, 17f, 64f, 15f, 54f) // back of the thigh
    close()
}

/** Head with a long snout that tapers down to the nose. */
private val HeadPath = Path().apply {
    moveTo(110f, 36f)
    cubicTo(114f, 32f, 118f, 32f, 122f, 33f) // top of the head
    cubicTo(130f, 35f, 136f, 40f, 142f, 45f) // forehead
    lineTo(151f, 50f) // bridge of the snout
    cubicTo(153f, 52f, 152f, 55f, 149f, 56f) // nose tip
    cubicTo(144f, 58f, 136f, 60f, 128f, 61f) // jaw
    cubicTo(120f, 62f, 112f, 60f, 110f, 54f) // throat
    cubicTo(108f, 48f, 107f, 40f, 110f, 36f)
    close()
}

/** Lighter along the back, darker towards the belly, like light from above. */
private val FurBrush = Brush.verticalGradient(
    0f to AppColors.BearFurLight,
    0.45f to AppColors.BearFur,
    1f to AppColors.BearFurShade,
    startY = 16f,
    endY = 72f,
)

private val OutlineStroke = Stroke(width = OUTLINE * 2, join = StrokeJoin.Round)

/** Searches that bring out The Boss, with case, spaces and punctuation removed. */
private val BossQueries = setOf("bear", "boss", "theboss", "bear122")

/**
 * Easter egg: true when [query] is "bear", "boss", "the boss" or "bear 122", ignoring case,
 * spaces and punctuation.
 */
fun isBearQuery(query: String): Boolean =
    query.lowercase().filter { it.isLetterOrDigit() } in BossQueries

/**
 * Full-screen overlay where The Boss walks in from the left along the bottom of the screen,
 * stops in the middle to sniff the air (and warn you about your snacks), then walks off to
 * the right.
 * It plays once each time [trigger] turns true. It only draws, so taps still reach the
 * screen underneath.
 *
 * @param trigger Plays a new walk every time this changes from false to true.
 */
@Composable
fun BearWalk(trigger: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth.value
        // Left edge of the bear, in dp. Its legs are driven by how far it has walked.
        val x = remember { Animatable(0f) }
        val sniff = remember { Animatable(0f) }
        var walking by remember { mutableStateOf(false) }
        var showBubble by remember { mutableStateOf(false) }
        val start = -WIDTH_DP
        val context = LocalContext.current

        LaunchedEffect(trigger) {
            if (!trigger) return@LaunchedEffect
            // Stop near the middle at a point in the cycle where all four feet are planted.
            val middle = (screenWidth - WIDTH_DP) / 2
            val halfStrides = ((middle - start) / (STRIDE_DP / 2) - 0.5f).roundToInt()
            val stop = start + (halfStrides + 0.5f) * (STRIDE_DP / 2)

            showBubble = false
            sniff.snapTo(0f)
            x.snapTo(start)
            walking = true
            var growl: MediaPlayer? = null
            try {
                x.animateTo(stop, tween(walkMillis(stop - start), easing = WalkInEasing))
                showBubble = true
                // The Boss roars while he stops to sniff. The roar is a real grizzly from the
                // Yellowstone sound library (NPS & MSU Acoustic Atlas / Jennifer Jerrett,
                // public domain). Sound is a bonus, so a missing or broken player never
                // stops the walk.
                growl = runCatching { MediaPlayer.create(context, R.raw.boss_growl) }.getOrNull()
                growl?.start()
                sniff.animateTo(1f, tween(SNIFF_MILLIS, easing = LinearEasing))
                x.animateTo(screenWidth, tween(walkMillis(screenWidth - stop), easing = FastOutLinearInEasing))
            } finally {
                growl?.release()
                walking = false
                showBubble = false
            }
        }

        if (walking) {
            val phase = 2 * PI.toFloat() * (x.value - start) / STRIDE_DP
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(bottom = BottomNavBarHeight)
                    .offset(x = x.value.dp),
            ) {
                Canvas(modifier = Modifier.size(WIDTH_DP.dp, (BEAR_HEIGHT * BOSS_SCALE).dp)) {
                    scale(size.width / BEAR_WIDTH, pivot = Offset.Zero) {
                        drawBear(phase = phase, sniff = sniff.value)
                    }
                }
                AnimatedVisibility(
                    visible = showBubble,
                    enter = scaleIn() + fadeIn(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-14).dp),
                ) {
                    Text(
                        text = "The Boss is in town. Hide your snacks!",
                        style = AppTextStyles.Caption,
                        maxLines = 1,
                        softWrap = false,
                        color = AppColors.TextOnAccent,
                        modifier = Modifier
                            .background(AppColors.Accent, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** How long walking [distance] dp takes at the bear's walking speed. */
private fun walkMillis(distance: Float): Int =
    (distance / STRIDE_DP * CYCLE_MILLIS).roundToInt().coerceAtLeast(1)

/** The four legs: where each hangs from, how thick its thigh is, and its place in the cycle. */
private class Leg(val hipX: Float, val thigh: Float, val phaseOffset: Float)

// Diagonal legs move together, like a real walk: near back with far front, and near front
// with far back. Back legs have thicker thighs, as on the reference grizzly.
private val FarLegs = listOf(
    Leg(HIP_X + 7f, thigh = 11f, phaseOffset = PI.toFloat()),
    Leg(SHOULDER_X + 7f, thigh = 9f, phaseOffset = 0f),
)
private val NearLegs = listOf(
    Leg(HIP_X, thigh = 11f, phaseOffset = 0f),
    Leg(SHOULDER_X, thigh = 9f, phaseOffset = PI.toFloat()),
)

/**
 * Draws the bear at [phase] (radians) through its walk cycle. [sniff] runs 0 to 1 while it
 * stops and lifts its nose to sniff the air three times.
 *
 * Every outline is drawn before any fill, so the body, head and near legs share one
 * silhouette outline with no seams between the parts.
 */
private fun DrawScope.drawBear(phase: Float, sniff: Float) {
    // The body dips when the legs are spread, so the planted feet stay on the ground.
    val spread = SWING_DEGREES * abs(sin(phase))
    val bodyDrop = LEG_LENGTH * (1 - cos(spread * PI.toFloat() / 180f))
    // The head bobs a little with each step, and lifts to sniff when stopped.
    val headTilt = 2f * sin(2 * phase) - 9f * abs(sin(sniff * 3 * PI.toFloat()))

    drawOval(AppColors.BearShadow, topLeft = Offset(30f, GROUND_Y - 3f), size = Size(104f, 6f))

    translate(top = bodyDrop) {
        // Far legs sit behind everything, in shade.
        FarLegs.forEach { drawLeg(it, phase, outlineOnly = true) }
        FarLegs.forEach { drawLeg(it, phase, outlineOnly = false) }

        // Outline pass.
        drawCircle(AppColors.BearFeatures, radius = 4f + OUTLINE, center = Offset(14f, 42f))
        drawPath(BodyPath, AppColors.BearFeatures, style = OutlineStroke)
        rotate(headTilt, pivot = NECK) {
            drawCircle(AppColors.BearFeatures, radius = 4.5f + OUTLINE, center = Offset(119f, 31f))
            drawPath(HeadPath, AppColors.BearFeatures, style = OutlineStroke)
        }
        NearLegs.forEach { drawLeg(it, phase, outlineOnly = true) }

        // Fill pass.
        drawCircle(AppColors.BearFur, radius = 4f, center = Offset(14f, 42f))
        NearLegs.forEach { drawLeg(it, phase, outlineOnly = false) }
        drawPath(BodyPath, FurBrush)
        rotate(headTilt, pivot = NECK) {
            drawCircle(AppColors.BearFur, radius = 4.5f, center = Offset(119f, 31f))
            drawCircle(AppColors.BearFurShade, radius = 2f, center = Offset(119.5f, 31.5f))
            drawPath(HeadPath, FurBrush)
            drawOval(AppColors.BearMuzzle, topLeft = Offset(136f, 52f), size = Size(12f, 5f))
            drawOval(AppColors.BearFeatures, topLeft = Offset(147f, 49.5f), size = Size(6f, 5f))
            drawCircle(AppColors.BearFeatures, radius = 1.7f, center = Offset(133f, 42f))
            // The Boss's battle scars across his face.
            ScarLines.forEach { (from, to) ->
                drawLine(AppColors.BearScar, from, to, strokeWidth = 1.2f, cap = StrokeCap.Round)
            }
            // Yellow ear tag, like the park's tagged bears wear.
            drawRoundRect(
                AppColors.BearEarTag,
                topLeft = Offset(120f, 33f),
                size = Size(3.5f, 5f),
                cornerRadius = CornerRadius(1f),
            )
        }
    }
}

/** Short scars across The Boss's face, as (start, end) pairs in bear units. */
private val ScarLines = listOf(
    Offset(127f, 38f) to Offset(131f, 46f),
    Offset(137f, 44f) to Offset(141f, 50f),
    Offset(139f, 43f) to Offset(142f, 47f),
)

/** Paw pointing forward, with the bottom of the leg at (0, 0) and the sole along y = 0. */
private val PawPath = Path().apply {
    moveTo(-7f, -6f)
    lineTo(6f, -6f)
    quadraticTo(11f, -5f, 11f, -1.5f) // top of the paw
    quadraticTo(11f, 0f, 8f, 0f) // toes
    lineTo(-5f, 0f) // sole
    quadraticTo(-7f, 0f, -7f, -2.5f) // heel
    close()
}

/**
 * Draws one thick, tapering leg with a paw that points forward. The leg swings forward and
 * back with the walk cycle and bends (shortens) while it swings forward, so the paw clears
 * the ground. The paw is drawn separately so it stays flat on the ground while planted,
 * and only tips its toes down while lifted. Far legs are shaded; near legs darken towards
 * the paw.
 */
private fun DrawScope.drawLeg(leg: Leg, phase: Float, outlineOnly: Boolean) {
    val legPhase = phase + leg.phaseOffset
    val forward = SWING_DEGREES * sin(legPhase)
    val liftAmount = max(0f, cos(legPhase))
    val length = LEG_LENGTH - LEG_LIFT * liftAmount
    val isFar = leg in FarLegs

    // Leg hanging straight down from (0, 0); the top is tucked up inside the body and the
    // bottom tucks into the paw.
    val shaft = Path().apply {
        moveTo(-leg.thigh, -14f)
        lineTo(leg.thigh, -14f)
        cubicTo(leg.thigh, length - 14f, 6.5f, length - 10f, 6.5f, length - 3f)
        lineTo(-6.5f, length - 3f)
        cubicTo(-6.5f, length - 10f, -leg.thigh, length - 16f, -leg.thigh, -14f)
        close()
    }
    // Where the bottom of the leg ends up once swung. Rotating by -forward moves the
    // point (0, length) to (length * sin, length * cos).
    val radians = forward * PI.toFloat() / 180f
    val foot = Offset(leg.hipX + length * sin(radians), HIP_Y + length * cos(radians))

    if (outlineOnly) {
        translate(left = leg.hipX, top = HIP_Y) {
            rotate(degrees = -forward, pivot = Offset.Zero) {
                drawPath(shaft, AppColors.BearFeatures, style = OutlineStroke)
            }
        }
        drawPaw(foot, liftAmount) { drawPath(it, AppColors.BearFeatures, style = OutlineStroke) }
        return
    }

    translate(left = leg.hipX, top = HIP_Y) {
        // Positive rotation is clockwise, which would swing the leg backwards.
        rotate(degrees = -forward, pivot = Offset.Zero) {
            if (isFar) {
                drawPath(shaft, AppColors.BearFurShade)
            } else {
                drawPath(
                    shaft,
                    Brush.verticalGradient(
                        0.4f to AppColors.BearFur,
                        1f to AppColors.BearFurShade,
                        startY = -14f,
                        endY = length,
                    ),
                )
            }
        }
    }
    drawPaw(foot, liftAmount) { drawPath(it, AppColors.BearFurShade) }
}

/** Places [PawPath] at [foot], tipping the toes down by up to 15 degrees as it lifts. */
private inline fun DrawScope.drawPaw(foot: Offset, liftAmount: Float, draw: DrawScope.(Path) -> Unit) {
    translate(left = foot.x, top = foot.y) {
        rotate(degrees = 15f * liftAmount, pivot = Offset.Zero) {
            draw(PawPath)
        }
    }
}
