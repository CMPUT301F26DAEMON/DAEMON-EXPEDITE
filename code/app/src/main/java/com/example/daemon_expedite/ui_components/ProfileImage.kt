package com.example.daemon_expedite.ui_components

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.daemon_expedite.R

/**
 * Circular profile picture, optionally with a pencil edit button in the bottom-right corner.
 *
 * Every part of the composable (border, edit button, icon) is sized as a fraction of [size],
 * so the same call looks right as a small avatar or a large profile header:
 *
 * ```
 * // Read-only avatar, e.g. in a chat row
 * ProfileImage(imageUrl = user.profileImageUrl, size = 40.dp)
 *
 * // Editable header on the profile screen
 * ProfileImage(imageUrl = user.profileImageUrl, size = 160.dp, onEditClick = { pickImage() })
 * ```
 *
 * @param imageUrl Web address of the photo. Loaded with Coil. When null, blank, or if loading
 * fails, [fallbackRes] is shown instead.
 * @param size Diameter of the picture.
 * @param modifier [Modifier] applied to the outer container.
 * @param onEditClick Called when the edit button is tapped. Pass null (the default) to hide the
 * edit button entirely.
 * @param showBorder Draws the thin sage ring around the photo. On by default when the edit button
 * is shown, matching the design.
 * @param fallbackRes Local drawable used when there is no [imageUrl], while it loads, and on error.
 * @param contentDescription Accessibility description of the photo.
 */
@Composable
fun ProfileImage(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null,
    showBorder: Boolean = onEditClick != null,
    @DrawableRes fallbackRes: Int = R.drawable.mock_profile,
    contentDescription: String? = "Profile picture",
) {
    val fallback = painterResource(fallbackRes)

    // Proportions taken from the mockups.
    val borderWidth = (size * 0.012f).coerceAtLeast(1.dp)
    val buttonSize = size * 0.32f
    val buttonRingWidth = buttonSize * 0.07f
    val iconSize = buttonSize * 0.42f
    // Puts the button's centre on the 45° line, pulled in just enough to stay inside the
    // container so the whole button remains tappable.
    val buttonOffset = size / 2 * 0.68f

    Box(modifier = modifier.size(size)) {
        AsyncImage(
            model = imageUrl?.takeIf { it.isNotBlank() },
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            placeholder = fallback,
            error = fallback,
            fallback = fallback,
            // Coil shows the fallback silently on failure, so log why (URL, HTTP error, etc.).
            onError = { state ->
                Log.w("ProfileImage", "Couldn't load $imageUrl", state.result.throwable)
            },
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(AppColors.SurfaceDisabled)
                .then(
                    if (showBorder) Modifier.border(borderWidth, AppColors.Secondary, CircleShape)
                    else Modifier
                ),
        )

        if (onEditClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = buttonOffset, y = buttonOffset)
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(AppColors.Background) // dark ring separating button from photo
                    .clickable(role = Role.Button, onClickLabel = "Edit profile picture", onClick = onEditClick)
                    .padding(buttonRingWidth)
                    .background(AppColors.Primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.EditPencil,
                    contentDescription = "Edit profile picture",
                    tint = AppColors.TextOnPrimary,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 420, heightDp = 220)
@Composable
private fun ProfileImagePreview() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileImage(imageUrl = null, size = 40.dp)
        ProfileImage(imageUrl = null, size = 72.dp, onEditClick = {})
        ProfileImage(imageUrl = null, size = 160.dp, onEditClick = {})
    }
}

// endregion
