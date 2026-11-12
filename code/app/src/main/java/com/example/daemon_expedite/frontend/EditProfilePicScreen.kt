package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.*

/**
 * Profile photo screen, built from the "Edit Profile pic" Figma frame: the current photo with
 * the user's name in the middle, and "Choose new photo" / "Remove photo" at the bottom.
 */
@Composable
fun EditProfilePhotoScreen(
    profileImageUrl: String? = null,
    name: String = "",
    onBackClick: () -> Unit = {},
    onChoosePhotoClick: () -> Unit = {},
    onDeletePhotoClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = "Profile photo", onBackClick = onBackClick)

        // Photo, name and caption, centred in the space above the buttons.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ProfileImage(
                imageUrl = profileImageUrl,
                size = 176.dp,
                showBorder = true,
            )
            Spacer(Modifier.height(20.dp))
            if (name.isNotBlank()) {
                Text(
                    text = name,
                    style = AppTextStyles.PageTitleSmall,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = "Campsite owners and other campers see this photo.",
                style = AppTextStyles.BodySmallMultiline,
                color = AppColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 22.dp),
        ) {
            val buttonWidth = maxWidth.value.toInt()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GreenButton(
                    onClick = onChoosePhotoClick,
                    icon = AppIcons.AddPhoto,
                    buttonText = "Choose new photo",
                    customHeight = 58,
                    customWidth = buttonWidth
                )
                RedButton(
                    onClick = onDeletePhotoClick,
                    icon = AppIcons.Dustbin,
                    buttonText = "Remove photo",
                    customHeight = 58,
                    customWidth = buttonWidth
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412, heightDp = 917)
@Composable
private fun EditProfilePhotoScreenPreview() {
    EditProfilePhotoScreen(name = "Maya Bennett")
}
