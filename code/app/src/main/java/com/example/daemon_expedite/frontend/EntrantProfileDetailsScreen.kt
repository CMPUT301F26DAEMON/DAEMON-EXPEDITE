
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Shared UI components created by the team
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.InfoCard
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.RedButton

/**
 * Entrant Profile Details screen.
 *
 * Displays personal details, profile photo, role selection,
 * notification preferences and account actions.
 *
 * TODO: Connect user data to Firestore.
 * TODO: Connect navigation callbacks.
 */
@Composable
fun EntrantProfileDetailsScreen(
    name: String = "",
    email: String = "",
    phone: String = "",
    photoResId: Int? = null,
    onBackClick: () -> Unit = {},
    onEditPhotoClick: () -> Unit = {},
    onUpdateProfileClick: () -> Unit = {},
    onOwnerViewClick: () -> Unit = {},
    onDeleteProfileClick: () -> Unit = {},
    onPoliciesClick: () -> Unit = {},
    onNotificationsChanged: (Boolean) -> Unit = {}
) {
    var notificationsEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    var showDeleteDialog by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        Spacer(Modifier.height(16.dp))

        // Component 1: Header
        ProfileHeader(onBackClick)

        Spacer(Modifier.height(24.dp))

        // Component 2: Profile picture
        ProfilePhoto(
            photoResId = photoResId,
            onEditPhotoClick = onEditPhotoClick
        )

        Spacer(Modifier.height(16.dp))

        // Component 3: Personal information
        ProfileInformation(
            name = name,
            email = email,
            phone = phone
        )

        Spacer(Modifier.height(16.dp))

        // Component 4: Update Profile button
        ProfileUpdateButton(onUpdateProfileClick)

        Spacer(Modifier.height(16.dp))

        // Component 5: Entrant/Owner selector
        ProfileRoleSelector(onOwnerViewClick)

        Spacer(Modifier.height(16.dp))

        // Component 6: Notifications
        ProfileNotifications(
            checked = notificationsEnabled,
            onCheckedChange = { enabled ->
                notificationsEnabled = enabled
                onNotificationsChanged(enabled)
            }
        )

        Spacer(Modifier.height(16.dp))

        // Component 7: Account Actions
        ProfileAccountActions(
            onDeleteClick = {
                showDeleteDialog = true
            },
            onPoliciesClick = onPoliciesClick
        )

        Spacer(Modifier.height(24.dp))
    }

    // Component 8: Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Delete Profile?")
            },
            text = {
                Text(
                    "Are you sure you want to delete your profile?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteProfileClick()
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = AppColors.Danger
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary
        )
    }
}

/**
 * Component 1: Profile header.
 * Uses the team's existing BackButton.
 */
@Composable
private fun ProfileHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(
            onClick = onBackClick
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Profile",
            style = AppTextStyles.HeadingSmall,
            color = AppColors.TextOnPrimary
        )
    }
}

/**
 * Component 2: Circular profile picture
 * with an edit pencil button.
 */
@Composable
private fun ProfilePhoto(
    photoResId: Int?,
    onEditPhotoClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(112.dp),
            contentAlignment = Alignment.Center
        ) {

            // Profile picture
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AppColors.Surface)
                    .border(
                        width = 1.dp,
                        color = AppColors.OutlineStrong,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (photoResId != null) {
                    Image(
                        painter = painterResource(photoResId),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = AppIcons.Profile,
                        contentDescription = "Default profile picture",
                        modifier = Modifier.size(50.dp),
                        tint = AppColors.TextSecondary
                    )
                }
            }

            // Edit profile picture button
            IconButton(
                onClick = onEditPhotoClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .background(
                        color = AppColors.Primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = AppIcons.EditPencil,
                    contentDescription = "Edit profile photo",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Add a profile photo",
            style = AppTextStyles.Caption,
            color = AppColors.TextSecondary
        )
    }
}

/**
 * Component 3: Personal information cards.
 * Uses the team's existing InfoCard component.
 */
@Composable
private fun ProfileInformation(
    name: String,
    email: String,
    phone: String
) {
    // Name
    InfoCard(
        label = "Name",
        value = name,
        icon = AppIcons.Profile,
        showDelete = false,
        showEdit = false
    )

    Spacer(Modifier.height(15.dp))

    // Email
    InfoCard(
        label = "Email",
        value = email,
        icon = AppIcons.Email,
        showDelete = false,
        showEdit = false
    )

    Spacer(Modifier.height(10.dp))

    // Phone number
    InfoCard(
        label = "Phone Number",
        value = phone,
        icon = AppIcons.Phone,
        showDelete = false,
        showEdit = false
    )
}

/**
 * Component 4: Update Profile button.
 * Uses the team's existing GreenButton component.
 */

@Composable
private fun ProfileUpdateButton(
    onUpdateClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        GreenButton(
            onClick = onUpdateClick,
            icon = Icons.Default.ArrowForward,
            buttonText = "Update Profile",
            customWidth = maxWidth.value.toInt(),
            customHeight = 56
        )
    }
}

/**
 * Component 5: Switch between Entrant and Owner views.
 */
@Composable
private fun ProfileRoleSelector(
    onOwnerViewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // Entrant View - selected
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Primary)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Entrant View",
                style = AppTextStyles.TabLabel,
                color = Color.White
            )
        }

        // Owner View - unselected
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOwnerViewClick)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Owner View",
                style = AppTextStyles.TabLabel,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Component 6: Notification preferences toggle.
 */
@Composable
private fun ProfileNotifications(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .padding(
                horizontal = 14.dp,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Notifications",
            style = AppTextStyles.TabLabel,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AppColors.Primary,
                checkedThumbColor = Color.White
            )
        )
    }
}

/**
 * Component 7: Account actions.
 * Uses the team's existing RedButton.
 */
@Composable
private fun ProfileAccountActions(
    onDeleteClick: () -> Unit,
    onPoliciesClick: () -> Unit
) {
    HorizontalDivider(
        color = AppColors.OutlineSubtle
    )

    Spacer(Modifier.height(14.dp))

    Text(
        text = "ACCOUNT ACTIONS",
        style = AppTextStyles.SectionLabel,
        color = AppColors.TextSecondary
    )

    Spacer(Modifier.height(10.dp))

    // Delete Profile button
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        RedButton(
            onClick = onDeleteClick,
            icon = AppIcons.Dustbin,
            buttonText = "Delete Profile",
            customWidth = maxWidth.value.toInt(),
            customHeight = 56
        )
    }

    Spacer(Modifier.height(12.dp))

    // App Policies
    Text(
        text = "App Policies",
        style = AppTextStyles.Caption,
        color = AppColors.TextSecondary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onPoliciesClick)
            .padding(6.dp)
    )
}

/**
 * Compose Preview for Android Studio.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514
)
@Composable
private fun EntrantProfileDetailsPreview() {
    EntrantProfileDetailsScreen()
}