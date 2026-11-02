package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.ui_components.AppFonts.Fraunces
import androidx.compose.material3.Button


/**
 * A customizable yellow filled button used for lottery
 *
 * Displays an icon (optional) alongside a text label
 *
 * @param onClick The callback invoked when the button is clicked.
 * @param icon The icon displayed beside the button text. (is null by default)
 * @param buttonText The text label displayed inside the button.
 * @param customHeight The height of the button in dp. Defaults to 58 dp.
 * @param customWidth The width of the button in dp. Defaults to 356 dp.
 */
@Composable
fun YellowButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    buttonText: String,
    customHeight: Int = 56,
    customWidth: Int = 356
) {
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .width(customWidth.dp)
            .height(customHeight.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppColors.Scrim,
            containerColor = AppColors.Accent

        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = "Danger Button",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(
            text = buttonText,
            fontFamily = Fraunces,
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun YellowButtonPreview() {
    var query by remember { mutableStateOf("") }

    YellowButton(
        onClick = {},
        icon =  AppIcons.Tick,
        buttonText = "Create Lottery"
    )
}