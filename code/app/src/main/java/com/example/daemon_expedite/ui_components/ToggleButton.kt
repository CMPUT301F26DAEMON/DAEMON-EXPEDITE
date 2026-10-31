package com.example.daemon_expedite.ui_components

import android.R.attr.maxWidth
import android.widget.ToggleButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.example.daemon_expedite.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

/**
 * A segmented toggle with a sliding, spring-animated selection pill.
 *
 * The number of segments equals [options].size. The composable is stateless:
 * it displays whatever [selectedIndex] you give it and reports taps through
 * [onOptionSelected]. Update your own state in that callback, and run any
 * extra logic (analytics, ViewModel calls, navigation) there as well.
 *
 * Usage:
 * ```
 * var selected by remember { mutableIntStateOf(0) }
 *
 * Togglebutton(
 *     options = listOf("Entrant", "Owner", "Admin"),
 *     selectedIndex = selected,
 *     onOptionSelected = { selected = it }
 * )
 * ```
 *
 * @param options Labels to display, one per segment, in order. Must not be empty.
 * @param selectedIndex Index into [options] of the currently selected segment.
 * @param onOptionSelected Called with the index of the tapped segment. It is also
 * called when the already selected segment is tapped, so deduplicate if needed.
 * @param modifier [Modifier] applied to the toggle track.
 * @param width Total width of the toggle. When left as [Dp.Unspecified] (the
 * default) the toggle fills the available width. Segments share the width equally.
 * @param height Total height of the toggle, including its inner padding.
 * @throws IllegalArgumentException if [options] is empty.
 *
 * @see SegmentedToggleWithContent for a variant that also animates content
 * between selections.
 */
@Composable
fun Togglebutton(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = 48.dp,
) {
    require(options.isNotEmpty()) { "options must not be empty" }

    val trackShape = RoundedCornerShape(20.dp)
    val indicatorShape = RoundedCornerShape(16.dp)

    BoxWithConstraints(
        modifier = modifier
            .then(if (width.isSpecified) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .background(AppColors.Surface, trackShape)
            .border(BorderStroke(1.dp, AppColors.OutlineSubtle), trackShape)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / options.size

        // Slides the pill to the selected segment with a soft spring.
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "indicatorOffset"
        )

        // Sliding selection pill.
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(AppColors.Primary, indicatorShape)
                .border(BorderStroke(1.dp, AppColors.OutlineStrong), indicatorShape)
        )

        // Tappable labels drawn on top of the pill.
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val textColor by animateColorAsState(
                    targetValue = if (index == selectedIndex) AppColors.TextOnPrimary else AppColors.TextPrimary,
                    label = "textColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        style = AppTextStyles.TabLabel,
                    )
                }
            }
        }
    }
}

/**
 * A [Togglebutton] with a content area below it that animates whenever the
 * selection changes.
 *
 * The content slides and fades in the direction of travel: choosing an option
 * to the right of the current one slides new content in from the right, and
 * choosing one to the left slides it in from the left.
 *
 * Usage:
 * ```
 * var selected by remember { mutableIntStateOf(0) }
 *
 * ToggleButtonWithContent(
 *     options = listOf("Entrant", "Owner", "Admin"),
 *     selectedIndex = selected,
 *     onOptionSelected = { selected = it }
 * ) { index ->
 *     when (index) {
 *         0 -> EntrantScreen()
 *         1 -> OwnerScreen()
 *         else -> AdminScreen()
 *     }
 * }
 * ```
 *
 * @param options Labels to display, one per segment, in order. Must not be empty.
 * @param selectedIndex Index into [options] of the currently selected segment.
 * @param onOptionSelected Called with the index of the tapped segment.
 * @param modifier [Modifier] applied to the whole column (toggle plus content).
 * @param toggleWidth Width of the toggle itself. Defaults to filling the available
 * width. See [Togglebutton].
 * @param toggleHeight Height of the toggle itself. See [Togglebutton].
 * @param spacing Vertical gap between the toggle and the content.
 * @param content Content for the selected segment. Receives the index being
 * displayed, which during a transition is the index of the content currently
 * animating in or out, not necessarily [selectedIndex].
 */
