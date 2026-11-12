
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.daemon_expedite.R
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.SearchBar

/**
 * Represents an image displayed in the admin gallery.
 *
 * @param id Unique identifier for the image.
 * @param imageRes Drawable resource used to display the image.
 * @param uploaderName Name of the user who uploaded the image.
 * @param uploadDate Date the image was uploaded.
 */
data class AdminImage(
    val id: String,
    val imageRes: Int,
    val uploaderName: String,
    val uploadDate: String
)

/**
 * Admin Images screen for viewing and selecting uploaded images.
 *
 * Displays a searchable three-column gallery.
 * Tapping an image selects or deselects it for deletion.
 * Long-pressing an image shows an enlarged preview with uploader details.
 * Releasing the press closes the preview.
 *
 * Image loading, deletion, and navigation are handled externally.
 *
 * @param images Images displayed in the gallery.
 * @param onBackClick Called when the back button is tapped.
 * @param onDeleteClick Called with selected image IDs when delete is tapped.
 */
@Composable
fun AdminImagesScreen(
    images: List<AdminImage> = emptyList(),
    onBackClick: () -> Unit = {},
    onDeleteClick: (Set<String>) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    // IDs of selected images
    var selectedImageIds by remember {
        mutableStateOf(setOf<String>())
    }

    val isSelectionMode = selectedImageIds.isNotEmpty()

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

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 24.dp)
            ) {

                // Existing teammate BackButton
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BackButton(onClick = onBackClick)
                }

                // Centered title
                Text(
                    text = "Images",
                    style = AppTextStyles.PageTitle,
                    color = AppColors.TextOnPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Delete button
                IconButton(
                    onClick = {
                        onDeleteClick(selectedImageIds)
                    },
                    enabled = isSelectionMode,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .border(
                            width = 1.dp,
                            color = if (isSelectionMode) {
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
                        tint = if (isSelectionMode) {
                            AppColors.Danger
                        } else {
                            AppColors.OutlineStrong
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    append("${images.size} images")

                    if (isSelectionMode) {
                        append(" · ${selectedImageIds.size} selected")
                    }
                },
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Three-column gallery
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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

        // Full image preview while long-pressing
        previewImage?.let { image ->
            AdminImagePreviewOverlay(image = image)
        }
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
            painter = painterResource(image.imageRes),
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
                        AppColors.Primary.copy(alpha = 0.35f)
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
                            Color(0x66000000)
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) {
                            AppColors.Primary
                        } else {
                            AppColors.TextOnPrimary
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
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
            .background(AppColors.Scrim),
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
                    val painter = painterResource(image.imageRes)
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

                // Uploader name and upload date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = image.uploaderName,
                        style = AppTextStyles.TitleSmall,
                        color = AppColors.TextOnPrimary
                    )

                    Text(
                        text = image.uploadDate,
                        style = AppTextStyles.BodySmall,
                        color = AppColors.TextSecondary
                    )
                }
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
            uploaderName = "Sample User ${index + 1}",
            uploadDate = "Nov 11, 2026"
        )
    }

    AdminImagesScreen(
        images = sampleImages
    )
}
