package com.example.daemon_expedite.ui_components

import com.example.daemon_expedite.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp


/**
 * Font families used by the Expedite app, extracted from the Figma file.
*/
object AppFonts {

    /** Body text, labels, tabs, buttons and form fields. */
    val Inter = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
        Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
    )

    /** Serif used for page titles, headings, campsite names and calendar dates. */
    val Fraunces = FontFamily(
        Font(R.font.fraunces_bold, FontWeight.Bold),
    )

    /** Geometric sans used only for the EXPEDITE wordmark (Jost). */
    val Wordmark = FontFamily(
        Font(R.font.jost_regular, FontWeight.Normal),
    )
}

/**
 * Every text style in the Expedite app, extracted from the Figma file.
 *
 * Figma has no text styles defined, so these are the roughly 45 unique
 * combinations found in the design, merged into named roles. Where Figma used
 * "Auto" line height the style leaves it unspecified; otherwise the line height
 * is expressed in `em` so it scales with the font size.
 *
 * Styles marked "uppercase" were set to uppercase in Figma. Compose has no
 * text-case property, so call `.uppercase()` on the string you display.
 */
object AppTextStyles {

    // region Fraunces (serif headings)

    /** Large screen titles such as "Messages". Fraunces Bold 28. */
    val PageTitleLarge = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 28.sp)

    /** Screen titles such as "My Reservations" and big headings. Fraunces Bold 24. */
    val PageTitle = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp)

    /** Multi-line version of [PageTitle] with a 1.4 line height. */
    val PageTitleMultiline = PageTitle.copy(lineHeight = 1.4.em)

    /** Section headings such as "Past adventures". Fraunces Bold 20, 1.45 line height. */
    val HeadingMedium = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 1.45.em)

    /** Amounts such as "$45". Fraunces Bold 20. */
    val Amount = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 20.sp)

    /** Small section headings such as "Upcoming stay". Fraunces Bold 18, 1.45 line height. */
    val HeadingSmall = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 1.45.em)

    /** Campsite names on cards. Fraunces Bold 18. */
    val CardTitle = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 18.sp)

    /** Large button labels such as "Reserve Campsite". Fraunces Bold 19. */
    val ButtonLabelLarge = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 19.sp)

    /** Standard button labels such as "Cancel reservation". Fraunces Bold 16. */
    val ButtonLabel = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 16.sp)

    /** Dates in the availability calendar. Fraunces Bold 14. */
    val CalendarDateSelected = TextStyle(fontFamily = AppFonts.Fraunces, fontWeight = FontWeight.Bold, fontSize = 14.sp)

    // endregion

    // region Inter (body and UI)

    /** Prominent body copy such as the welcome tagline. Inter Regular 16, 1.45 line height. */
    val BodyLarge = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 1.45.em, letterSpacing = 0.2.sp)

    /** Descriptions and dialog text. Inter Regular 15, 1.5 line height. */
    val Body = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 1.5.em)

    /** Messages and calendar numbers. Inter Regular 14, 1.4 line height. */
    val BodyMedium = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 1.4.em)

    /** Secondary details such as locations and occupancy. Inter Regular 13. */
    val BodySmall = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp)

    /** Multi-line version of [BodySmall] with a 1.4 line height. */
    val BodySmallMultiline = BodySmall.copy(lineHeight = 1.4.em)

    /** Captions and legends such as "/ night". Inter Regular 12. */
    val Caption = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp)

    /** Multi-line version of [Caption] with a 1.4 line height. */
    val CaptionMultiline = Caption.copy(lineHeight = 1.4.em)

    /** Status bar clock. Inter Medium 14. */
    val StatusBar = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp)

    /** Profile values such as name and email. Inter Medium 15. */
    val ValueMedium = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)

    /** Names and guest counts. Inter SemiBold 15. */
    val ValueEmphasis = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

    /** Prices such as "$45". Inter SemiBold 16. */
    val Price = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

    /** Addresses and small titles. Inter SemiBold 14. */
    val TitleSmall = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

    /** Tab and toggle labels. Inter SemiBold 13. */
    val TabLabel = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

    /** Bottom navigation labels. Inter SemiBold 12. */
    val NavLabel = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

    /** Status badges such as "Confirmed". Inter SemiBold 11. */
    val Badge = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)

    /** Filled button labels such as "Apply filters". Inter Bold 16. */
    val ButtonLabelSans = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp)

    /** Bold numbers such as the guest counter. Inter Bold 18. */
    val CounterValue = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp)

    /** Selected calendar dates. Inter Bold 14. */
    val CalendarDate = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp)

    /** Uppercase form labels and placeholders. Inter Regular 11, 0.5 letter spacing. Uppercase. */
    val Overline = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.5.sp)

    /** Uppercase section labels such as "Location". Inter Regular 11, 0.7 letter spacing. Uppercase. */
    val SectionLabel = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.7.sp)

    /** Uppercase text inside form fields. Inter Medium 11, 1.45 line height, 0.5 letter spacing. Uppercase. */
    val FieldContent = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 1.45.em, letterSpacing = 0.5.sp)

    /** Tiny uppercase field labels such as "Guests". Inter Medium 10. Uppercase. */
    val FieldLabel = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Medium, fontSize = 10.sp)

    // endregion

    // region Map labels

    /** Trail labels on the map. Inter SemiBold 11, 0.3 letter spacing. Uppercase. */
    val MapTrailLabel = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.3.sp)

    /** Water labels on the map. Inter Italic 10, 0.4 letter spacing. */
    val MapWaterLabel = TextStyle(fontFamily = AppFonts.Inter, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic, fontSize = 10.sp, letterSpacing = 0.4.sp)

    // endregion

    // region Brand

    /** Large EXPEDITE wordmark on the welcome screen. Jost 30, 0.5em letter spacing. */
    val WordmarkLarge = TextStyle(fontFamily = AppFonts.Wordmark, fontWeight = FontWeight.Normal, fontSize = 30.sp, letterSpacing = 0.5.em)

    /** Small EXPEDITE wordmark in the top bar. Jost 16, 0.28em letter spacing. */
    val WordmarkSmall = TextStyle(fontFamily = AppFonts.Wordmark, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.28.em)

    // endregion
}