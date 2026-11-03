package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A form field that is tapped rather than typed in, for example to open a date picker or a
 * dropdown. Looks like [FormTextField]: while empty the label sits where the value goes; once
 * filled the label shrinks above the value.
 *
 * Used by [DatePickerField] and [MultiSelectField].
 *
 * @param label Field name, shown uppercase.
 * @param value What has been chosen, or null if nothing yet.
 * @param onClick Called when the field is tapped.
 * @param modifier [Modifier] applied to the field. It fills the available width.
 * @param icon Leading icon, or null for none.
 * @param trailingIcon Icon on the right, for example a dropdown arrow, or null for none.
 * @param error Message shown under the field with a red border, or null when the value is fine.
 *   An empty string gives the red border without a message (for callers that draw it themselves).
 * @param enabled False greys the field out and ignores taps.
 */
@Composable
fun FormSelectField(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (error != null) AppColors.Danger else AppColors.OutlineField
    val contentAlpha = if (enabled) 1f else 0.5f

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(shape)
                .background(AppColors.FillField, shape)
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = AppColors.TextOnPrimary.copy(alpha = contentAlpha), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (value == null) {
                    Text(label.uppercase(), style = AppTextStyles.FieldContent, color = AppColors.OutlineField.copy(alpha = 0.7f * contentAlpha))
                } else {
                    Text(label.uppercase(), style = AppTextStyles.FieldLabel, color = AppColors.Secondary)
                    Text(
                        text = value,
                        style = AppTextStyles.ValueMedium,
                        color = AppColors.TextOnPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, tint = AppColors.TextOnPrimary.copy(alpha = contentAlpha), modifier = Modifier.size(24.dp))
            }
        }
        if (!error.isNullOrEmpty()) {
            Text(error, style = AppTextStyles.Caption, color = AppColors.Danger, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412)
@Composable
private fun FormSelectFieldPreview() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormSelectField(label = "Open date", value = null, onClick = {}, icon = AppIcons.Calendar)
        FormSelectField(label = "Close date", value = "Mon, 5 Oct 2026", onClick = {}, icon = AppIcons.Calendar)
        FormSelectField(
            label = "Select campsites", value = null, onClick = {}, icon = AppIcons.Campsite,
            trailingIcon = AppIcons.DropdownArrow, error = "Pick at least one campsite",
        )
    }
}
