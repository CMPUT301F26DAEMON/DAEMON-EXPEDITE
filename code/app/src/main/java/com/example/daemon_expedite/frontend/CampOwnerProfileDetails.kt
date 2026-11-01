package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.*

/**
 * Profile page for the owner view.
 *
 * Read-only: the photo, name, email and phone belong to the account (the entrant profile), so
 * they are only shown here. Changing them is done from the entrant profile. This page only
 * switches roles and deletes the owner side of the account.
 *
 * @param profileImageUrl URL of the user's profile photo, or null for the default image.
 * @param isAdmin Adds the Admin option to the role toggle. Only true for users in "admins".
 * @param onRoleSelected Called when another role is tapped (switches views straight away).
 * @param onDeleteProfile Called after the user confirms deleting the owner profile.
 */
@Composable
fun ProfileDetailsScreen(
    profileImageUrl: String? = null,
    name: String = "",
    email: String = "",
    phoneNumber: String = "",
    isAdmin: Boolean = false,
    onBackClick: () -> Unit = {},
    onRoleSelected: (ProfileRole) -> Unit = {},
    onDeleteProfile: () -> Unit = {}
) {
    var showDeleteDialog by rememberSaveable {
        mutableStateOf(false)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {

        val contentWidth = minOf(maxWidth, 356.dp)

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(contentWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 12.dp,
                    bottom = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                BackButton(
                    onClick = onBackClick
                )
                Spacer(
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Profile",
                    style = AppTextStyles.PageTitle,
                    color = AppColors.TextOnPrimary
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // Profile picture (no edit button: photos are changed from the entrant profile)
            ProfileImage(
                imageUrl = profileImageUrl,
                size = 124.dp,
                showBorder = true
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "To change your photo or details, switch to Entrant",
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            // Read-only details, using the same cards as the entrant profile
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                InfoCard(
                    label = "Name",
                    value = name,
                    icon = AppIcons.Profile,
                    showDelete = false,
                    showEdit = false,
                    customWidth = contentWidth.value.toInt()
                )
                InfoCard(
                    label = "Email",
                    value = email,
                    icon = AppIcons.Email,
                    showDelete = false,
                    showEdit = false,
                    customWidth = contentWidth.value.toInt()
                )
                InfoCard(
                    label = "Phone Number",
                    value = phoneNumber,
                    icon = AppIcons.Phone,
                    showDelete = false,
                    showEdit = false,
                    customWidth = contentWidth.value.toInt()
                )
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            // Role selection heading
            Text(
                text = "Pick a Role",
                style = AppTextStyles.ButtonLabel,
                color = AppColors.TextOnPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            // Role selector (Entrant / Owner, plus Admin for admins)
            RoleSelector(
                current = ProfileRole.OWNER,
                isAdmin = isAdmin,
                onRoleSelected = onRoleSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "ACCOUNT ACTIONS",
                style = AppTextStyles.Overline,
                color = AppColors.TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // Delete Profile button
            RedButton(
                onClick = { showDeleteDialog = true },
                icon = AppIcons.Dustbin,
                buttonText = "Delete Profile",
                customHeight = 58,
                customWidth = contentWidth.value.toInt()
            )
        }
    }

    // Asks before deleting. This only removes the owner side; the entrant profile is kept.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete owner profile?") },
            text = {
                Text(
                    "Your campsites and their lotteries and reservations will be deleted. " +
                        "Your entrant profile stays, and you can become an owner again later."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteProfile()
                }) {
                    Text(text = "Delete", color = AppColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.TextPrimary,
            textContentColor = AppColors.TextSecondary
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 402,
    heightDp = 850
)
@Composable
private fun ProfileDetailsScreenPreview() {
    ProfileDetailsScreen(
        name = "Armaan",
        email = "armaan@example.com",
        phoneNumber = "780 555 0199"
    )
}
