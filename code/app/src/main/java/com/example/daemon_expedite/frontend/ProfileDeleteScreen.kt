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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.ConfirmDialog
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
/**
 * The admin's Profiles tab: every user, searchable by name, phone or email.
 *
 * Tap a profile to select it, then the dustbin to delete it (after a confirmation dialog).
 * Opened from the admin bottom bar, which AppNavigation draws over this screen, so the screen
 * has no bar or back button of its own. [ProfileDeleteScreenViewModel] does the loading and deleting.
 *
 * @param state The profiles to list, or a loading/error state.
 * @param onRetry Called when the error message is tapped.
 * @param onDeleteConfirmed Called with the profile's id once the admin confirms the delete.
 * @param isDeleting True while a delete runs; the dustbin does nothing meanwhile.
 * @param initialSelectedProfileId Profile selected at first (for previews).
 * @param initialShowDeleteDialog Whether the delete dialog is open at first (for previews).
 */
@Composable
fun AdminProfilesScreen(
    state: UiState<List<AdminProfile>> = UiState.Content(sampleProfiles),
    onRetry: () -> Unit = {},
    onDeleteConfirmed: (String) -> Unit = {},
    isDeleting: Boolean = false,
    initialSelectedProfileId: String? = null,
    initialShowDeleteDialog: Boolean = false
) {
    val profiles = (state as? UiState.Content)?.data.orEmpty()

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .statusBarsPadding()
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = 24.dp
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
                        if (selectedProfile != null && !isDeleting) {
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

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }

                    state is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "${state.message}. Tap to try again.",
                            style = AppTextStyles.BodyMedium,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable(onClick = onRetry).padding(12.dp),
                        )
                    }

                    filteredProfiles.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "No profiles to show" else "No profiles match \"$searchQuery\"",
                            style = AppTextStyles.BodyMedium,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    else -> LazyColumn(
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

            // Room for the shared bottom bar AppNavigation draws on top.
            BottomNavBarSpacer()
        }
    //  Delete dialog
    if (showDeleteDialog && selectedProfile != null) {
        ConfirmDialog(
            title = "Delete this profile?",
            message = "${selectedProfile.name}'s account, reservations " +
                "and messages will be permanently removed. This can't be undone.",
            confirmText = "Yes, delete profile",
            dismissText = "Keep profile",
            onConfirm = {
                showDeleteDialog = false
                selectedProfileId = null
                onDeleteConfirmed(selectedProfile.id)
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

        textStyle = AppTextStyles.BodyMedium.copy(color = AppColors.TextOnPrimary),
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
                            style = AppTextStyles.Caption,
                            color = AppColors.TextSecondary
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
                    AppColors.DangerBorder
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
                style = AppTextStyles.LabelMedium,
                color = AppColors.TextOnPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Only lines with content are drawn, so the column (centred in the row) stays
            // centred when the phone number or email is missing.
            if (profile.phone.isNotBlank()) {
                Text(
                    text = profile.phone,
                    style = AppTextStyles.Caption,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (profile.email.isNotBlank()) {
                Text(
                    text = profile.email,
                    style = AppTextStyles.Caption,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
