package com.example.daemon_expedite.frontend

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.ui_components.*


enum class ProfileRole(val title: String) {
    ENTRANT("Entrant"),
    OWNER("Owner"),
    ADMIN("Admin")
}


@Composable
fun ProfileDetailsScreen(
    profileImageUrl: String? = null,
    name: String = "",
    email: String = "",
    phoneNumber: String = "",
    initialRole: ProfileRole = ProfileRole.ENTRANT,
    onBackClick: () -> Unit = {},
    onProfilePhotoClick: () -> Unit = {},
    onPhotoSelected: (Uri) -> Unit = {},
    onUpdateProfile: (String, String, String, ProfileRole) -> Unit =
        { _, _, _, _ -> },
    onDeleteProfile: () -> Unit = {}
) {
    var editableName by rememberSaveable {
        mutableStateOf(name)
    }

    var editableEmail by rememberSaveable {
        mutableStateOf(email)
    }

    var editablePhone by rememberSaveable {
        mutableStateOf(phoneNumber)
    }

    // Roles
    var selectedRole by rememberSaveable {
        mutableStateOf(initialRole)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
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

            // Profile picture
            ProfileImage(
                imageUrl = profileImageUrl,
                size = 124.dp,
                onEditClick = {},
                showBorder = true
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Add a profile photo",
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary,
                modifier = Modifier.clickable {
                    // No action for now
                }
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {

                // Name
                ProfileTextField(
                    label = "Name",
                    value = editableName,
                    onValueChange = { editableName = it },
                    icon = AppIcons.Profile,
                    placeholder = "Enter your name",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                // Email
                ProfileTextField(
                    label = "Email",
                    value = editableEmail,
                    onValueChange = { editableEmail = it },
                    icon = AppIcons.Email,
                    placeholder = "Enter your email",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
                // Phone number
                ProfileTextField(
                    label = "Phone number",
                    value = editablePhone,
                    onValueChange = { editablePhone = it },
                    icon = AppIcons.Phone,
                    placeholder = "Enter your phone number",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                )
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            // Update Profile button
            GreenButton(
                onClick = {},
                icon = Icons.Default.ArrowForward,
                buttonText = "Update Profile",
                customHeight = 58,
                customWidth = contentWidth.value.toInt()
            )

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

            // Role selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        AppColors.SurfaceDisabled,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {

                ProfileRole.entries.forEach { role ->

                    val isSelected = selectedRole == role

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (isSelected) {
                                    AppColors.Primary
                                } else {
                                    AppColors.SurfaceDisabled
                                },
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                selectedRole = role
                            },
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = role.title,
                            fontFamily = AppFonts.Inter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                            color = AppColors.TextOnPrimary
                        )
                    }
                }
            }

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
                onClick = {},
                icon = AppIcons.Dustbin,
                buttonText = "Delete Profile",
                customHeight = 58,
                customWidth = contentWidth.value.toInt()
            )
        }
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
    ProfileDetailsScreen()
}