package com.example.daemon_expedite.ui_components

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for every color in the Expedite app.
 *
 * Every value is taken from the Figma file "Expedite". The Figma file has no color styles,
 * so each token notes where the design uses it. Screens should only use these tokens, never
 * raw [Color] values or `.copy(alpha = …)`, so the app stays in step with the design.
 *
 * Translucent colors are defined with the alpha baked in
 * (for example [OutlineSubtle] is white at 8% opacity).
 */
object AppColors {
    // region Backgrounds and surfaces
    /** App background. Used on every screen frame. */
    val Background = Color(0xFF121514)

    /** Reservation and owner campsite cards, lottery cards. */
    val Surface = Color(0xFF2E3129)

    /** Dark green for the bottom navigation, dialogs, sheets, badges and booking summaries. */
    val SurfaceDeep = Color(0xFF161D15)

    /** Lottery badge sitting on top of a campsite photo. */
    val SurfaceBadge = Color(0xFF172018)

    /** Bottom sheet on the welcome screen. */
    val SurfaceSheet = Color(0xFF1C1F1E)

    /** Disabled button and blank avatar container. */
    val SurfaceDisabled = Color(0xFF2E382F)

    /** Location bottom sheet on the map (97% opaque). */
    val SurfaceOverlay = Color(0xF8151D16)

    /** Floating map controls such as zoom and current location (91% opaque). */
    val SurfaceMapControl = Color(0xE8172018)
    // endregion

    // region Brand and accent
    /** Primary brand green. Selected tabs, primary buttons and toggle pill. */
    val Primary = Color(0xFF437657)

    /** [Primary] at 10%. Confirmation icon background, policy callouts. */
    val PrimaryTint = Color(0x1A437657)

    /** [Primary] at 20%. "Pin placed" chip on the location picker. */
    val PrimarySelection = Color(0x33437657)

    /** [Primary] at 28%. Selected item in the bottom navigation. */
    val PrimaryNavSelected = Color(0x47437657)

    /** [Primary] at 35%. Overlay on a selected image in the admin gallery. */
    val PrimarySelected = Color(0x59437657)

    /**
     * [Primary] at 45%. Date ranges already added in a multi-range calendar, and disabled
     * checked boxes. Figma has no drawing of these states.
     */
    val PrimaryMuted = Color(0x73437657)

    /** Sage green for field labels, secondary actions and icons. */
    val Secondary = Color(0xFF90A99A)

    /** Amber used for star ratings and lottery highlights. */
    val Accent = Color(0xFFF2B046)

    /** Destructive actions and warnings, such as leaving a waiting list. */
    val Danger = Color(0xFFD57A72)

    /** [Danger] at 8%. Fill behind danger buttons and warning icons. */
    val DangerTint = Color(0x14D57A72)
    // endregion

    // region Text
    /** Default text and icon color. */
    val TextPrimary = Color(0xFFDCDCDC)

    /** Supporting text, section labels and guidance text. */
    val TextSecondary = Color(0xFFBCC4BA)

    /** Placeholders, helper text, dates and inactive tab and navigation labels. */
    val TextMuted = Color(0xFF909892)

    /** [TextPrimary] at 70%. "Choose on map" label and remove crosses on chips. */
    val TextDim = Color(0xB3DCDCDC)

    /** [TextPrimary] at 50%. Unavailable calendar dates and disabled icons. */
    val TextDisabled = Color(0x80DCDCDC)

    /** Text and icons on top of [Primary], and emphasized headings. */
    val TextOnPrimary = Color(0xFFFFFFFF)

    /** Text and icons on top of [Accent] (the yellow button). */
    val TextOnAccent = Color(0xFF121514)

    /** Placeholder text inside the white search bar (dark green at 70%). */
    val TextPlaceholder = Color(0xB3161D15)
    // endregion

    // region Outlines
    /** White at 6%. Border around the availability calendar. */
    val OutlineFaint = Color(0x0FFFFFFF)

    /** White at 8%. Back buttons, sheets and other hairline borders. */
    val OutlineSubtle = Color(0x14FFFFFF)

    /** White at 10%. Form fields, search fields, tab tracks and dialogs. */
    val OutlineField = Color(0x1AFFFFFF)

    /** White at 12%. Outlined (secondary) buttons, steppers and lottery cards. */
    val OutlineButton = Color(0x1FFFFFFF)

    /** White at 15%. Amenity tags, month arrows, map controls and sheet handles. */
    val OutlineStrong = Color(0x26FFFFFF)

    /** [TextPrimary] at 15%. Campsite cards on Explore and photo arrows. */
    val OutlineCard = Color(0x26DCDCDC)

    /** White at 25%. Dashed border of the promo image upload box. */
    val OutlineDashed = Color(0x40FFFFFF)

    /** White at 30%. Dividers between notifications. */
    val Divider = Color(0x4DFFFFFF)

    /** White at 40%. Border around the owner's lottery calendar. */
    val OutlineBright = Color(0x66FFFFFF)

    /** White at 50%. Unticked checkboxes and selection circles. */
    val OutlineControl = Color(0x80FFFFFF)

    /** [Secondary] at 40%. Reservation and owner campsite card borders. */
    val OutlineAccent = Color(0x6690A99A)

    /** White at 10%. Background contour circles on the loading and welcome screens. */
    val Contour = Color(0x1AFFFFFF)
    // endregion

    // region Translucent fills
    /** White at 4%. Tab tracks, role switch, stepper buttons and the photo upload prompt. */
    val FillTrack = Color(0x0AFFFFFF)

    /** White at 5%. Back buttons, month arrows and chips. */
    val FillSubtle = Color(0x0DFFFFFF)

    /** White at 6%. Form fields, search fields and message bubbles. */
    val FillField = Color(0x0FFFFFFF)

    /** [TextPrimary] at 5%. Campsite cards on Explore. */
    val FillCard = Color(0x0DDCDCDC)

    /** [SurfaceDeep] at 8%. Notification cards. */
    val FillDeepTint = Color(0x14161D15)

    /** [SurfaceDeep] at 60%. Unselected selection circle on images. */
    val FillIndicator = Color(0x99161D15)

    /** White at 80%. */
    val SearchBarBackground = Color(0xCCFFFFFF)

    /** Dark green used for search bar icons. */
    val SearchBarIcon = Color(0xFF161D15)
    // endregion

    // region Scrims
    /** Dim layer behind dialogs and bottom sheets (near-black green at 72%). */
    val Scrim = Color(0xB8071008)

    /** Darker dim layer behind a full-size image preview (88%). */
    val ScrimStrong = Color(0xE0000905)
    // endregion

    // region Map-specific
    /** Water label color on the map. */
    val MapWaterLabel = Color(0xB8A7C1C3)

    /** Trail labels and attribution text on the map ([TextPrimary] at 65%). */
    val MapLabel = Color(0xA6DCDCDC)

    /** Selection radius and halo on the map (primary at 14% and 22%). */
    val MapSelectionRadius = Color(0x24437657)
    val MapSelectionHalo = Color(0x38437657)

    /** Soft shadow under floating map controls (near-black green at 50%). */
    val MapControlShadow = Color(0x80071008)

    /** Shadow under the location bottom sheet (near-black green at 60%). */
    val MapSheetShadow = Color(0x99071008)

    /** Gradient over the map picker: dark top, clear middle, darker bottom. */
    val MapGradientTop = Color(0x66161D15)
    val MapGradientClear = Color(0x00161D15)
    val MapGradientBottom = Color(0xA6071008)
    // endregion
}
