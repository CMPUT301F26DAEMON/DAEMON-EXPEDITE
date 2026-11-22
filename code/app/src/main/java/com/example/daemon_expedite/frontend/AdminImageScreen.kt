
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import coil3.compose.rememberAsyncImagePainter
import com.example.daemon_expedite.R
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.SearchBar

/**
 * Represents an image displayed in the admin gallery.
 *
 * @param id Unique identifier for the image.
 * @param uploaderName Name of the user who uploaded the image.
 * @param imageUrl Download URL of the image. Takes priority over [imageRes].
 * @param imageRes Drawable resource to show instead, for previews.
 */
data class AdminImage(
    val id: String,
    val uploaderName: String,
    val imageUrl: String? = null,
    val imageRes: Int? = null,
)

/** What the image loader should load for [image]: its URL, or its drawable in previews. */
private val AdminImage.model: Any? get() = imageUrl ?: imageRes

/**
 * The admin's Images tab: a searchable three-column gallery of every uploaded image.
 *
 * Tapping an image selects or deselects it; the dustbin deletes the selection after a
 * confirmation. Long-pressing an image shows an enlarged preview until the press is released.
 *
 * Opened from the admin bottom bar, which AppNavigation draws over this screen, so there is no
 * back button. [AdminImageScreenViewModel] does the loading and deleting.
 *
 * @param state The images to show, or a loading/error state.
 * @param onRetry Called when the error message is tapped.
 * @param onDeleteConfirmed Called with the selected image ids once the admin confirms.
 * @param isDeleting True while a delete runs; the dustbin is disabled meanwhile.
 */
