package com.example.daemon_expedite.ui_components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Search = Icons.Default.Search
    val Filter = Icons.Default.Tune
    val QRCode = Icons.Default.QrCodeScanner
    val Profile = Icons.Default.Person
    val Star = Icons.Default.Star
    val Dustbin = Icons.Default.Delete
    val Cross = Icons.Default.Close
    val Tick = Icons.Default.Check
    val Calendar = Icons.Default.CalendarMonth
    val EditPencil = Icons.Default.Edit
    val Phone = Icons.Default.Phone
    val Email = Icons.Default.Email
    val ArrowBack = Icons.AutoMirrored.Filled.ArrowBack
    val Reservation = Icons.Outlined.EventAvailable
    val LotteryTicket = Icons.Outlined.ConfirmationNumber
    val Chat = Icons.Outlined.ChatBubbleOutline

    val BellIcon = Icons.Default.Notifications
    val Trophy = Icons.Default.EmojiEvents
    val Waitlist = Icons.Default.FormatListNumbered

    // Register campsite and Select location (Material stand-ins for the Figma Lucide icons).
    val Campsite = Icons.Outlined.Forest
    val Description = Icons.AutoMirrored.Outlined.Notes
    val MapPin = Icons.Outlined.Place
    val Guests = Icons.Outlined.Groups
    val Price = Icons.Outlined.MonetizationOn
    val AddPhoto = Icons.Outlined.AddPhotoAlternate
    val Rules = Icons.AutoMirrored.Outlined.MenuBook
    val Shield = Icons.Outlined.VerifiedUser
    val Plus = Icons.Default.Add
    val Minus = Icons.Default.Remove
    val MyLocation = Icons.Default.MyLocation
    val Move = Icons.Default.OpenWith

    /** Thin chevron from the Figma calendar (20x20, 1.8 stroke). Pointing left. */
    val ChevronLeft: ImageVector by lazy { chevron("ChevronLeft", 12.5f, 7.5f) }

    /** Thin chevron from the Figma calendar (20x20, 1.8 stroke). Pointing right. */
    val ChevronRight: ImageVector by lazy { chevron("ChevronRight", 7.5f, 12.5f) }
}

/** Builds the Figma chevron path M{outerX} 15 L{tipX} 10 L{outerX} 5. */
private fun chevron(name: String, outerX: Float, tipX: Float): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f,
    ).path(
        stroke = SolidColor(Color.Black), // Icon() replaces this with its tint.
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(outerX, 15f)
        lineTo(tipX, 10f)
        lineTo(outerX, 5f)
    }.build()
