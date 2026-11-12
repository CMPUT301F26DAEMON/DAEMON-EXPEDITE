package com.example.daemon_expedite.frontend
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppFonts
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.ProfileImage
data class AdminProfile(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val imageUrl: String? = null
)
//Sample profiles for preview
private val sampleProfiles = listOf(
    AdminProfile(
        id = "1",
        name = "Maya Bennett",
        phone = "+1 (415) 555-0138",
        email = "maya.bennett@example.com"
    ),
    AdminProfile(
        id = "2",
        name = "Amitej",
        phone = "6787678767",
        email = "aks@gmail.com"
    ),
    AdminProfile(
        id = "3",
        name = "Keya",
        phone = "1234567890",
        email = "keya@gmail.com"
    )
)
@Composable
fun AdminProfilesScreen(
    profiles: List<AdminProfile> = sampleProfiles,
    onCampsitesClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onProfilesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onImagesClick: () -> Unit = {},
    initialSelectedProfileId: String? = null,
    initialShowDeleteDialog: Boolean = false
) {
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var selectedProfileId by rememberSaveable {
        mutableStateOf(initialSelectedProfileId)
    }

    var showDeleteDialog by rememberSaveable {
        mutableStateOf(initialShowDeleteDialog)
    }
    val selectedProfile = profiles.firstOrNull{it.id ==selectedProfileId}
    val filteredProfiles = profiles.filter { profile ->
        profile.name.contains(searchQuery, ignoreCase = true) ||
                profile.phone.contains(searchQuery, ignoreCase = true) ||
                profile.email.contains(searchQuery, ignoreCase = true)}

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomNavBar(
                labels = listOf(
                    "Campsites",
                    "Messages",
                    "Profiles",
                    "Notifications",
                    "Images"
                ),
                icons = listOf(
                    AppIcons.Campsite,
                    AppIcons.Chat,
                    AppIcons.Profile,
                    AppIcons.BellIcon,
                    AppIcons.AddPhoto
                ),
                onClicks = listOf(
                    onCampsitesClick,
                    onMessagesClick,
                    onProfilesClick,
                    onNotificationsClick,
                    onImagesClick
                )
            )
        }
    ) {paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = 22.dp
                )
        ) {Text(text = "Profiles", style = AppTextStyles.PageTitle,color = AppColors.TextOnPrimary)

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    AdminProfileSearchBar(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                        }
                    )
                }
                IconButton(
                    onClick = { // Delete icon changes red
                        if (selectedProfile != null) {
                            showDeleteDialog = true
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Dustbin,
                        contentDescription = "Delete profile",
                        tint = if (selectedProfile != null) {
                            AppColors.Danger
                        } else {
                            AppColors.TextOnPrimary
                        },
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = 12.dp
                )
            ) {

                itemsIndexed(
                    items = filteredProfiles,
                    key = { _, profile -> profile.id }
                ) { index, profile ->

                    val isSelected =
                        selectedProfileId == profile.id

                    AdminProfileRow(
                        profile = profile,
                        isSelected = isSelected,
                        onClick = {
                            selectedProfileId =
                                if (isSelected) {
                                    null
                                } else {
                                    profile.id
                                }
                        }
                    )

                    if (index < filteredProfiles.lastIndex) { // Divider
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = 10.dp
                            ),
                            thickness = 1.dp,
                            color = AppColors.OutlineSubtle
                        )
                    }
                }
            }
        }
    }
    //  Delete dialog
    if (showDeleteDialog && selectedProfile != null) {
        AdminDeleteProfileDialog(
            profileName = selectedProfile.name,

            onConfirm = {
                // Deletion for later implement
                showDeleteDialog = false
            },

            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}
@Composable
private fun AdminProfileSearchBar(  //Search bar
    query: String,
    onQueryChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(50)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,

        textStyle = TextStyle(
            color = AppColors.TextOnPrimary,
            fontFamily = AppFonts.Inter,
            fontSize = 14.sp
        ),
        cursorBrush = SolidColor(AppColors.TextOnPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(shape)
            .background(AppColors.FillSubtle)
            .border(
                width = 1.dp,
                color = AppColors.OutlineSubtle,
                shape = shape
            ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = "Search",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(9.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {

                    if (query.isEmpty()) {
                        Text(
                            text = "Search profiles",
                            color = AppColors.TextSecondary,
                            fontFamily = AppFonts.Inter,
                            fontSize = 12.sp
                        )
                    }

                    innerTextField()
                }
            }
        }
    )
}
@Composable
private fun AdminProfileRow(  //Display 1 profile
    profile: AdminProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(83.dp)
            .clip(shape)
            .background(
                if (isSelected) {
                    AppColors.DangerTint
                } else {
                    Color.Transparent
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    AppColors.Danger.copy(alpha = 0.7f)
                } else {
                    Color.Transparent
                },
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileImage(
            imageUrl = profile.imageUrl,
            size = 40.dp,
            showBorder = false,
            contentDescription =
                "${profile.name} profile picture"
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = profile.name,
                fontFamily = AppFonts.Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AppColors.TextOnPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = profile.phone,
                fontFamily = AppFonts.Inter,
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = profile.email,
                fontFamily = AppFonts.Inter,
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@Composable
private fun AdminDeleteProfileDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Scrim),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
            )

            val dialogShape = RoundedCornerShape(19.dp)

            Column(
                modifier = Modifier
                    .widthIn(max = 346.dp)
                    .fillMaxWidth(0.88f)
                    .clip(dialogShape)
                    .background(AppColors.SurfaceOverlay)
                    .border(
                        width = 1.dp,
                        color = AppColors.OutlineSubtle,
                        shape = dialogShape
                    )
                    .padding(18.dp),

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) { //Trash icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AppColors.DangerTint)
                        .border(
                            width = 1.dp,
                            color = AppColors.Danger,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = AppIcons.Dustbin,
                        contentDescription = "Delete",
                        tint = AppColors.Danger,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                // Popup
                Text(
                    text = "Delete this profile?",
                    style = AppTextStyles.HeadingMedium,
                    color = AppColors.TextOnPrimary,
                    textAlign = TextAlign.Center
                )
                // Warning
                Text(
                    text = "$profileName's account, reservations " +
                            "and messages will be permanently removed. " +
                            "This can't be undone.",
                    style = AppTextStyles.BodySmallMultiline,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(  //Confirmation
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = AppColors.Danger
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppColors.DangerTint,
                        contentColor = AppColors.Danger
                    )
                ) {Text(
                        text = "Yes, delete profile",
                        fontFamily = AppFonts.Inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Keep profile
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = AppColors.OutlineStrong
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppColors.SurfaceDeep,
                        contentColor = AppColors.TextOnPrimary
                    )
                ) {

                    Text(
                        text = "Keep profile",
                        fontFamily = AppFonts.Inter,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
@Preview(
    name = "Profiles - Normal",
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun AdminProfilesNormalPreview() {
    AdminProfilesScreen()
}
@Preview(name= "Profiles - Selected",showBackground=true,backgroundColor = 0xFF121514,widthDp = 390,heightDp = 844)
@Composable
private fun AdminProfilesSelectedPreview(){AdminProfilesScreen(initialSelectedProfileId = "1")
}
@Preview(
    name = "Profiles - Delete Confirmation",
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun AdminProfilesDeletePreview(){AdminProfilesScreen(initialSelectedProfileId = "1",initialShowDeleteDialog = true) }
