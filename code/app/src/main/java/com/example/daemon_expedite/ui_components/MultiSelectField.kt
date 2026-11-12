// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * A form field that opens a dropdown of checkboxes, for picking several options.
 *
 * The menu opens just under the field, as wide as it, with rounded corners. It shows up to
 * [MAX_VISIBLE_OPTIONS] rows and scrolls for more. It stays open while ticking, so several
 * options can be picked in one go.
 *
 * ```
 * MultiSelectField(
 *     label = "Select campsites",
 *     options = campsites,
 *     selected = picked,
 *     optionLabel = { it.name },
 *     onToggle = { c -> picked = if (c in picked) picked - c else picked + c },
 * )
 * ```
 *
 * @param label Field name, shown uppercase.
 * @param options Everything that can be picked, in the order shown.
 * @param selected The options currently picked.
 * @param optionLabel Text shown for an option.
 * @param onToggle Called with an option when its row or checkbox is tapped.
 * @param modifier [Modifier] applied to the field.
 * @param icon Leading icon, or null for none.
 * @param emptyText Shown instead of a value when there are no options; the field is disabled.
 * @param error Message shown under the field, or null.
 * @param isOptionEnabled False for options that can't be changed; their checkbox is greyed out.
 */
@Composable
fun <T> MultiSelectField(
    label: String,
    options: List<T>,
    selected: Set<T>,
    optionLabel: (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emptyText: String = "Nothing to choose from",
    error: String? = null,
    isOptionEnabled: (T) -> Boolean = { true },
) {
    var open by rememberSaveable { mutableStateOf(false) }
    // Options in the order they are listed, so the summary doesn't jump around as boxes are ticked.
    val pickedLabels = remember(options, selected) { options.filter { it in selected }.map(optionLabel) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // The menu is anchored to this box (just the field), so it opens right under the field.
        BoxWithConstraints {
            val fieldWidth = maxWidth
            FormSelectField(
                label = label,
                value = when {
                    options.isEmpty() -> emptyText
                    pickedLabels.isEmpty() -> null
                    else -> pickedLabels.joinToString(", ")
                },
                onClick = { open = true },
                icon = icon,
                trailingIcon = AppIcons.DropdownArrow,
                enabled = options.isNotEmpty(),
                // Red border only; the message is drawn below, outside the menu's anchor.
                error = error?.let { "" },
            )
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier
                    .width(fieldWidth)
                    // Room for MAX_VISIBLE_OPTIONS rows plus the menu's own top and bottom padding.
                    .heightIn(max = OPTION_HEIGHT * MAX_VISIBLE_OPTIONS + MENU_PADDING * 2),
                offset = DpOffset(0.dp, 6.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = AppColors.Surface,
                border = BorderStroke(1.dp, AppColors.OutlineStrong),
            ) {
                options.forEach { option ->
                    val optionEnabled = isOptionEnabled(option)
                    DropdownMenuItem(
                        modifier = Modifier.height(OPTION_HEIGHT),
                        enabled = optionEnabled,
                        text = {
                            Text(
                                optionLabel(option),
                                style = AppTextStyles.ValueMedium,
                                color = if (optionEnabled) AppColors.TextPrimary else AppColors.TextDisabled,
                            )
                        },
                        onClick = { onToggle(option) },
                        leadingIcon = {
                            Checkbox(
                                checked = option in selected,
                                onCheckedChange = { onToggle(option) },
                                enabled = optionEnabled,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppColors.Primary,
                                    uncheckedColor = AppColors.Secondary,
                                    checkmarkColor = AppColors.TextOnPrimary,
                                    disabledCheckedColor = AppColors.PrimaryMuted,
                                ),
                            )
                        },
                    )
                }
            }
        }
        if (!error.isNullOrEmpty()) {
            Text(error, style = AppTextStyles.Caption, color = AppColors.Danger, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Rows shown before the menu starts scrolling. */
const val MAX_VISIBLE_OPTIONS = 4

private val OPTION_HEIGHT = 52.dp

/** Material's DropdownMenu pads its list by 8 dp at the top and bottom. */
private val MENU_PADDING = 8.dp

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412)
@Composable
private fun MultiSelectFieldPreview() {
    MultiSelectField(
        label = "Select campsites",
        options = listOf("Pine Creek Hideaway", "Maple Campsite", "Lakeview"),
        selected = setOf("Pine Creek Hideaway", "Lakeview"),
        optionLabel = { it },
        onToggle = {},
        icon = AppIcons.Campsite,
    )
}
