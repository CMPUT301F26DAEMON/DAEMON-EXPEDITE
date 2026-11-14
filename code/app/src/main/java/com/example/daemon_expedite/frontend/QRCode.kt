package com.example.daemon_expedite.frontend

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.ScreenHeader
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Defines the target item for QR code generation.
 */
sealed interface QrCodeTarget {
    val id: String
    val title: String
    val formattedContent: String
    val type: String

    data class Campsite(
        val campsiteId: String,
        val campsiteTitle: String = "Campsite",
    ) : QrCodeTarget {
        override val id: String get() = campsiteId
        override val title: String get() = campsiteTitle
        override val formattedContent: String get() = "campsite/$campsiteId"
        override val type = "campsite"
    }

    data class Lottery(
        val lotteryId: String,
        val lotteryTitle: String = "Lottery",
    ) : QrCodeTarget {
        override val id: String get() = lotteryId
        override val title: String get() = lotteryTitle
        override val formattedContent: String get() = "lottery/$lotteryId"
        override val type = "lottery"
    }
}

/**
 * Screen displaying a QR Code for a campsite or lottery with a title bar, save button, and consistent styling.
 *
 * @param target The target item ([QrCodeTarget.Campsite] or [QrCodeTarget.Lottery]).
 * @param onBackClick Action to perform when the back button is clicked.
 * @param modifier [Modifier] applied to the screen layout.
 */
@Composable
fun QrCodeScreen(
    target: QrCodeTarget,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = "QR Code",
            onBackClick = onBackClick,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Share your ${target.type}",
                style = AppTextStyles.PageTitle,
                color = AppColors.TextOnPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Let campers scan this code to view your ${target.type} details.",
                style = AppTextStyles.Body,
                color = AppColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )

            Spacer(modifier = Modifier.height(40.dp))

            QrCodeCard(
                target = target,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            GreenButton(
                onClick = {
                    val success = saveQrCodeToDevice(context, target)
                    val message =
                        if (success) {
                            "QR Code saved to Pictures/Expedite"
                        } else {
                            "Couldn't save QR Code"
                        }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                buttonText = "Download QR Code",
            )
        }
    }
}

/**
 * Convenience composable screen specifically for a Campsite QR Code.
 */
@Composable
fun CampsiteQrCodeScreen(
    campsiteId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    campsiteTitle: String = "Campsite QR Code",
) {
    QrCodeScreen(
        target = QrCodeTarget.Campsite(campsiteId, campsiteTitle),
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/**
 * Convenience composable screen specifically for a Lottery QR Code.
 */
@Composable
fun LotteryQrCodeScreen(
    lotteryId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    lotteryTitle: String = "Lottery QR Code",
) {
    QrCodeScreen(
        target = QrCodeTarget.Lottery(lotteryId, lotteryTitle),
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/**
 * Card component containing the generated QR code, title, content format tag, and instructions.
 */
@Composable
fun QrCodeCard(
    target: QrCodeTarget,
    modifier: Modifier = Modifier,
) {
    val qrBitmap = rememberQrCodeBitmap(content = "expedite://${target.formattedContent}")

    Column(
        modifier =
            modifier
                .background(Color.White, shape = RoundedCornerShape(20.dp))
                .border(1.dp, AppColors.OutlineStrong, shape = RoundedCornerShape(20.dp))
                .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(282.dp)
                    .background(Color.White, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "QR Code for ${target.formattedContent}",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(
                    color = AppColors.Primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Text(
            text = target.title,
            style = AppTextStyles.PageTitle,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scan to view this ${target.type} on Expedite",
            style = AppTextStyles.Caption,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Helper composable that generates an [ImageBitmap] QR code from a string payload.
 */
@Composable
fun rememberQrCodeBitmap(
    content: String,
    sizePx: Int = 512,
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
): ImageBitmap? =
    remember(content, sizePx, foregroundColor, backgroundColor) {
        runCatching {
            val bitmap = generateQrCodeBitmap(content, sizePx, foregroundColor.toArgb(), backgroundColor.toArgb())
            bitmap.asImageBitmap()
        }.getOrNull()
    }

/**
 * Generates a high-resolution QR code [Bitmap] from string content.
 */
fun generateQrCodeBitmap(
    content: String,
    sizePx: Int = 1024,
    foregroundColor: Int = android.graphics.Color.BLACK,
    backgroundColor: Int = android.graphics.Color.WHITE,
): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) foregroundColor else backgroundColor)
        }
    }
    return bitmap
}

/**
 * Saves the QR code image to the device's Pictures/Expedite folder via MediaStore.
 */
fun saveQrCodeToDevice(
    context: Context,
    target: QrCodeTarget,
): Boolean {
    return runCatching {
        val content = "expedite://${target.formattedContent}"
        val bitmap = generateQrCodeBitmap(content, sizePx = 1024)
        val fileName = "Expedite_QR_${target.type}_${target.id}"

        val resolver = context.contentResolver
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Expedite")
            }

        val uri =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        } ?: false
    }.getOrDefault(false)
}

/** Helper function to format campsite QR code payload */
fun formatCampsiteQrContent(campsiteId: String): String = "campsite/$campsiteId"

/** Helper function to format lottery QR code payload */
fun formatLotteryQrContent(lotteryId: String): String = "lottery/$lotteryId"

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun CampsiteQrCodePreview() {
    CampsiteQrCodeScreen(
        campsiteId = "01a1c3a6-579b-769a-be63-907f72e5eaf9",
        campsiteTitle = "Pine Creek Campground",
        onBackClick = {},
    )
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun LotteryQrCodePreview() {
    LotteryQrCodeScreen(
        lotteryId = "01a1d34d-ed61-7353-9e20-23bbf0693a7c ",
        lotteryTitle = "Summer Weekend Lottery",
        onBackClick = {},
    )
}