@Composable
fun ToggleButtonWithContent(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    toggleWidth: Dp = Dp.Unspecified,
    toggleHeight: Dp = 48.dp,
    spacing: Dp = 16.dp,
    content: @Composable (index: Int) -> Unit,
) {
    Column(modifier = modifier) {
        Togglebutton(
            options = options,
            selectedIndex = selectedIndex,
            onOptionSelected = onOptionSelected,
            width = toggleWidth,
            height = toggleHeight,
        )
        Spacer(Modifier.height(spacing))
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(350)) { it * direction / 3 } + fadeIn(tween(350)))
                    .togetherWith(
                        slideOutHorizontally(tween(350)) { -it * direction / 3 } + fadeOut(tween(200))
                    )
            },
            label = "toggleContent"
        ) { index ->
            content(index)
        }
    }
}

/** Preview of [Togglebutton] with three options. */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SegmentedTogglePreview() {
    var selected by remember { mutableIntStateOf(0) }
    Box(Modifier.padding(16.dp)) {
        Togglebutton(
            options = listOf("Entrant", "Owner", "Admin"),
            selectedIndex = selected,
            onOptionSelected = { selected = it }
        )
    }
}

/** Preview of [ToggleButtonWithContent] with two options. */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SegmentedToggleWithContentPreview() {
    var selected by remember { mutableIntStateOf(0) }
    Box(Modifier.padding(16.dp)) {
        ToggleButtonWithContent(
            options = listOf("Active", "Past", "Cancelled"),
            selectedIndex = selected,
            onOptionSelected = { selected = it }
        ) {
        }
    }
}

// region Campsite card list preview

/** One toggle option. [pillText] is the label shown on that tab's cards. */
private enum class StayTab(val label: String, val pillText: String) {
    Active("Active", "Confirmed"),
    Past("Past", "Completed"),
    Cancelled("Cancelled", "Cancelled"),
}

private data class SampleStay(
    val id: Int,
    val name: String,
    val location: String,
    val price: String,
    val rating: Double,
    val tab: StayTab,
)

private val sampleStays = listOf(
    SampleStay(1, "Pine Creek Hideaway", "Big Sur, California", "$45", 4.9, StayTab.Active),
    SampleStay(2, "Redwood Meadow", "Felton, California", "$38", 4.8, StayTab.Active),
    SampleStay(3, "Lakeside Retreat", "Banff, Alberta", "$65", 4.7, StayTab.Past),
    SampleStay(4, "Aspen Ridge", "Jasper, Alberta", "$52", 4.6, StayTab.Past),
    SampleStay(5, "Cedar Hollow", "Hinton, Alberta", "$40", 4.5, StayTab.Cancelled),
)

/** A scrolling list of cards for one tab, or a message when the tab is empty. */
@Composable
private fun StayList(stays: List<SampleStay>) {
    if (stays.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing here yet", style = AppTextStyles.BodyMedium, color = AppColors.TextSecondary)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(stays, key = { it.id }) { stay ->
            CampsiteCard(
                name = stay.name,
                location = stay.location,
                pricePerNight = stay.price,
                rating = stay.rating,
                containerColor = AppColors.Surface,
                pill = { CampsitePill(stay.tab.pillText) },
                action = when (stay.tab) {
                    StayTab.Active -> ({ RedButton(buttonText = "Cancel reservation", onClick = {}, icon = AppIcons.Cross) })
                    StayTab.Past -> ({ GreenButton(buttonText = "Rate Your Stay", onClick = {}, icon = null) })
                    StayTab.Cancelled -> null
                },
            )
        }
    }
}

/** Toggle with a filtered list of [CampsiteCard]s under each option. */
@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380, heightDp = 800)
@Composable
private fun ToggleWithCampsiteCardsPreview() {
    val tabs = StayTab.entries
    var selected by remember { mutableIntStateOf(0) }
    ToggleButtonWithContent(
        options = tabs.map { it.label },
        selectedIndex = selected,
        onOptionSelected = { selected = it },
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) { index ->
        StayList(sampleStays.filter { it.tab == tabs[index] })
    }
}

// endregion