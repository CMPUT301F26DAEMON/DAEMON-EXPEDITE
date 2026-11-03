package com.example.daemon_expedite.ui_components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.daemon_expedite.R

/**
 * A campsite photo with previous/next buttons and page dots.
 *
 * Built from the "Expedite/Media/Campsite carousel" component in the Figma file.
 * The arrows and dots are only shown when there is more than one photo. The arrows wrap
 * around (next on the last photo goes back to the first).
 *
 * ```
 * CampsiteImageCarousel(
 *     imageUrls = details.imageUrls,
 *     pill = { CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = AppIcons.LotteryTicket) },
 * )
 * ```
 *
 * @param imageUrls Download URLs of the photos. When empty, [placeholderRes] is shown.
 * @param modifier [Modifier] applied to the carousel.
 * @param height Height of the photo area. 232 dp in the design.
 * @param placeholderRes Drawable shown when there are no photos, while one loads, or if it fails.
 * @param pill Optional slot drawn in the top-right corner, usually a [CampsitePill].
 */
@Composable
fun CampsiteImageCarousel(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 232.dp,
    @DrawableRes placeholderRes: Int = R.drawable.camp1,
    pill: (@Composable () -> Unit)? = null,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val count = imageUrls.size
    // The list can shrink after a reload; never point past the end.
    val current = if (count == 0) 0 else page.coerceIn(0, count - 1)
    val shape = RoundedCornerShape(16.dp)
    val placeholder = painterResource(placeholderRes)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape),
    ) {
        AsyncImage(
            model = imageUrls.getOrNull(current),
            contentDescription = "Campsite photo ${current + 1} of ${maxOf(count, 1)}",
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (pill != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) { pill() }
        }

        if (count > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CarouselArrow(AppIcons.ChevronLeft, "Previous photo") {
                    page = (current - 1 + count) % count
                }
                CarouselArrow(AppIcons.ChevronRight, "Next photo") {
                    page = (current + 1) % count
                }
            }
            PageDots(
                count = count,
                current = current,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun CarouselArrow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(AppColors.SurfaceDeep)
            .border(BorderStroke(1.dp, AppColors.OutlineStrong), CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = AppColors.TextPrimary, modifier = Modifier.size(20.dp))
    }
}

/** The current page is a 24 dp bar, the others are 5 dp dots. */
@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .width(if (selected) 24.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (selected) AppColors.TextPrimary else AppColors.TextPrimary.copy(alpha = 0.5f)),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun CampsiteImageCarouselPreview() {
    CampsiteImageCarousel(
        imageUrls = listOf("a", "b", "c"),
        modifier = Modifier.padding(12.dp),
        pill = { CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = AppIcons.LotteryTicket) },
    )
}
