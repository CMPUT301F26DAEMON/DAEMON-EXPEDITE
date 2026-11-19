package com.example.daemon_expedite.frontend

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.ScreenHeader
import java.io.File

/**
 * Screen displaying an app policy or long markdown document.
 *
 * Reuses [ScreenHeader] for navigation and [AppColors] / [AppTextStyles]
 * to maintain visual consistency with the rest of the Expedite application.
 *
 * @param markdownText Markdown text string to render. Defaults to [DEFAULT_APP_POLICY_MARKDOWN].
 * @param title Page title displayed in the header bar. Defaults to "App Policy".
 * @param onBackClick Called when the user taps the back button.
 * @param modifier [Modifier] applied to the screen layout.
 */
@Composable
fun AppPolicyScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    markdownText: String = "",
    title: String = "App Policy",
) {
    val textToDisplay = markdownText.ifBlank { "No policy text found." }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        ScreenHeader(title = title, onBackClick = onBackClick)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            MarkdownText(markdownText = textToDisplay)
        }
    }
}

/**
 * Reusable Markdown renderer composable that parses and displays structured Markdown content.
 * Styled with the app's dark green brand palette and typography.
 *
 * @param markdownText Raw markdown content string to display.
 * @param modifier [Modifier] applied to the text container.
 */
@Composable
fun MarkdownText(
    markdownText: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdownText) { parseMarkdown(markdownText) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            MarkdownBlockItem(block = block)
        }
    }
}

/**
 * Represents structured Markdown elements.
 */
sealed interface MarkdownBlock {
    data class Header(
        val level: Int,
        val text: String,
    ) : MarkdownBlock

    data class Paragraph(
        val text: String,
    ) : MarkdownBlock

    data class BulletItem(
        val text: String,
    ) : MarkdownBlock

    data class NumberedItem(
        val number: String,
        val text: String,
    ) : MarkdownBlock

    data class Blockquote(
        val text: String,
    ) : MarkdownBlock

    data class CodeBlock(
        val code: String,
        val language: String? = null,
    ) : MarkdownBlock

    object HorizontalRule : MarkdownBlock
}

/**
 * Renders an individual parsed [MarkdownBlock] using app brand colors and typography styles.
 */
@Composable
private fun MarkdownBlockItem(block: MarkdownBlock) {
    when (block) {
        is MarkdownBlock.Header -> {
            val (style, color) =
                when (block.level) {
                    1 -> AppTextStyles.PageTitleLarge to AppColors.TextOnPrimary
                    2 -> AppTextStyles.HeadingMedium to AppColors.TextOnPrimary
                    3 -> AppTextStyles.HeadingSmall to AppColors.Secondary
                    else -> AppTextStyles.TitleSmall to AppColors.TextPrimary
                }
            Text(
                text = buildMarkdownAnnotatedString(block.text),
                style = style,
                color = color,
                modifier =
                    Modifier.padding(
                        top = if (block.level == 1) 16.dp else 12.dp,
                        bottom = 8.dp,
                    ),
            )
        }

        is MarkdownBlock.Paragraph -> {
            Text(
                text = buildMarkdownAnnotatedString(block.text),
                style = AppTextStyles.Body,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        is MarkdownBlock.BulletItem -> {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "•",
                    style = AppTextStyles.Body,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = buildMarkdownAnnotatedString(block.text),
                    style = AppTextStyles.Body,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is MarkdownBlock.NumberedItem -> {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = block.number,
                    style = AppTextStyles.BodySmall,
                    color = AppColors.Secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = buildMarkdownAnnotatedString(block.text),
                    style = AppTextStyles.Body,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is MarkdownBlock.Blockquote -> {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(AppColors.PrimaryTint, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, AppColors.OutlineSubtle, shape = RoundedCornerShape(8.dp))
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .background(AppColors.Primary, shape = RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = buildMarkdownAnnotatedString(block.text),
                    style = AppTextStyles.Body.copy(fontStyle = FontStyle.Italic),
                    color = AppColors.TextSecondary,
                )
            }
        }

        is MarkdownBlock.CodeBlock -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(AppColors.SurfaceDeep, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, AppColors.OutlineStrong, shape = RoundedCornerShape(8.dp))
                        .padding(12.dp),
            ) {
                Text(
                    text = block.code,
                    style = AppTextStyles.Caption.copy(fontFamily = FontFamily.Monospace),
                    color = AppColors.Secondary,
                )
            }
        }

        MarkdownBlock.HorizontalRule -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = AppColors.OutlineStrong,
                thickness = 1.dp,
            )
        }
    }
}

/**
 * Parses a raw Markdown string into a list of typed [MarkdownBlock]s.
 */
fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Code blocks: ```
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim().ifEmpty { null }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && (!lines[i].trim().startsWith("```"))) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language))
            continue
        }

        // Horizontal rules: ---, ***, ___
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        // Headers: #, ##, ###, ####
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val text = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Header(level, text))
            i++
            continue
        }

        // Blockquotes: >
        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }

        // Bullet lists: - , * , +
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
            val text = trimmed.drop(2).trim()
            blocks.add(MarkdownBlock.BulletItem(text))
            i++
            continue
        }

        // Numbered lists: 1. , 2.
        val numberedMatch = Regex("""^(\d+\.)\s+(.*)$""").find(trimmed)
        if (numberedMatch != null) {
            val number = numberedMatch.groupValues[1]
            val text = numberedMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(number, text))
            i++
            continue
        }

        // Paragraphs
        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val currentLine = lines[i]
            val currentTrimmed = currentLine.trim()
            if (currentTrimmed.isEmpty() ||
                currentTrimmed.startsWith("```") ||
                currentTrimmed.startsWith("#") ||
                currentTrimmed.startsWith(">") ||
                currentTrimmed.startsWith("- ") ||
                currentTrimmed.startsWith("* ") ||
                currentTrimmed.startsWith("+ ") ||
                Regex("""^(\d+\.)\s+""").containsMatchIn(currentTrimmed) ||
                currentTrimmed == "---" || currentTrimmed == "***" || currentTrimmed == "___"
            ) {
                break
            }
            paragraphLines.add(currentTrimmed)
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
        }
    }
    return blocks
}

/**
 * Transforms inline Markdown markup (`**bold**`, `*italic*`, `` `code` ``, `[text](url)`)
 * into an [AnnotatedString] using app styling.
 */
fun buildMarkdownAnnotatedString(text: String): AnnotatedString =
    buildAnnotatedString {
        val pattern = Regex("""(\*\*(.*?)\*\*|\*(.*?)\*|`(.*?)`|\[(.*?)\]\((.*?)\))""")
        var currentIndex = 0
        pattern.findAll(text).forEach { matchResult ->
            val range = matchResult.range
            if (range.first > currentIndex) {
                append(text.substring(currentIndex, range.first))
            }
            val groupValues = matchResult.groupValues
            val matchedText = groupValues[1]
            when {
                matchedText.startsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppColors.TextOnPrimary)) {
                        append(groupValues[2])
                    }
                }

                matchedText.startsWith("*") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = AppColors.TextSecondary)) {
                        append(groupValues[3])
                    }
                }

                matchedText.startsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = AppColors.Surface,
                            color = AppColors.Secondary,
                        ),
                    ) {
                        append(" ${groupValues[4]} ")
                    }
                }

                matchedText.startsWith("[") -> {
                    withStyle(
                        SpanStyle(
                            color = AppColors.Secondary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append(groupValues[5])
                    }
                }

                else -> {
                    append(matchResult.value)
                }
            }
            currentIndex = range.last + 1
        }
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }

/**
 * Loads a Markdown file from the `assets` folder.
 */
fun loadMarkdownFromAsset(
    context: Context,
    fileName: String,
): String =
    runCatching {
        context.assets
            .open(fileName)
            .bufferedReader()
            .use { it.readText() }
    }.getOrDefault("")

/**
 * Loads a Markdown file from a local filesystem path.
 */
fun loadMarkdownFromFile(filePath: String): String =
    runCatching {
        File(filePath).readText()
    }.getOrDefault("")

/**
 * Loads a Markdown file from a raw resource ID (`R.raw.file_name`).
 */
fun loadMarkdownFromRaw(
    context: Context,
    @RawRes resId: Int,
): String =
    runCatching {
        context.resources
            .openRawResource(resId)
            .bufferedReader()
            .use { it.readText() }
    }.getOrDefault("")

/**
 * Default sample Markdown terms of service & privacy policy text for preview and default state.
 */

private val DEFAULT_APP_POLICY_MARKDOWN = """# EXPEDITE Terms of Service & Privacy Policy

*Last updated: November 2024*

Welcome to **EXPEDITE**. By using our application, booking campsites, or participating in campsite lotteries, you agree to these Terms of Service and Privacy Policy.

---

## 1. Terms of Service

### 1.1 Acceptance of Terms
By accessing or using the EXPEDITE platform, you agree to be bound by these terms. If you do not agree, please do not use our services.

### 1.2 User Accounts & Responsibilities
- Users must provide accurate, complete, and updated profile information.
- You are responsible for safeguarding your account credentials.
- Any activity originating from your account is your sole responsibility.

### 1.3 Campsite Reservations & Lotteries
> **Important Note:** Entering a campsite lottery does not guarantee a reservation. Winning entrants are selected randomly and notified via in-app notifications and email.

1. **Fair Selection:** All lotteries are conducted using random draw algorithms to ensure equal opportunity.
2. **Acceptance Window:** Selected entrants have 48 hours to confirm and complete their reservation.
3. **Cancellations:** Reservations can be canceled up to 7 days prior to check-in for a full refund.

---

## 2. Privacy Policy

### 2.1 Information We Collect
We collect information you provide directly to us:
- **Personal Info:** Name, email address, phone number, and profile picture.
- **Location Data:** Approximate or precise location when searching for nearby campsites.
- **Usage Data:** Device information, app interactions, and booking history.

### 2.2 How We Use Your Data
- To process campsite reservations and lottery entries.
- To send status updates, confirmation receipts, and reminder alerts.
- To improve our recommendation engine and overall user experience.

### 2.3 Data Protection & Security
We employ industry-standard encryption and security measures:
```
Data Encryption: TLS 1.3 in transit, AES-256 at rest
Authentication: Firebase Auth with Secure Credentials
```

### 2.4 Contact Us
If you have any questions or concerns regarding our policy, please contact our support team at [support@expedite.example.com](mailto:support@expedite.example.com).
"""

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun AppPolicyScreenPreview() {
    AppPolicyScreen(
        onBackClick = {},
    )
}
