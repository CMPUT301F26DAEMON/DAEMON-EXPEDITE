package com.example.daemon_expedite.ui_components

import android.R.attr.fontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.ui_components.AppColors.TextSecondary
import androidx.compose.ui.text.font.FontWeight

/**
 * A card with label and corresponding information.
 *
 * Displays an icon (optional) alongside a text label
 * Displays delete and/or edit icon button
 *
 * @param label The label for what the card holds
 * @param value The actual content of the card
 * @param icon The icon on the left of the value indicating what the card is. null by default.
 * @param showDelete Boolean to decide whether to show delete button on the right
 * @param showEdit Boolean to decide whether to show edit button on the right
 * @param onEditClick Action to perform when the edit button is clicked.
 * @param onDeleteClick Action to perform when the delete button is clicked.
 * @param customHeight The height of the button in dp. Defaults to 58 dp.
 * @param customWidth The width of the button in dp. Defaults to 356 dp.
 *
 */
@Composable
fun InfoCard(
    label: String,
    value: String,
    icon: ImageVector? = null,
    showDelete: Boolean,
    showEdit: Boolean,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    customHeight: Int = 72,
    customWidth: Int = 356
) {
    Card(
        modifier = Modifier
            .width(customWidth.dp)
            .height(customHeight.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.FillSubtle),
        border = BorderStroke(1.dp, AppColors.OutlineSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.TextOnPrimary
                )

                Spacer(Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    fontFamily = AppFonts.Inter,
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = value,
                    fontFamily = AppFonts.Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = AppColors.TextPrimary
                )
            }

            if (showEdit) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = AppIcons.EditPencil,
                        contentDescription = "Edit",
                        tint = AppColors.TextOnPrimary
                    )
                }
            }

            if (showDelete) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = AppIcons.Dustbin,
                        contentDescription = "Delete",
                        tint = AppColors.Danger
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun InfoCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard("Name", "Maya Bennett", showDelete = false, showEdit = false)
            InfoCard("Name", "Maya Bennett", Icons.Default.Person, showDelete = false, showEdit = true)
            InfoCard("Name", "Maya Bennett", Icons.Default.Person, showDelete = true, showEdit = false)
        }
    }
}