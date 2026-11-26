// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Make the admin messages dropdown a UI component", 2026-11-26.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A compact field with a label above it that opens a dropdown for picking one option.
 * The menu closes as soon as an option is picked. With no options the field can't be opened.
 *
 * For picking several options use [MultiSelectField] instead.
 *
 * ```
 * DropdownSelectField(
 *     label = "Campsite owner",
 *     selectedText = selectedOwner?.name ?: "Select owner",
 *     options = owners,
 *     optionLabel = { it.name },
 *     onSelect = { selectedOwnerId = it.id },
 * )
 * ```
 *
 * @param label Field name, shown uppercase above the field.
 * @param selectedText Text shown in the field: the picked option, or a hint like "Select owner".
 * @param options Everything that can be picked, in the order shown.
 * @param optionLabel Text shown for an option in the menu.
 * @param onSelect Called with the option that was tapped.
 * @param modifier [Modifier] applied to the whole field, label included.
 */
@Composable
fun <T> DropdownSelectField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = AppTextStyles.FieldLabel,
            color = AppColors.Secondary,
        )

        Spacer(modifier = Modifier.height(5.dp))

        // The menu is anchored to this box (just the field), so it opens right under the field.
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(shape)
                    .background(AppColors.FillField)
                    .border(1.dp, AppColors.OutlineField, shape)
                    .clickable(enabled = options.isNotEmpty(), role = Role.DropdownList) { expanded = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedText,
                    style = AppTextStyles.BodySmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = "Select $label",
                    tint = AppColors.Secondary,
                    modifier = Modifier
                        .size(17.dp)
                        .rotate(90f), // points down
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(AppColors.SurfaceDeep)
                    .widthIn(min = 150.dp, max = 250.dp),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel(option),
                                style = AppTextStyles.BodySmall,
                                color = AppColors.TextPrimary,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 393)
@Composable
private fun DropdownSelectFieldPreview() {
    Row(
        modifier = Modifier.padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DropdownSelectField(
            label = "Campsite owner",
            selectedText = "Oliver Hayes",
            options = listOf("Oliver Hayes", "Elena Park"),
            optionLabel = { it },
            onSelect = {},
            modifier = Modifier.weight(1f),
        )
        DropdownSelectField(
            label = "Entrant",
            selectedText = "Select entrant",
            options = emptyList<String>(),
            optionLabel = { it },
            onSelect = {},
            modifier = Modifier.weight(1f),
        )
    }
}
