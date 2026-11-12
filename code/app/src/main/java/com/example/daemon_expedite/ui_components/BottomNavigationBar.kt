
package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation bar shared across the app.
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            labels.indices.forEach { i ->
                val isSelected = i == selectedIndex

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            color = if (isSelected) {
                                AppColors.PrimaryNavSelected
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable(onClick = onClicks[i])
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icons[i],
                        contentDescription = labels[i],
                        tint = if (isSelected) {
                            AppColors.TextOnPrimary
                        } else {
                            AppColors.TextMuted
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = labels[i],
                        style = AppTextStyles.NavLabel,
                        color = if (isSelected) {
                            AppColors.TextOnPrimary
                        } else {
                            AppColors.TextMuted
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
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
