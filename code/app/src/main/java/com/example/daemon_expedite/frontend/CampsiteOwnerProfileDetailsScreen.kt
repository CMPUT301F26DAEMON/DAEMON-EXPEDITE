package com.example.daemon_expedite.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.daemon_expedite.ui_components.AppIcons

// Temporary screen-specific colors.
// Replace with matching AppColors values from the project.
private val ScreenBg = Color(0xFF101412)
private val CardBg = Color(0xFF202522)
private val CardBorder = Color(0xFF3B423E)
private val AccentGreen = Color(0xFF4B8064)
private val MutedText = Color(0xFF9AA9A0)
private val DeleteRed = Color(0xFFDB7973)

@Composable
fun CampsiteOwnerProfileDetailsScreen(
    onBackClick: () -> Unit = {},
    onEntrantViewClick: () -> Unit = {},
    onDeleteProfileClick: () -> Unit = {},
    profileImage: Painter? = null
) {
    var ownerSelected by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(CardBg, CircleShape)
            ) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = "EXPEDITE",
                color = Color.White,
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Profile",
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp
            )
        }

        Spacer(Modifier.height(32.dp))

        // Profile photo
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(AccentGreen),
            contentAlignment = Alignment.Center
        ) {
            if (profileImage != null) {
                Image(
                    painter = profileImage,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = AppIcons.Profile,
                    contentDescription = "Profile placeholder",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Profile information
        ProfileInfoCard(
            icon = AppIcons.Profile,
            label = "NAME",
            value = ""
        )

        Spacer(Modifier.height(9.dp))

        ProfileInfoCard(
            icon = AppIcons.Email,
            label = "EMAIL",
            value = ""
        )

        Spacer(Modifier.height(9.dp))

        ProfileInfoCard(
            icon = AppIcons.Phone,
            label = "PHONE NUMBER",
            value = ""
        )

        Spacer(Modifier.height(25.dp))

        Text(
            text = "Pick a Role",
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // Entrant / Campsite Owner selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    CardBorder,
                    RoundedCornerShape(12.dp)
                )
                .background(CardBg, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            RoleOption(
                label = "Entrant View",
                selected = !ownerSelected,
                modifier = Modifier.weight(1f),
                onClick = {
                    ownerSelected = false
                    onEntrantViewClick()
                }
            )

            RoleOption(
                label = "Campsite Owner View",
                selected = ownerSelected,
                modifier = Modifier.weight(1f),
                onClick = { ownerSelected = true }
            )
        }

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(color = CardBorder)

        Spacer(Modifier.height(10.dp))

        Text(
            text = "ACCOUNT ACTIONS",
            color = MutedText,
            fontSize = 10.sp,
            letterSpacing = 0.7.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DeleteRed),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CardBg,
                contentColor = DeleteRed
            )
        ) {
            Icon(
                imageVector = AppIcons.Dustbin,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )

            Spacer(Modifier.width(9.dp))

            Text(
                text = "Delete Profile",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }

    // Confirmation before requesting account deletion.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Profile?", color = Color.White)
            },
            text = {
                Text(
                    "Are you sure you want to delete your profile?",
                    color = MutedText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteProfileClick()
                    }
                ) {
                    Text("Delete", color = DeleteRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = CardBg
        )
    }
}

@Composable
private fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 67.dp)
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                CardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(14.dp))

        Column {
            Text(
                text = label,
                color = MutedText,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun RoleOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) AccentGreen else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OwnerProfilePreview() {
    CampsiteOwnerProfileDetailsScreen()
}


