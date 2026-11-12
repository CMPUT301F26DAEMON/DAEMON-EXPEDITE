package com.example.daemon_expedite.ui_components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Height of the bar's tab row, not counting the system navigation bar below it. */
val BottomNavBarHeight = 66.dp

private val ItemHeight = 56.dp
private val SidePadding = 8.dp

/**
 * Bottom navigation bar shared across the app.
 *
 * The highlight behind the selected tab slides to the newly selected tab, and the icon and
 * label colors fade between selected and unselected. For the slide to show, keep one bar on
 * screen while the content changes (see AppNavigation) instead of drawing a new bar on every
 * screen.
 *
 * @param labels Labels for each navigation tab.
 * @param icons Icons corresponding to each label.
 * @param onClicks Actions performed when tabs are tapped.
 * @param selectedIndex Currently selected tab (-1 means none).
 */
@Composable
fun BottomNavBar(
    labels: List<String>,
    icons: List<ImageVector>,
    onClicks: List<() -> Unit>,
    selectedIndex: Int = -1,
) {
    require(labels.size == icons.size && icons.size == onClicks.size) {
        "labels, icons, and onClicks must all be the same size"
    }

    Surface(
        modifier = Modifier.drawBehind {
            // Figma: 1px top-only border, drawn inside the bar.
            val stroke = 1.dp.toPx()
            drawLine(AppColors.OutlineNav, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
        },
        color = AppColors.SurfaceDeep,
        contentColor = AppColors.TextPrimary
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(BottomNavBarHeight)
                .padding(horizontal = SidePadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            val itemWidth = maxWidth / labels.size

            // Sliding highlight behind the selected tab. While nothing is selected it stays
            // where it was and fades out, so it can slide again from there.
            var lastSelected by remember { mutableIntStateOf(selectedIndex.coerceAtLeast(0)) }
            SideEffect { if (selectedIndex >= 0) lastSelected = selectedIndex }
            val indicatorIndex = if (selectedIndex >= 0) selectedIndex else lastSelected
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * indicatorIndex,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "navIndicatorOffset",
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (selectedIndex >= 0) 1f else 0f,
                label = "navIndicatorAlpha",
            )
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(ItemHeight)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .background(AppColors.PrimaryNavSelected, RoundedCornerShape(16.dp))
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                labels.indices.forEach { i ->
                    val isSelected = i == selectedIndex
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) AppColors.TextOnPrimary else AppColors.TextMuted,
                        label = "navItemColor",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(ItemHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                role = Role.Tab,
                                onClick = onClicks[i],
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icons[i],
                            contentDescription = labels[i],
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = labels[i],
                            style = if (isSelected) AppTextStyles.NavLabel else AppTextStyles.NavLabel.copy(fontWeight = FontWeight.Normal),
                            color = contentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty space the size of [BottomNavBar], including the system navigation bar below it.
 * Put it at the bottom of a screen that has the shared bar drawn on top of it, so the
 * screen's content doesn't end up hidden behind the bar.
 */
@Composable
fun BottomNavBarSpacer() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(BottomNavBarHeight)
    )
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
private fun BottomNavBarPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        BottomNavBar(
            labels = listOf(
                "Explore",
                "Reservations",
                "Lotteries",
                "Messages"
            ),
            icons = listOf(
                AppIcons.Home,
                AppIcons.Reservation,
                AppIcons.LotteryTicket,
                AppIcons.Chat
            ),
            onClicks = listOf({}, {}, {}, {}),
            selectedIndex = 0
        )
    }
}
