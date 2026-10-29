package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme


/**
 * A navigation bar at the bottom of the main screen.
 *
 * Takes 3 parallel lists of equal size
 *
 * @param labels A list of strings where each element is a label for the corresponding icon
 * @param icons A list of ImageVectors where each element is an icon corresponding to the label
 * @param onClicks Action to perform when the corresponding icon is clicked
 */
@Composable
fun BottomNavBar(
    labels: List<String>,
    icons: List<ImageVector>,
    onClicks: List<() -> Unit>,
) {
    require(labels.size == icons.size && icons.size == onClicks.size) {
        "labels, icons, and onClicks must all be the same size"
    }

    Surface(
        contentColor = AppColors.TextOnPrimary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            labels.indices.forEach { i ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconButton(onClick = onClicks[i]) {
                        Icon(
                            imageVector = icons[i],
                            contentDescription = labels[i],
                        )
                    }
                    Text(
                        text = labels[i],
                        fontFamily = AppFonts.Inter
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun BottomNavBarPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        BottomNavBar(
            labels = listOf("Reservations", "Lotteries", "Messages"),
            icons = listOf(
                AppIcons.Reservation,
                AppIcons.LotteryTicket,
                AppIcons.Chat,
            ),
            onClicks = listOf({}, {}, {}))
    }
}