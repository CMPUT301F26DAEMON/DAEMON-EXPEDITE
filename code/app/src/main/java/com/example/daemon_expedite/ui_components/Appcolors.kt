package com.example.daemon_expedite.ui_components

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for every color in the Expedite app.
 *
 * Extracted from the Figma file "Expedite".
 *
 * Translucent colors are defined as [Color] values with alpha baked in
 * (for example [OutlineSubtle] is white at 8% opacity).
 */
object AppColors {
    // region Backgrounds and surfaces
    /** App background. Used on every screen frame. */
    val Background = Color(0xFF121514)

    /** Cards, tab tracks and other raised containers. */
    val Surface = Color(0xFF2E3129)

    /** Dark green fill for the bottom navigation, badges and QR code areas. */
    val SurfaceDeep = Color(0xFF161D15)

    /** Disabled button and blank avatar container. */
    val SurfaceDisabled = Color(0xFF2E382F)

    /** Popups and bottom sheets (96 to 98% opaque in the design). */
    val SurfaceOverlay = Color(0xF8151D16)

    /** Floating map controls such as zoom and current location (91% opaque). */
    val SurfaceMapControl = Color(0xE8172018)
    // endregion

    // region Brand and accent
    /** Primary brand green. Selected tabs, primary buttons and toggle pill. */
    val Primary = Color(0xFF437657)

    /** Sage green for secondary actions, icons and muted labels. */
    val Secondary = Color(0xFF90A99A)

    /** Amber used for star ratings and lottery highlights. */
    val Accent = Color(0xFFF2B046)

    /** Destructive actions and warnings, such as leaving a waiting list. */
    val Danger = Color(0xFFD57A72)
    // endregion

    // region Text
    /** Default text and icon color. */
    val TextPrimary = Color(0xFFDCDCDC)

    /** Section labels and guidance text. */
    val TextSecondary = Color(0xFFBCC4BA)

    /** Text and icons on top of [Primary], and emphasized headings. */
    val TextOnPrimary = Color(0xFFFFFFFF)

    /** Placeholder text inside search fields (dark green at 70%). */
    val TextPlaceholder = Color(0xB3161D15)
    // endregion

    // region Outlines and translucent fills
    /** Hairline borders on cards and the navigation bar (white at 8%). */
    val OutlineSubtle = Color(0x14FFFFFF)

    /** Borders on tags and buttons (white at 15%). */
    val OutlineStrong = Color(0x26FFFFFF)

    /** Selected card border (sage at 40%). */
    val OutlineAccent = Color(0x6690A99A)

    /** Faint white fill for chips and icon buttons (white at 5%). */
    val FillSubtle = Color(0x0DFFFFFF)

    /** Form field fill (white at 9%). */
    val FillField = Color(0x17FFFFFF)

    /** White at 80% opacity. */
    val SearchBarBackground = Color(0xCCFFFFFF)

    /** Dark green used for search bar icons. */
    val SearchBarIcon = Color(0xFF161D15)

    /** Tint behind owner-side messages and selection states (primary at 8%). */
    val PrimaryTint = Color(0x14437657)

    /** Tint behind danger buttons (danger at 8%). */
    val DangerTint = Color(0x14D57A72)
    // endregion

    // region Scrims
    /** Dim layer behind modals (85% near-black green). */
    val Scrim = Color(0xD9000905)
    // endregion

    // region Map-specific
    /** Water label color on the map. */
    val MapWaterLabel = Color(0xB8A7C1C3)

    /** Selection radius and halo on the map (primary at 14% and 22%). */
    val MapSelectionRadius = Color(0x24437657)
    val MapSelectionHalo = Color(0x38437657)
    // endregion
}