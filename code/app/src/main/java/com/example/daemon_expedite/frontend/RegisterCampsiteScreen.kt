package com.example.daemon_expedite.frontend

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.daemon_expedite.frontend.RegisterCampsiteViewModel.Companion.MAX_AMENITY_LENGTH
import com.example.daemon_expedite.frontend.RegisterCampsiteViewModel.Companion.MAX_PHOTOS
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.FormTextField
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.RedButton
import java.util.Locale

/**
 * "Add campsite" form, from the Figma frame "Register campsite · Empty".
 *
 * Only draws [draft] and reports changes; [RegisterCampsiteViewModel] holds the values and
 * saves them. The location row opens the Select location map.
 *
 * @param draft Current form values.
 * @param errors Problems to show under the fields (only filled in after the first submit).
 * @param isSaving True while saving: the register button is disabled and says "Registering…".
 * @param onDraftChange Called with a change to apply to the draft, for example `{ it.copy(name = text) }`.
 * @param onAddressChange Called when the owner types in the address field.
 * @param onOccupancyChange Called with +1 or −1 from the occupancy buttons.
 * @param onPriceChange Called when the owner types a price (the ViewModel keeps only digits).
 * @param onAddAmenity Called with a typed amenity. Returns false if it wasn't added (duplicate).
 * @param onChooseOnMapClick Opens the Select location map.
 * @param onAddPhotosClick Opens the photo picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterCampsiteScreen(
    draft: CampsiteDraft,
    errors: CampsiteDraftErrors = CampsiteDraftErrors(),
    isSaving: Boolean = false,
    onDraftChange: ((CampsiteDraft) -> CampsiteDraft) -> Unit = {},
    onAddressChange: (String) -> Unit = {},
    onOccupancyChange: (Int) -> Unit = {},
    onPriceChange: (String) -> Unit = {},
    onAddAmenity: (String) -> Boolean = { true },
    onRemoveAmenity: (String) -> Unit = {},
    onChooseOnMapClick: () -> Unit = {},
    onAddPhotosClick: () -> Unit = {},
    onRemovePhoto: (Uri) -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Full-width buttons inside the 28dp side padding (GreenButton/RedButton take a width in dp).
        val buttonWidth = (maxWidth - 56.dp).value.toInt()

        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Add campsite", onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // region Campsite essentials
                SectionHeading("Campsite essentials")
                FormTextField(
                    label = "Campsite name",
                    value = draft.name,
                    onValueChange = { text -> onDraftChange { it.copy(name = text) } },
                    icon = AppIcons.Campsite,
                    error = errors.name,
                )
                FormTextField(
                    label = "Description",
                    value = draft.description,
                    onValueChange = { text -> onDraftChange { it.copy(description = text) } },
                    icon = AppIcons.Description,
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )
                FormTextField(
                    label = "Address",
                    value = draft.address,
                    onValueChange = onAddressChange,
                    icon = AppIcons.MapPin,
                    error = errors.address,
                    containerColor = AppColors.Surface,
                )
                LocationPicker(draft = draft, error = errors.location, onClick = onChooseOnMapClick)
                // endregion

                // region Amenities
                SectionHeading("Amenities")
                AmenitiesEditor(
                    amenities = draft.amenities,
                    onAddAmenity = onAddAmenity,
                    onRemoveAmenity = onRemoveAmenity,
                )
                OccupancyStepper(guests = draft.maxOccupancy, onChange = onOccupancyChange)
                FormTextField(
                    label = "Price per night",
                    value = draft.pricePerNight,
                    // Filtered here too, so a pasted "$45.50" never flashes up before the ViewModel cleans it.
                    onValueChange = { text -> onPriceChange(text.filter { it in '0'..'9' }) },
                    icon = AppIcons.Price,
                    error = errors.pricePerNight,
                    // Digits-only keypad: no "." "," "-" or space keys.
                    keyboardType = KeyboardType.NumberPassword,
                )
                // endregion

                // region Promo image
                SectionHeading("Promo image")
                PromoImageUpload(
                    photos = draft.photos,
                    onAddPhotosClick = onAddPhotosClick,
                    onRemovePhoto = onRemovePhoto,
                )
                // endregion

                // region Guest guidelines
                SectionHeading("Guest guidelines")
                FormTextField(
                    label = "Campsite rules",
                    value = draft.rules,
                    onValueChange = { text -> onDraftChange { it.copy(rules = text) } },
                    icon = AppIcons.Rules,
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )
                FormTextField(
                    label = "Cancellation policy",
                    value = draft.cancellationPolicy,
                    onValueChange = { text -> onDraftChange { it.copy(cancellationPolicy = text) } },
                    icon = AppIcons.Shield,
                    singleLine = false,
                    imeAction = ImeAction.Default,
                )
                // endregion

                GreenButton(
                    onClick = { if (!isSaving) onRegisterClick() },
                    icon = AppIcons.Tick,
                    buttonText = if (isSaving) "Registering…" else "Register campsite",
                    customHeight = 58,
                    customWidth = buttonWidth,
                )
                RedButton(
                    onClick = { if (!isSaving) onCancelClick() },
                    buttonText = "Cancel",
                    customHeight = 58,
                    customWidth = buttonWidth,
                )
            }
        }
    }
}

// region Building blocks

/** Back button on the left, page title on the right, as in the Figma "App header". */
@Composable
internal fun ScreenHeader(title: String, onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 24.dp),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) { BackButton(onClick = onBackClick) }
        Text(
            text = title,
            style = AppTextStyles.PageTitle,
            color = AppColors.TextOnPrimary,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text = text, style = AppTextStyles.HeadingMedium, color = AppColors.TextPrimary)
}

