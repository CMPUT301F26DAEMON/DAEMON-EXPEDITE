
package com.example.daemon_expedite.frontend

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Existing team UI components
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.RedButton

/**
 * Represents information entered in the campsite form.
 *
 * TODO: Connect to the team's campsite model.
 */
data class CampsiteRegistrationDraft(
    val name: String,
    val description: String,
    val address: String,
    val hasBathroom: Boolean,
    val hasWater: Boolean,
    val hasFirepit: Boolean,
    val maxOccupancy: Int,
    val pricePerNight: Double,
    val campsiteRules: String,
    val cancellationPolicy: String,
    val photos: List<Uri>
)

/**
 * Displays the Register Campsite screen.
 *
 * Collects campsite information and validates inputs.
 * Database operations are handled by the caller.
 *
 * TODO: Connect campsite registration to Firestore.
 * TODO: Connect map location navigation.
 * TODO: Upload selected images to Firebase Storage.
 */
@Composable
fun RegisterCampsiteScreen(
    onBackClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onChooseMapClick: () -> Unit = {},
    onRegisterClick: (CampsiteRegistrationDraft) -> Unit = {}
) {

    // -----------------------------
    // Form state
    // -----------------------------

    var campsiteName by rememberSaveable {
        mutableStateOf("")
    }

    var description by rememberSaveable {
        mutableStateOf("")
    }

    var address by rememberSaveable {
        mutableStateOf("")
    }

    var maxOccupancy by rememberSaveable {
        mutableStateOf("")
    }

    var pricePerNight by rememberSaveable {
        mutableStateOf("")
    }

    var campsiteRules by rememberSaveable {
        mutableStateOf("")
    }

    var cancellationPolicy by rememberSaveable {
        mutableStateOf("")
    }

    var hasBathroom by rememberSaveable {
        mutableStateOf(true)
    }

    var hasWater by rememberSaveable {
        mutableStateOf(true)
    }

    var hasFirepit by rememberSaveable {
        mutableStateOf(false)
    }

    var formError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedPhotos by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    // -----------------------------
    // Photo picker
    // -----------------------------

    val isPreview = LocalInspectionMode.current

    val openPhotoPicker: () -> Unit

    if (!isPreview) {
        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { photos ->
            selectedPhotos = (
                    selectedPhotos + photos
                    ).distinct().take(8)
        }

        openPhotoPicker = {
            photoPicker.launch("image/*")
        }
    } else {
        openPhotoPicker = {}
    }

    // -----------------------------
    // Main screen
    // -----------------------------
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(
                state = scrollState,
                enabled = true
            )
            .padding(horizontal = 20.dp)
    ) {

        Spacer(Modifier.height(16.dp))

        // COMPONENT 1: Header

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(
                onClick = onBackClick
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Add campsite",
                style = AppTextStyles.HeadingSmall,
                color = AppColors.TextOnPrimary
            )
        }

        Spacer(Modifier.height(28.dp))

        // COMPONENT 2: Campsite essentials

        FormSectionTitle("Campsite essentials")

        Spacer(Modifier.height(14.dp))

        RegistrationTextField(
            value = campsiteName,
            onValueChange = { campsiteName = it },
            label = "CAMPSITE NAME",
            icon = Icons.Default.Landscape
        )

        Spacer(Modifier.height(12.dp))

        RegistrationTextField(
            value = description,
            onValueChange = { description = it },
            label = "DESCRIPTION",
            icon = Icons.Default.Description,
            multiline = true
        )

        Spacer(Modifier.height(12.dp))

        RegistrationTextField(
            value = address,
            onValueChange = { address = it },
            label = "ADDRESS",
            icon = Icons.Default.LocationOn
        )

        Spacer(Modifier.height(14.dp))

        // COMPONENT 3: Map location

        Text(
            text = "LOCATION",
            style = AppTextStyles.SectionLabel,
            color = AppColors.TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        MapLocationSelector(
            onClick = onChooseMapClick
        )

        Spacer(Modifier.height(24.dp))

        // COMPONENT 4: Amenities

        FormSectionTitle("Amenities")

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            AmenityChip(
                label = "Has bathroom",
                selected = hasBathroom,
                onClick = {
                    hasBathroom = !hasBathroom
                }
            )

            AmenityChip(
                label = "Water",
                selected = hasWater,
                onClick = {
                    hasWater = !hasWater
                }
            )

            AmenityChip(
                label = "Firepit",
                selected = hasFirepit,
                onClick = {
                    hasFirepit = !hasFirepit
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        RegistrationTextField(
            value = maxOccupancy,
            onValueChange = { maxOccupancy = it },
            label = "MAXIMUM OCCUPANCY",
            icon = AppIcons.Profile,
            keyboardType = KeyboardType.Number
        )

        Spacer(Modifier.height(12.dp))

        RegistrationTextField(
            value = pricePerNight,
            onValueChange = { pricePerNight = it },
            label = "PRICE PER NIGHT",
            icon = Icons.Default.AttachMoney,
            keyboardType = KeyboardType.Decimal
        )

        Spacer(Modifier.height(24.dp))

        // COMPONENT 5: Promo images

        FormSectionTitle("Promo image")

        Spacer(Modifier.height(12.dp))

        PromoImagePicker(
            imageCount = selectedPhotos.size,
            onUploadClick = openPhotoPicker
        )

        Spacer(Modifier.height(24.dp))

        // COMPONENT 6: Guest guidelines

        FormSectionTitle("Guest guidelines")

        Spacer(Modifier.height(14.dp))

        RegistrationTextField(
            value = campsiteRules,
            onValueChange = { campsiteRules = it },
            label = "CAMPSITE RULES",
            icon = Icons.Default.Description,
            multiline = true
        )

        Spacer(Modifier.height(12.dp))

        RegistrationTextField(
            value = cancellationPolicy,
            onValueChange = { cancellationPolicy = it },
            label = "CANCELLATION POLICY",
            icon = Icons.Default.Info,
            multiline = true
        )

        Spacer(Modifier.height(16.dp))

        // Show validation error

        if (formError != null) {

            Text(
                text = formError!!,
                style = AppTextStyles.Caption,
                color = AppColors.Danger
            )

            Spacer(Modifier.height(12.dp))
        }

        // COMPONENT 7: Register button

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {

            GreenButton(
                onClick = {

                    val occupancy =
                        maxOccupancy.toIntOrNull()

                    val price =
                        pricePerNight.toDoubleOrNull()

                    when {

                        campsiteName.isBlank() ||
                                description.isBlank() ||
                                address.isBlank() -> {

                            formError =
                                "Please complete the campsite essentials."
                        }

                        occupancy == null ||
                                occupancy <= 0 -> {

                            formError =
                                "Enter a valid maximum occupancy."
                        }

                        price == null ||
                                !price.isFinite() ||
                                price < 0 -> {

                            formError =
                                "Enter a valid nightly price."
                        }

                        campsiteRules.isBlank() ||
                                cancellationPolicy.isBlank() -> {

                            formError =
                                "Please complete the guest guidelines."
                        }

                        else -> {

                            formError = null

                            onRegisterClick(
                                CampsiteRegistrationDraft(
                                    name = campsiteName,
                                    description = description,
                                    address = address,
                                    hasBathroom = hasBathroom,
                                    hasWater = hasWater,
                                    hasFirepit = hasFirepit,
                                    maxOccupancy = occupancy,
                                    pricePerNight = price,
                                    campsiteRules = campsiteRules,
                                    cancellationPolicy = cancellationPolicy,
                                    photos = selectedPhotos
                                )
                            )
                        }
                    }
                },
                icon = AppIcons.Tick,
                buttonText = "Register campsite",
                customWidth = maxWidth.value.toInt(),
                customHeight = 56
            )
        }

        Spacer(Modifier.height(14.dp))

        // COMPONENT 8: Cancel button

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {

            RedButton(
                onClick = onCancelClick,
                icon = AppIcons.Cross,
                buttonText = "Cancel",
                customWidth = maxWidth.value.toInt(),
                customHeight = 56
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Reusable section heading.
 * Uses existing team typography.
 */
@Composable
private fun FormSectionTitle(
    title: String
) {
    Text(
        text = title,
        style = AppTextStyles.HeadingSmall,
        color = AppColors.TextPrimary
    )
}

/**
 * Reusable editable input field.
 * Created locally because InfoCard is read-only.
 */
@Composable
private fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = !multiline,
        minLines = if (multiline) 2 else 1,
        textStyle = AppTextStyles.FieldContent.copy(
            color = AppColors.TextPrimary
        ),
        placeholder = {
            Text(
                text = label,
                style = AppTextStyles.FieldContent,
                color = AppColors.TextSecondary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.FillField,
            unfocusedContainerColor = AppColors.FillField,
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = AppColors.OutlineStrong,
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            cursorColor = AppColors.Primary
        )
    )
}

/**
 * Map location selection field.
 */
@Composable
private fun MapLocationSelector(
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AppColors.Surface,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = AppColors.TextPrimary
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = "CHOOSE ON MAP",
            style = AppTextStyles.FieldContent,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Choose map location",
            tint = AppColors.TextPrimary
        )
    }
}

/**
 * Reusable amenity selection chip.
 */
@Composable
private fun AmenityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = AppTextStyles.Caption
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = AppIcons.Tick,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AppColors.Surface,
            labelColor = AppColors.TextPrimary,
            selectedContainerColor = AppColors.Primary,
            selectedLabelColor = Color.White
        )
    )
}

/**
 * Promo image selection component.
 * Supports selecting up to eight images.
 *
 * Image upload to Firebase Storage is not implemented.
 */
@Composable
private fun PromoImagePicker(
    imageCount: Int,
    onUploadClick: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.Surface,
        border = BorderStroke(
            width = 1.dp,
            color = AppColors.OutlineStrong
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        AppColors.SurfaceDisabled,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onUploadClick)
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Choose campsite photos",
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (imageCount == 0) {
                        "Choose a photo of your campsite"
                    } else {
                        "$imageCount of 8 images selected"
                    },
                    style = AppTextStyles.Caption,
                    color = AppColors.TextSecondary
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Up to 8 images · Cover photo first",
                style = AppTextStyles.Caption,
                color = AppColors.TextSecondary
            )

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onUploadClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    AppColors.Primary
                )
            ) {

                Text(
                    text = "Upload promo image",
                    style = AppTextStyles.TabLabel,
                    color = AppColors.TextPrimary
                )
            }
        }
    }
}

/**
 * Android Studio Compose Preview.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514
)
@Composable
private fun RegisterCampsitePreview() {
    RegisterCampsiteScreen()
}
