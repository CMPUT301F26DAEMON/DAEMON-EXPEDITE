package com.example.daemon_expedite.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.*

@Composable
fun EditProfilePhotoScreen(
    profileImageUrl: String? = null,
    onBackClick: () -> Unit = {},
    onChoosePhotoClick: () -> Unit = {},
    onTakePhotoClick: () -> Unit = {},
    onDeletePhotoClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Header with back button
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
                text = "Profile photo",
                style = AppTextStyles.PageTitle,
                color = AppColors.TextOnPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Current profile picture
        ProfileImage(
            imageUrl = profileImageUrl,
            size = 168.dp,
            onEditClick = null,
            showBorder = true
        )

        Spacer(modifier = Modifier.height(36.dp))

        //Buttons
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {

            val buttonWidth = minOf(maxWidth, 356.dp).value.toInt()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Choose new photo button
                GreenButton(
                    onClick = onChoosePhotoClick,
                    icon = AppIcons.EditPencil,
                    buttonText = "Choose new photo",
                    customHeight = 58,
                    customWidth = buttonWidth
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Take new photo button
                OutlinedButton(
                    onClick = onTakePhotoClick,
                    modifier = Modifier
                        .width(buttonWidth.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = AppColors.OutlineStrong
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppColors.SurfaceDeep,
                        contentColor = AppColors.TextOnPrimary
                    )
                ) {

                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Take a photo",
                        style = AppTextStyles.ButtonLabel,
                        color = AppColors.TextOnPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                //Delete current photo button
                RedButton(
                    onClick = onDeletePhotoClick,
                    icon = AppIcons.Dustbin,
                    buttonText = "Delete current picture",
                    customHeight = 70,
                    customWidth = buttonWidth
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(  //Max file size 1MB
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = AppColors.Secondary,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "Maximum file size 1 MB",
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary
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
private fun EditProfilePhotoScreenPreview() {
    EditProfilePhotoScreen(
        profileImageUrl = null,
        onBackClick = {},
        onChoosePhotoClick = {},
        onTakePhotoClick = {},
        onDeletePhotoClick = {}
    )
}