@Composable
fun AdminImagesScreen(
    state: UiState<List<AdminImage>> = UiState.Content(emptyList()),
    onRetry: () -> Unit = {},
    onDeleteConfirmed: (Set<String>) -> Unit = {},
    isDeleting: Boolean = false,
) {
    val images = (state as? UiState.Content)?.data.orEmpty()

    var searchQuery by remember { mutableStateOf("") }

    // IDs of selected images
    var selectedImageIds by remember {
        mutableStateOf(setOf<String>())
    }

    val isSelectionMode = selectedImageIds.isNotEmpty()

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Image shown while long-pressing
    var previewImage by remember {
        mutableStateOf<AdminImage?>(null)
    }

    // Filter images by uploader name
    val filteredImages = images.filter {
        it.uploaderName.contains(
            searchQuery.trim(),
            ignoreCase = true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // Header: left-aligned title, delete button on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Images",
                    style = AppTextStyles.PageTitle,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                val canDelete = isSelectionMode && !isDeleting
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = canDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 1.dp,
                            color = if (canDelete) {
                                AppColors.Danger
                            } else {
                                AppColors.OutlineSubtle
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = AppIcons.Dustbin,
                        contentDescription = "Delete selected images",
                        tint = if (canDelete) {
                            AppColors.Danger
                        } else {
                            AppColors.OutlineStrong
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Existing teammate SearchBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholderText = "Search by uploader",
                    showQrIcon = false,
                    onQRCodeClicked = {}
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Image count and selection count
            Text(
                text = buildString {
                    val count = filteredImages.size
                    append(if (count == 1) "1 image" else "$count images")

                    if (isSelectionMode) {
                        append(" · ${selectedImageIds.size} selected")
                    }
                },
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
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
                            modifier = Modifier.clickable(onClick = onRetry).padding(12.dp)
                        )
                    }

                    filteredImages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "No images to show" else "No images match \"$searchQuery\"",
                            style = AppTextStyles.BodyMedium,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Three-column gallery
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            items = filteredImages,
                            key = { it.id }
                        ) { image ->

                            AdminImageThumbnail(
                                image = image,
                                isSelectionMode = isSelectionMode,
                                isSelected = image.id in selectedImageIds,

                                // Tap to select/deselect
                                onClick = {
                                    selectedImageIds =
                                        if (image.id in selectedImageIds) {
                                            selectedImageIds - image.id
                                        } else {
                                            selectedImageIds + image.id
                                        }
                                },

                                // Long press to open preview
                                onLongClick = {
                                    previewImage = image
                                },

                                // Release finger to close preview
                                onLongPressRelease = {
                                    if (previewImage?.id == image.id) {
                                        previewImage = null
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Room for the shared bottom bar AppNavigation draws on top.
            BottomNavBarSpacer()
        }

        // Full image preview while long-pressing
        previewImage?.let { image ->
            AdminImagePreviewOverlay(image = image)
        }
    }

    if (showDeleteDialog && isSelectionMode) {
        val count = selectedImageIds.size
        ConfirmDialog(
            title = if (count == 1) "Delete this image?" else "Delete $count images?",
            message = "They will be removed from any profile or campsite using them. This can't be undone.",
            confirmText = if (count == 1) "Yes, delete image" else "Yes, delete images",
            dismissText = "Keep",
            onConfirm = {
                showDeleteDialog = false
                onDeleteConfirmed(selectedImageIds)
                selectedImageIds = emptySet()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

/**
 * Displays an image thumbnail inside the gallery.
 *
 * Images have rounded corners and are cropped to a consistent
 * thumbnail size. Selected images have a green overlay and checkmark.
 *
 * @param image Image to display.
 * @param isSelectionMode Whether any images are selected.
 * @param isSelected Whether this image is selected.
 * @param onClick Called when the image is tapped.
 * @param onLongClick Called when the image is long-pressed.
 * @param onLongPressRelease Called when the press ends.
 */
@Composable
private fun AdminImageThumbnail(
    image: AdminImage,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongPressRelease: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnRelease by rememberUpdatedState(onLongPressRelease)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.77f)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(image.id) {
                detectTapGestures(
                    onTap = {
                        currentOnClick()
                    },
                    onLongPress = {
                        currentOnLongClick()
                    },
                    onPress = {
                        try {
                            tryAwaitRelease()
                        } finally {
                            currentOnRelease()
                        }
                    }
                )
            }
    ) {

        // Thumbnail image
        Image(
            painter = rememberAsyncImagePainter(image.model),
            contentDescription =
                "Image uploaded by ${image.uploaderName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Green overlay for selected images
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        AppColors.PrimarySelected
                    )
                    .border(
                        width = 2.dp,
                        color = AppColors.Primary,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }

        // Selection circles appear only in selection mode
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(
                        color = if (isSelected) {
                            AppColors.Primary
                        } else {
                            AppColors.FillIndicator
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) {
                            AppColors.Primary
                        } else {
                            AppColors.OutlineControl
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = AppIcons.Tick,
                        contentDescription = "Selected",
                        tint = AppColors.TextOnPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Displays an enlarged image while the admin holds a long press.
 *
 * The popup has rounded corners, but the image itself is displayed
 * using ContentScale.Fit to preserve the complete image without cropping.
 *
 * @param image Image to preview.
 */
@Composable
private fun AdminImagePreviewOverlay(
    image: AdminImage
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ScrimStrong),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.Surface)
            ) {

                // Enlarged image preserving its original proportions
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val painter = rememberAsyncImagePainter(image.model)
                    val intrinsicSize = painter.intrinsicSize

                    val aspectRatio =
                        if (intrinsicSize.width > 0f &&
                            intrinsicSize.height > 0f) {
                            intrinsicSize.width / intrinsicSize.height
                        } else {
                            1f
                        }

                    // Limit the image height while maintaining aspect ratio
                    val imageHeight =
                        (maxWidth / aspectRatio).coerceAtMost(450.dp)

                    Image(
                        painter = painter,
                        contentDescription =
                            "Preview of image uploaded by ${image.uploaderName}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageHeight)
                    )
                }

                // Uploader name
                Text(
                    text = image.uploaderName,
                    style = AppTextStyles.TitleSmall,
                    color = AppColors.TextOnPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Release to close",
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Compose preview with sample images.
 * Mock data is used only here, not in the actual screen.
 */
@Preview(
    showBackground = true,
    widthDp = 393,
    heightDp = 852
)
@Composable
private fun AdminImagesScreenPreview() {

    val sampleImages = List(24) { index ->
        AdminImage(
            id = index.toString(),
            imageRes = R.drawable.mock_profile_funky,
            uploaderName = "Sample User ${index + 1}"
        )
    }

    AdminImagesScreen(
        state = UiState.Content(sampleImages)
    )
}
