package com.example.daemon_expedite.ui_components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val FLOAT_MS = 180

/**
 * Decoration for a text field whose label works as the placeholder.
 *
 * While [floated] is false the label sits in line where the text goes, in [inlineStyle] and
 * [inlineColor]. When it turns true the label shrinks, recolors and slides up above the text,
 * ending as the small uppercase label in [floatedStyle] and [floatedColor]. The room for the
 * small label opens up as it rises, so the in-line label lines up with a leading icon. Give the
 * field a minimum height (the fields here use 72dp) so its outline doesn't move meanwhile.
 *
 * Also works for read-only values: pass a `Text` as [innerTextField] and `floated = value.isNotBlank()`.
 *
 * Use it inside a `BasicTextField`'s `decorationBox`:
 * ```
 * decorationBox = { inner ->
 *     FloatingLabel(label = "Name", floated = focused || value.isNotEmpty(), ...) { inner() }
 * }
 * ```
 *
 * @param label The label, written normally. It's shown in uppercase once floated.
 * @param floated Whether the label is up above the text.
 * @param gap Space between the floated label and the text.
 * @param innerTextField The text field itself (the `decorationBox` lambda's parameter).
 */
@Composable
fun FloatingLabel(
    label: String,
    floated: Boolean,
    inlineStyle: TextStyle,
    floatedStyle: TextStyle,
    modifier: Modifier = Modifier,
    inlineColor: Color = AppColors.TextMuted,
    floatedColor: Color = AppColors.Secondary,
    gap: Dp = 4.dp,
    innerTextField: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (floated) 1f else 0f,
        animationSpec = tween(FLOAT_MS, easing = FastOutSlowInEasing),
        label = "floatingLabel",
    )
    val style = lerp(inlineStyle, floatedStyle, progress)
    val color = lerp(inlineColor, floatedColor, progress)

    Layout(
        modifier = modifier,
        content = {
            // The in-line label is written normally and the floated one in uppercase, so the two
            // cross-fade while they move instead of the letters switching case mid-slide.
            Box {
                Text(label, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(1f - progress))
                Text(label.uppercase(), style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(progress))
            }
            Box { innerTextField() }
            // Measured only, never placed: the height the floated label needs.
            Text(label.uppercase(), style = floatedStyle, maxLines = 1)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val labelPlaceable = measurables[0].measure(loose)
        val textPlaceable = measurables[1].measure(constraints.copy(minHeight = 0))
        val floatedLabelHeight = measurables[2].measure(loose).height
        val gapPx = gap.roundToPx()

        // The room above the text for the small label opens up as the label rises, so while the
        // label is in line the whole block is just one line tall and lines up with a leading icon.
        val textY = ((floatedLabelHeight + gapPx) * progress).roundToInt()
        val width = maxOf(textPlaceable.width, constraints.minWidth)
        val height = maxOf(textY + textPlaceable.height, constraints.minHeight)

        // In line: centred on the text's line (one line, since the field is empty then).
        // Floated: at the very top.
        val inlineY = textY + (textPlaceable.height - labelPlaceable.height) / 2
        val labelY = (inlineY * (1f - progress)).roundToInt()

        layout(width, height) {
            textPlaceable.place(0, textY)
            labelPlaceable.place(0, labelY)
        }
    }
}
