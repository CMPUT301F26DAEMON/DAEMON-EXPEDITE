package com.example.daemon_expedite.ui_components

import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import com.example.daemon_expedite.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ConfirmationNumber // needs material-icons-extended
import androidx.compose.material.icons.filled.Schedule // needs material-icons-extended
import androidx.compose.material.icons.filled.StarOutline // needs material-icons-extended
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage // io.coil-kt.coil3:coil-compose + coil-network-okhttp

/**
 * Visual style of a [CampsitePill].
 */
enum class PillVariant {
    /** Amber outline with an amber icon. For attention states such as an active lottery. */
    Highlight,
    /** Quiet dark pill with a subtle border. For neutral states such as "no lottery". */
    Muted,
    /** Dark pill with a thin sage outline. For status labels such as "Confirmed". */
    Outlined,
}

/**
 * A small rounded label that sits on top of a campsite photo.
 *
 * It is a standalone composable, so you can use it inside [CampsiteCard]'s
 * `pill` slot or anywhere else. Create one per state of your campsite:
 *
 * ```
 * CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = Icons.Default.ConfirmationNumber)
 * CampsitePill("Lottery · None", variant = PillVariant.Muted, icon = Icons.Default.Schedule)
 * CampsitePill("Confirmed")
 * ```
 *
 * @param text Label shown inside the pill.
 * @param modifier [Modifier] applied to the pill.
 * @param variant Color style of the pill. See [PillVariant].
 * @param icon Optional leading icon. Omit for a text-only pill.
 */
@Composable
fun CampsitePill(
    text: String,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.Outlined,
    icon: ImageVector? = null,
) {
    val background: Color
    val borderColor: Color
    val borderWidth: Float
    val iconTint: Color
    when (variant) {
        PillVariant.Highlight -> {
            background = AppColors.SurfaceDeep.copy(alpha = 0.55f)
            borderColor = AppColors.Accent
            borderWidth = 1.5f
            iconTint = AppColors.Accent
        }
        PillVariant.Muted -> {
            background = AppColors.SurfaceDeep.copy(alpha = 0.8f)
            borderColor = AppColors.OutlineSubtle
            borderWidth = 1f
            iconTint = AppColors.TextSecondary
        }
        PillVariant.Outlined -> {
            background = AppColors.SurfaceDeep.copy(alpha = 0.7f)
            borderColor = AppColors.OutlineAccent
            borderWidth = 1f
            iconTint = AppColors.TextPrimary
        }
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(BorderStroke(borderWidth.dp, borderColor), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Text(text = text, style = AppTextStyles.TabLabel, color = AppColors.TextOnPrimary)
    }
}

/**
 * One campsite card, used for browsing, reservations and past stays.
 *
 * The parts that differ between screens are slots, so one composable covers
 * every variant:
 * - [pill] shows a [CampsitePill] (or anything else) in the photo's top-right corner.
 * - [action] shows a button row under the details, for example your
 *   `ButtonRed` ("Cancel reservation") or `ButtonGreen` ("Rate Your Stay").
 * - [onClick] makes the whole card tappable and adds the trailing arrow.
 *   Leave it null for cards that only show an [action].
 *
 * ```
 * // Browse card
 * CampsiteCard(
 *     name = "Pine Creek Hideaway", location = "Big Sur, California",
 *     pricePerNight = "$45", rating = 4.9, imageUrl = url, maxOccupancy = 5,
 *     pill = { CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = Icons.Default.ConfirmationNumber) },
 *     onClick = { openCampsite(id) },
 * )
 *
 * // Reservation card
 * CampsiteCard(
 *     name = "Pine Creek Hideaway", location = "Big Sur, California",
 *     pricePerNight = "$45", rating = 4.9, imageUrl = url,
 *     pill = { CampsitePill("Confirmed") },
 *     action = { ButtonRed("Cancel reservation", onClick = { cancel(id) }) },
 * )
 * ```
 *
 * @param name Campsite name.
 * @param location Location line, for example "Big Sur, California".
 * @param pricePerNight Already formatted price, for example "$45".
 * @param imageUrl Web address of the photo, for example `Campsite.coverImageUrl`. Loaded with Coil.
 *  * @param imageRes Local drawable shown when [imageUrl] is null, while the photo loads, and if
 *  * loading fails. Defaults to the app's placeholder, so most callers never pass it.
 *  @param modifier [Modifier] applied to the card.
 * @param rating Average rating, or null to hide the rating.
 * @param maxOccupancy Maximum guests, or null to hide "(Max Occ: n)".
 * @param pill Optional slot drawn in the top-right corner of the photo.
 * @param action Optional slot drawn at the bottom of the card, full width.
 * @param onClick Called when the card is tapped. When null the card is not
 * clickable and the arrow is hidden.
 * @param containerColor Background of the details area. Use a darker color for
 * browse cards and [AppColors.Surface] for reservation cards.
 * @param imageAspectRatio Width to height ratio of the photo.
 */
@Composable
fun CampsiteCard(
    name: String,
    location: String,
    pricePerNight: String,
    imageUrl: String? = null,
    @DrawableRes imageRes: Int = R.drawable.camp1,
    modifier: Modifier = Modifier,
    rating: Double? = null,
    maxOccupancy: Int? = null,
    pill: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    containerColor: Color = AppColors.FillField,
    imageAspectRatio: Float = 2.8f,
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(BorderStroke(1.dp, AppColors.OutlineSubtle), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Photo with optional pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(imageAspectRatio)
                .background(AppColors.SurfaceDeep)
        ) {
            val fallback = painterResource(imageRes)
            AsyncImage(
                model = imageUrl,
                contentDescription = "Photo of $name",
                contentScale = ContentScale.Crop,
                placeholder = fallback,
                error = fallback,
                fallback = fallback,
                modifier = Modifier.matchParentSize(),
            )

            if (pill != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) { pill() }
            }
        }

        // Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = AppTextStyles.CardTitle,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (rating != null) {
                    Icon(
                        Icons.Default.StarOutline,
                        contentDescription = "Rating",
                        tint = AppColors.Accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "%.1f".format(rating),
                        style = AppTextStyles.BodyMedium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            Text(
                text = location,
                style = AppTextStyles.BodyMedium,
                color = AppColors.TextSecondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = AppColors.TextOnPrimary)) { append(pricePerNight) }
                        withStyle(
                            AppTextStyles.Caption.toSpanStyle().copy(color = AppColors.TextSecondary)
                        ) {
                            append(" / night")
                            if (maxOccupancy != null) append(" (Max Occ: $maxOccupancy)")
                        }
                    },
                    style = AppTextStyles.Price,
                    modifier = Modifier.weight(1f),
                )
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open campsite",
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (action != null) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp)) { action() }
            }
        }
    }
}

