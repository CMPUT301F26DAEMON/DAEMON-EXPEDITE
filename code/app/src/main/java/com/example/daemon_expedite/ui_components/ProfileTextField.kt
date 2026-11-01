package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Field background from the Figma design (white at 7%). */
private val FieldBackground = Color(0x12FFFFFF)

/**
 * Editable profile field from the Edit Profile screen: a leading icon, a small uppercase
 * label, the editable value, and a pencil on the right.
 *
 * Tapping anywhere on the value, or on the pencil, starts editing.
 *
 * ```
 * var name by remember { mutableStateOf(user.name) }
 * ProfileTextField(label = "Name", value = name, onValueChange = { name = it }, icon = Icons.Outlined.Person)
 *
 * ProfileTextField(
 *     label = "Email", value = email, onValueChange = { email = it }, icon = Icons.Outlined.Email,
 *     keyboardType = KeyboardType.Email,
 * )
 * ```
 *
 * @param label Label above the value, for example "Name". Shown in uppercase.
 * @param value Current text of the field.
 * @param onValueChange Called with the new text whenever the user types.
 * @param modifier [Modifier] applied to the field. It fills the available width by default.
 * @param icon Optional leading icon describing the field. Omit for no icon.
 * @param placeholder Text shown in place of the value while it is empty.
 * @param keyboardType Keyboard to show, for example [KeyboardType.Email] or [KeyboardType.Phone].
 * @param imeAction Action button on the keyboard. Use [ImeAction.Done] on the last field.
 * @param keyboardActions What happens when the keyboard action button is pressed.
 * @param enabled When false, the field can't be edited and the pencil is hidden.
 */
@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(FieldBackground, shape)
            .border(BorderStroke(1.dp, AppColors.Secondary), shape)
            .padding(start = 16.dp, end = 4.dp),
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

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = AppTextStyles.Overline.copy(letterSpacing = 0.sp),
                color = AppColors.Secondary,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = AppTextStyles.ValueMedium.copy(color = AppColors.TextOnPrimary),
                cursorBrush = SolidColor(AppColors.Secondary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppTextStyles.ValueMedium,
                            color = AppColors.TextSecondary.copy(alpha = 0.5f),
                        )
                    }
                    innerTextField()
                },
            )
        }

        if (enabled) {
            // 40dp touch target; with the Row's 4dp end padding the 17dp pencil lands where the design has it.
            IconButton(onClick = { focusRequester.requestFocus() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit ${label.lowercase()}",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412)
@Composable
private fun ProfileTextFieldPreview() {
    var name by remember { mutableStateOf("Maya Bennett") }
    var email by remember { mutableStateOf("maya.bennett@example.com") }
    var phone by remember { mutableStateOf("+1 (415) 555-0138") }

    Column(
        modifier = Modifier.padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProfileTextField("Name", name, { name = it }, icon = Icons.Outlined.Person)
        ProfileTextField("Email", email, { email = it }, icon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
        ProfileTextField(
            "Phone number", phone, { phone = it }, icon = Icons.Outlined.Phone,
            keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done,
        )
    }
}

// endregion