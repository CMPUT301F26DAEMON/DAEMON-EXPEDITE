
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.daemon_expedite.ui_components.ProfileImage
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
    photoUrl: String? = null,
    notificationsEnabled: Boolean = true,
    onBackClick: () -> Unit = {},
    onEditPhotoClick: () -> Unit = {},
    onUpdateProfileClick: () -> Unit = {},
    // Adds the Admin option to the role toggle. Only true for users in the "admins" collection.
    isAdmin: Boolean = false,
    // Called when another role is tapped in the role toggle (switches views straight away).
    onRoleSelected: (ProfileRole) -> Unit = {},
    onDeleteProfileClick: () -> Unit = {},
    onPoliciesClick: () -> Unit = {},
    onNotificationsChanged: (Boolean) -> Unit = {}
) {
    // Starts from the user's saved setting; keyed so it resets if a different value is passed in.
    var notificationsChecked by rememberSaveable(notificationsEnabled) {
        mutableStateOf(notificationsEnabled)
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
            photoUrl = photoUrl,
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
        RoleSelector(
            current = ProfileRole.ENTRANT,
            isAdmin = isAdmin,
            onRoleSelected = onRoleSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Component 6: Notifications
        ProfileNotifications(
            checked = notificationsChecked,
            onCheckedChange = { enabled ->
                notificationsChecked = enabled
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
    photoUrl: String?,
    onEditPhotoClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shared avatar component: loads the photo, shows the default image when there is
        // none (or it fails to load), and draws the edit pencil button.
        ProfileImage(
            imageUrl = photoUrl,
            size = 112.dp,
            onEditClick = onEditPhotoClick,
            contentDescription = "Profile picture"
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (photoUrl == null) "Add a profile photo" else "Change profile photo",
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