/**
 * "Location" row. Shows "Choose on map" until a spot is picked, then the coordinates.
 * Tapping it opens the map either way.
 */
@Composable
private fun LocationPicker(draft: CampsiteDraft, error: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "LOCATION", style = AppTextStyles.SectionLabel, color = AppColors.TextSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .background(AppColors.FillField)
                .border(1.dp, if (error != null) AppColors.Danger else AppColors.OutlineField, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(AppIcons.MapPin, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (draft.hasLocation) {
                    Text(
                        text = formatCoordinates(draft.latitude!!, draft.longitude!!),
                        style = AppTextStyles.TitleSmall,
                        color = AppColors.TextOnPrimary,
                    )
                    Text(text = "Tap to move the pin", style = AppTextStyles.Caption, color = AppColors.TextSecondary)
                } else {
                    Text(
                        text = "CHOOSE ON MAP",
                        style = AppTextStyles.Overline,
                        color = AppColors.TextOnPrimary.copy(alpha = 0.7f),
                    )
                }
            }
            Icon(AppIcons.ChevronRight, contentDescription = "Choose on map", tint = AppColors.TextOnPrimary, modifier = Modifier.size(18.dp))
        }
        if (error != null) {
            Text(text = error, style = AppTextStyles.Caption, color = AppColors.Danger, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/**
 * "Maximum occupancy" row in the form-field style, with − and + buttons instead of typing.
 * The buttons fade out at [RegisterCampsiteViewModel.MIN_OCCUPANCY] and
 * [RegisterCampsiteViewModel.MAX_OCCUPANCY].
 */
@Composable
private fun OccupancyStepper(guests: Int, onChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(AppColors.FillField, shape)
            .border(1.dp, AppColors.OutlineField, shape)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.Guests, contentDescription = null, tint = AppColors.TextOnPrimary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("MAXIMUM OCCUPANCY", style = AppTextStyles.FieldLabel, color = AppColors.Secondary)
            Text(
                text = if (guests == 1) "1 guest" else "$guests guests",
                style = AppTextStyles.ValueMedium,
                color = AppColors.TextOnPrimary,
            )
        }
        StepperButton(AppIcons.Minus, "Fewer guests", enabled = guests > RegisterCampsiteViewModel.MIN_OCCUPANCY) { onChange(-1) }
        Text(
            text = guests.toString(),
            style = AppTextStyles.CounterValue,
            color = AppColors.TextOnPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        StepperButton(AppIcons.Plus, "More guests", enabled = guests < RegisterCampsiteViewModel.MAX_OCCUPANCY) { onChange(1) }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) AppColors.Primary else AppColors.FillSubtle)
            .border(1.dp, if (enabled) AppColors.Primary else AppColors.OutlineStrong, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) AppColors.TextOnPrimary else AppColors.TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Amenity chips plus a chip-shaped box to type a new one.
 *
 * Amenities are short tags, at most [MAX_AMENITY_LENGTH] characters. Typing past that stops at
 * the limit, and the hint under the chips stays on "use the description" for as long as the
 * text is at the limit. Tapping a chip removes it.
 *
 * The limit warning is worked out from the text itself rather than set when a key is pressed:
 * keyboards often send the cut-down text straight back as a second change, which used to clear
 * the warning a moment after it appeared.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmenitiesEditor(
    amenities: List<String>,
    onAddAmenity: (String) -> Boolean,
    onRemoveAmenity: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    // Only for "already added"; cleared as soon as the text actually changes.
    var duplicateMessage by remember { mutableStateOf<String?>(null) }
    val atLimit = text.length >= MAX_AMENITY_LENGTH
    val warning = duplicateMessage
        ?: if (atLimit) "Amenities are $MAX_AMENITY_LENGTH characters max. Put longer details in the description." else null

    fun add() {
        if (text.isBlank()) return
        if (onAddAmenity(text)) {
            text = ""
            duplicateMessage = null
        } else {
            duplicateMessage = "\"${text.trim()}\" is already added"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            amenities.forEach { tag ->
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary)
                        .clickable { onRemoveAmenity(tag) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(AppIcons.Tick, contentDescription = null, tint = AppColors.TextOnPrimary, modifier = Modifier.size(15.dp))
                    Text(text = tag, style = AppTextStyles.TabLabel, color = AppColors.TextOnPrimary)
                    Icon(AppIcons.Cross, contentDescription = "Remove $tag", tint = AppColors.TextOnPrimary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }

            // Unselected-chip look from the Figma, used as the input.
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(AppColors.FillSubtle)
                    .border(1.dp, if (atLimit) AppColors.Accent else AppColors.OutlineStrong, CircleShape)
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { new ->
                        val capped = new.take(MAX_AMENITY_LENGTH)
                        if (capped != text) {
                            text = capped
                            duplicateMessage = null
                        }
                    },
                    singleLine = true,
                    textStyle = AppTextStyles.TabLabel.copy(color = AppColors.TextOnPrimary),
                    cursorBrush = SolidColor(AppColors.Secondary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.width(120.dp),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text("Add amenity", style = AppTextStyles.TabLabel, color = AppColors.TextSecondary)
                        }
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(enabled = text.isNotBlank(), onClick = ::add),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.Plus,
                        contentDescription = "Add amenity",
                        tint = if (text.isNotBlank()) AppColors.Secondary else AppColors.TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Text(
            text = warning ?: "${text.length}/$MAX_AMENITY_LENGTH · Tap a chip to remove it",
            style = AppTextStyles.CaptionMultiline,
            color = if (warning != null) AppColors.Accent else AppColors.TextSecondary,
        )
    }
}

/**
 * Dashed "Promo image" box. Empty: the big "Choose a photo" prompt. With photos: a scrollable
 * row of thumbnails, cover first, each with a remove button.
 */
@Composable
private fun PromoImageUpload(
    photos: List<Uri>,
    onAddPhotosClick: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
) {
    val canAddMore = photos.size < MAX_PHOTOS
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .dashedBorder(AppColors.OutlineField, 16.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (photos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SecondaryTint)
                    .clickable(onClick = onAddPhotosClick),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(AppIcons.AddPhoto, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(36.dp))
                Text("Choose a photo of your campsite", style = AppTextStyles.BodySmall, color = AppColors.TextPrimary)
            }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                photos.forEachIndexed { index, uri ->
                    PhotoThumbnail(uri = uri, isCover = index == 0, onRemove = { onRemovePhoto(uri) })
                }
            }
        }

        Text(
            text = if (photos.isEmpty()) "Up to $MAX_PHOTOS images · Cover photo first"
            else "${photos.size} of $MAX_PHOTOS images · Cover photo first",
            style = AppTextStyles.BodySmallMultiline,
            color = AppColors.TextOnPrimary,
        )

        OutlinedButton(
            onClick = onAddPhotosClick,
            enabled = canAddMore,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (canAddMore) AppColors.Secondary else AppColors.OutlineStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AppColors.Surface,
                contentColor = AppColors.TextPrimary,
                disabledContainerColor = AppColors.Surface,
                disabledContentColor = AppColors.TextSecondary,
            ),
        ) {
            Text(
                text = when {
                    photos.isEmpty() -> "Upload promo image"
                    canAddMore -> "Add more photos"
                    else -> "Photo limit reached"
                },
                style = AppTextStyles.ButtonLabel,
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(uri: Uri, isCover: Boolean, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 120.dp, height = 150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SecondaryTint),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = if (isCover) "Cover photo" else "Campsite photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isCover) {
            Text(
                text = "COVER",
                style = AppTextStyles.Badge,
                color = AppColors.TextOnPrimary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(AppColors.Primary, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(AppColors.Scrim)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Cross, contentDescription = "Remove photo", tint = AppColors.TextOnPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

/** Dashed rounded border, as on the Figma "Promo image upload" box. */
private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    val strokeWidth = 1.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

/** "53.52320, -113.52630" (5 decimals is about 1 m). */
internal fun formatCoordinates(latitude: Double, longitude: Double) =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

// endregion

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412, heightDp = 1700)
@Composable
private fun RegisterCampsiteEmptyPreview() {
    RegisterCampsiteScreen(draft = CampsiteDraft())
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 412, heightDp = 1700)
@Composable
private fun RegisterCampsiteFilledPreview() {
    RegisterCampsiteScreen(
        draft = CampsiteDraft(
            name = "Pine Creek Hideaway",
            address = "14 Pine Creek Road, Big Sur",
            latitude = 36.2704,
            longitude = -121.8081,
            amenities = listOf("Has bathroom", "Water", "Firepit"),
            maxOccupancy = 6,
        ),
        errors = CampsiteDraftErrors(pricePerNight = "Enter a price in whole dollars"),
    )
}

// endregion