// region Previews

/** Android Studio previews do not download images, so the photo area shows the placeholder. */
private const val PREVIEW_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/daemon-exp.firebasestorage.app/o/campsites%2F01a18eca-6027-7c70-82a8-12b2a05cf4c2%2F01a18f07-3600-76fc-bd41-e46057e7da7d.jpg?alt=media&token=915a3fd5-5b9d-48af-a639-264c0cf20434"

/** Stand-in for your ButtonRed / ButtonGreen so the previews compile on their own. */
@Composable
private fun PreviewActionButton(label: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(container, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AppTextStyles.ButtonLabel, color = content)
    }
}

/** All four card variants from the Figma screens. The photo is blank in previews since preview doesnt have access to internet :(. */
@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380, heightDp = 1100)
@Composable
private fun CampsiteCardPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CampsiteCard(
            name = "Pine Creek Hideaway", location = "Big Sur, California",
            pricePerNight = "$45", rating = 4.9, maxOccupancy = 5, imageUrl = PREVIEW_IMAGE_URL,
            pill = { CampsitePill("Lottery · Active", variant = PillVariant.Highlight, icon = Icons.Default.ConfirmationNumber) },
            onClick = {},
            containerColor = AppColors.SurfaceDeep,
        )
        CampsiteCard(
            name = "Redwood Meadow", location = "Felton, California",
            pricePerNight = "$38", rating = 4.8, maxOccupancy = 6, imageUrl = null,
            pill = { CampsitePill("Lottery · None", variant = PillVariant.Muted, icon = Icons.Default.Schedule) },
            onClick = {},
            containerColor = AppColors.SurfaceDeep,
        )
        CampsiteCard(
            name = "Pine Creek Hideaway", location = "Big Sur, California",
            pricePerNight = "$45", rating = 4.9, imageUrl = null,
            pill = { CampsitePill("Confirmed") },
            action = { PreviewActionButton("Cancel reservation", AppColors.DangerTint, AppColors.Danger) },
        )
        CampsiteCard(
            name = "Pine Creek Hideaway", location = "Big Sur, California",
            pricePerNight = "$45", rating = 4.9, imageUrl = null,
            action = { PreviewActionButton("Rate Your Stay", AppColors.Primary, AppColors.TextOnPrimary) },
        )
    }
}

// endregion