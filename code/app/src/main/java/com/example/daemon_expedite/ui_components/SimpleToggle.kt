package com.example.daemon_expedite.ui_components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A customizable toggle button that switches between ON and OFF states.
 *
 * @param isOn Whether the toggle is currently ON.
 * @param onStateChange Callback invoked when the toggle is clicked,
 * returning 1 when switched ON and 0 when switched OFF.
 */
@Composable
fun SimpleToggle(
    isOn: Boolean,
    onStateChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val togglePosition by animateDpAsState(
        targetValue = if (isOn) 22.dp else 2.dp,
        label = "Toggle Animation"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isOn) AppColors.Primary else AppColors.Secondary
            )
            .clickable {
                onStateChange(if (isOn) 0 else 1)
            }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = togglePosition)
                .size(24.dp)
                .background(AppColors.TextOnPrimary, CircleShape)
                .align(Alignment.CenterStart)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SimpleTogglePreview() {
    var toggleState by remember { mutableIntStateOf(0) }

    SimpleToggle(
        isOn = toggleState == 1,
        onStateChange = { toggleState = it }
    )
}
