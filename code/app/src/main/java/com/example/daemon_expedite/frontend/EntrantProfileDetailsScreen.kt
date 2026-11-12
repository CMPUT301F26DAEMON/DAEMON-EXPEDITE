package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.InfoCard
import com.example.daemon_expedite.ui_components.ProfileImage
import com.example.daemon_expedite.ui_components.RedButton
import com.example.daemon_expedite.ui_components.ScreenHeader
import com.example.daemon_expedite.ui_components.SimpleToggle

/**
 * Entrant Profile Details screen, built from the "Entrant Profile Details (v2)" Figma frame.
 *
 * Shows the profile photo, personal details, the role switch, the notifications setting
 * and account actions. Deleting the profile asks for confirmation first.
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
    ) {
        ScreenHeader(title = "Profile", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Photo, details and the edit button sit a little further apart than the sections below.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ProfilePhoto(photoUrl = photoUrl, onEditPhotoClick = onEditPhotoClick)
                ProfileInformation(name = name, email = email, phone = phone)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    GreenButton(
                        onClick = onUpdateProfileClick,
                        icon = AppIcons.EditPencil,
                        buttonText = "Edit profile",
                        customWidth = maxWidth.value.toInt(),
                        customHeight = 58
                    )
                }
            }

            RoleSelector(
                current = ProfileRole.ENTRANT,
                isAdmin = isAdmin,
                onRoleSelected = onRoleSelected,
                modifier = Modifier.fillMaxWidth()
            )

            ProfileNotifications(
                checked = notificationsChecked,
                onCheckedChange = { enabled ->
                    notificationsChecked = enabled
                    onNotificationsChanged(enabled)
                }
            )

            ProfileAccountActions(
                onDeleteClick = { showDeleteDialog = true },
                onPoliciesClick = onPoliciesClick
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete your profile?",
            message = "This permanently deletes your profile. This can't be undone.",
            confirmText = "Delete profile",
            onConfirm = {
                showDeleteDialog = false
                onDeleteProfileClick()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

/** Circular profile picture with the pencil button, and a caption under it. */
@Composable
private fun ProfilePhoto(
    photoUrl: String?,
    onEditPhotoClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Shared avatar component: loads the photo, shows the default image when there is
        // none (or it fails to load), and draws the edit pencil button.
        ProfileImage(
            imageUrl = photoUrl,
            size = 128.dp,
            onEditClick = onEditPhotoClick,
            contentDescription = "Profile picture"
        )

        Text(
            text = if (photoUrl == null) "Add a profile photo" else "Change profile photo",
            style = AppTextStyles.BodySmall,
            color = AppColors.TextSecondary
        )
    }
}

/** Name, email and phone number, read-only. */
@Composable
private fun ProfileInformation(
    name: String,
    email: String,
    phone: String
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.value.toInt()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoCard(label = "Name", value = name, icon = AppIcons.Profile, showDelete = false, showEdit = false, customWidth = width)
            InfoCard(label = "Email", value = email, icon = AppIcons.Email, showDelete = false, showEdit = false, customWidth = width)
            InfoCard(label = "Phone number", value = phone, icon = AppIcons.Phone, showDelete = false, showEdit = false, customWidth = width)
        }
    }
}

/** Notifications on/off, in an outlined row. */
@Composable
private fun ProfileNotifications(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, AppColors.OutlineButton, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Notifications",
            style = AppTextStyles.ValueEmphasis,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        SimpleToggle(
            isOn = checked,
            onStateChange = { onCheckedChange(it == 1) }
        )
    }
}

/** "Account actions": delete profile, and a link to the app policies. */
@Composable
private fun ProfileAccountActions(
    onDeleteClick: () -> Unit,
    onPoliciesClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = AppColors.OutlineSubtle)

        Text(
            text = "ACCOUNT ACTIONS",
            style = AppTextStyles.SectionLabel,
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            RedButton(
                onClick = onDeleteClick,
                icon = AppIcons.Dustbin,
                buttonText = "Delete Profile",
                customWidth = maxWidth.value.toInt(),
                customHeight = 58
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onPoliciesClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = AppIcons.Info,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "App Policies",
                style = AppTextStyles.BodySmall,
                color = AppColors.TextSecondary,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412, heightDp = 1000)
@Composable
private fun EntrantProfileDetailsPreview() {
    EntrantProfileDetailsScreen(
        name = "Maya Bennett",
        email = "maya.bennett@example.com",
        phone = "+1 (415) 555-0138",
    )
}
