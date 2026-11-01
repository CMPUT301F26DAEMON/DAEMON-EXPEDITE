package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors.Danger
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.ui_components.AppColors.LowOpacityDanger
import com.example.daemon_expedite.ui_components.AppFonts.Fraunces


/**
 * A customizable red outlined button intended for destructive actions, such as deleting a profile.
 *
 * Displays an icon alongside a text label
 *
 * @param onClick The callback invoked when the button is clicked.
 * @param icon The icon displayed beside the button text. Null for a text-only button.
 * @param buttonText The text label displayed inside the button.
 * @param customHeight The height of the button in dp. Defaults to 58 dp.
 * @param customWidth The width of the button in dp. Defaults to 356 dp.
 */
@Composable
fun RedButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    buttonText: String,
    customHeight: Int = 56,
    customWidth: Int = 356
) {
    OutlinedButton(
        onClick = { onClick() },
        modifier = Modifier
            .width(customWidth.dp)
            .height(customHeight.dp),
        border = BorderStroke(width = 1.dp, color = Danger),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Danger,
            containerColor = LowOpacityDanger

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
fun RedButtonPreview() {
    var query by remember { mutableStateOf("") }

    RedButton(
        onClick = {},
        icon =  AppIcons.Dustbin,
        buttonText = "Yes, delete profile"
    )
}