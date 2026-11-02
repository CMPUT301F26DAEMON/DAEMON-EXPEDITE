package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Text field from the Register campsite form: a leading icon and an uppercase label that
 * sits where the text goes while the field is empty, then moves above the text once typed in.
 *
 * ```
 * var name by remember { mutableStateOf("") }
 * FormTextField(label = "Campsite name", value = name, onValueChange = { name = it }, icon = AppIcons.Campsite)
 * ```
 *
 * @param label Uppercase label, for example "Campsite name".
 * @param value Current text.
 * @param onValueChange Called with the new text whenever the user types.
 * @param modifier [Modifier] applied to the field. It fills the available width.
 * @param icon Leading icon, or null for none.
 * @param error Message shown under the field with a red border, or null when the value is fine.
 * @param singleLine False lets the text wrap onto more lines (descriptions, rules).
 * @param keyboardType Keyboard to show, for example [KeyboardType.Number].
 * @param imeAction Keyboard action button.
 * @param containerColor Field fill. The design uses [AppColors.Surface] for the address field.
 */
@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    error: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    containerColor: Color = AppColors.FillField,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (error != null) AppColors.Danger else AppColors.OutlineField

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .background(containerColor, shape)
                .border(1.dp, borderColor, shape)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.TextOnPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = 1,
                textStyle = AppTextStyles.ValueMedium.copy(color = AppColors.TextOnPrimary),
                cursorBrush = SolidColor(AppColors.Secondary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (value.isEmpty()) {
                            // Empty: the label takes the text's place, as in the Figma.
                            Row {
                                Text(
                                    text = label.uppercase(),
                                    style = AppTextStyles.FieldContent,
                                    color = AppColors.OutlineField,
                                )
                            }
                        } else {
                            Text(
                                text = label.uppercase(),
                                style = AppTextStyles.FieldLabel,
                                color = AppColors.Secondary,
                            )
                        }
                        // Always composed so the field keeps focus while the label moves.
                        Row(modifier = Modifier.heightIn(min = if (value.isEmpty()) 0.dp else 20.dp)) {
                            innerTextField()
                        }
                    }
                },
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = AppTextStyles.Caption,
                color = AppColors.Danger,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412)
@Composable
private fun FormTextFieldPreview() {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("45") }
    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        FormTextField("Campsite name", name, { name = it }, icon = AppIcons.Campsite)
        FormTextField("Price per night", price, { price = it }, icon = AppIcons.Price, keyboardType = KeyboardType.Number)
        FormTextField("Maximum occupancy", "", {}, icon = AppIcons.Guests, error = "Enter a number above 0")
    }
}

// endregion
