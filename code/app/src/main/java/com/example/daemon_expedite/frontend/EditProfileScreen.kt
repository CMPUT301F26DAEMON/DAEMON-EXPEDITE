package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.*

/**
 * Edit profile screen, built from the "Edit Profile (v2)" Figma frame: the profile photo,
 * editable name, email and phone number, and Save / Cancel pinned to the bottom.
 */
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
    onCancelClick: () -> Unit = {},
) {
    var editableName by rememberSaveable { mutableStateOf(name) }
    var editableEmail by rememberSaveable { mutableStateOf(email) }
    var editablePhone by rememberSaveable { mutableStateOf(phoneNumber) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ScreenHeader(title = "Edit profile", onBackClick = onBackClick)

        // Photo and fields scroll; the buttons stay at the bottom (above the keyboard when it's open).
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileImage(
                    imageUrl = profileImageUrl,
                    size = 128.dp,
                    onEditClick = onProfilePhotoClick,
                )
                Text(
                    text = if (profileImageUrl == null) "Add a profile photo" else "Change profile photo",
                    style = AppTextStyles.BodySmall,
                    color = AppColors.TextSecondary,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileTextField(
                    label = "Name",
                    value = editableName,
                    onValueChange = { editableName = it },
                    icon = AppIcons.Profile,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                )
                ProfileTextField(
                    label = "Email",
                    value = editableEmail,
                    onValueChange = { editableEmail = it },
                    icon = AppIcons.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )
                ProfileTextField(
                    label = "Phone number",
                    value = editablePhone,
                    onValueChange = { text -> editablePhone = text.filter { it.isDigit() } },
                    icon = AppIcons.Phone,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 22.dp),
        ) {
            val buttonWidth = maxWidth.value.toInt()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GreenButton(
                    onClick = {
                        if (!isSaving) onSaveClick(editableName.trim(), editableEmail.trim(), editablePhone.trim())
                    },
                    icon = AppIcons.Tick,
                    buttonText = if (isSaving) "Saving…" else "Save changes",
                    customHeight = 58,
                    customWidth = buttonWidth,
                )
                CancelButton(onClick = onCancelClick)
            }
        }
    }
}

/** Figma "Cancel button": 48dp, outlined, no fill. */
@Composable
private fun CancelButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .border(1.dp, AppColors.OutlineButton, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Cancel", style = AppTextStyles.ValueEmphasis, color = AppColors.TextPrimary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412, heightDp = 917)
@Composable
private fun EditProfileScreenPreview() {
    EditProfileScreen(name = "Maya Bennett", email = "maya.bennett@example.com", phoneNumber = "4155550138")
}
