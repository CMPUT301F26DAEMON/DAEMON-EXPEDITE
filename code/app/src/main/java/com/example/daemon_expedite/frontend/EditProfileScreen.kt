
package com.example.daemon_expedite.frontend


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.*


@Composable
fun EditProfileScreen(
    profileImageUrl: String? = null,
    name: String = "",
    email: String = "",
    phoneNumber: String = "",
    onBackClick: () -> Unit = {},
    onProfilePhotoClick: () -> Unit = {},
    // Gets the edited name, email and phone number when "Save changes" is tapped.
    onSaveClick: (name: String, email: String, phoneNumber: String) -> Unit = { _, _, _ -> },
    // While true the save button is disabled, so a slow save can't be sent twice.
    isSaving: Boolean = false,
    onCancelClick: () -> Unit = {}
) {

    //editable
    var editableName by rememberSaveable { mutableStateOf(name) }
    var editableEmail by rememberSaveable { mutableStateOf(email) }
    var editablePhone by rememberSaveable { mutableStateOf(phoneNumber) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {

        val fieldWidth = minOf(maxWidth, 356.dp).value.toInt()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {

                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BackButton(
                        onClick = onBackClick
                    )
                }

                Text(
                    text = "Edit profile",
                    style = AppTextStyles.PageTitle,
                    color = AppColors.TextOnPrimary,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            ProfileImage(
                imageUrl = profileImageUrl,
                size = 96.dp,
                onEditClick = onProfilePhotoClick,
                showBorder = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Change profile photo",
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Editable profile information
            Column(
                modifier = Modifier.width(fieldWidth.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Name field
                ProfileTextField(
                    label = "Name",
                    value = editableName,
                    onValueChange = { editableName = it },
                    icon = AppIcons.Profile,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                // Email field
                ProfileTextField(
                    label = "Email",
                    value = editableEmail,
                    onValueChange = { editableEmail = it },
                    icon = AppIcons.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )

                // Phone number field
                ProfileTextField(
                    label = "Phone number",
                    value = editablePhone,
                    onValueChange = { editablePhone = it },
                    icon = AppIcons.Phone,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                )
            }

            Spacer(modifier = Modifier.height(250.dp))
            GreenButton(
                onClick = {
                    if (!isSaving) onSaveClick(editableName.trim(), editableEmail.trim(), editablePhone.trim())
                },
                icon = AppIcons.Tick,
                buttonText = if (isSaving) "Saving…" else "Save changes",
                customHeight =58,
                customWidth = fieldWidth
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
private fun EditProfileScreenPreview() {
    EditProfileScreen()
}
