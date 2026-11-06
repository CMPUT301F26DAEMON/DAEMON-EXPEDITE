// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the campsite details, policies and map screens from our Figma design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The top row used by inner screens: a [BackButton] on the left and the page title on the right.
 *
 * Built from the "App header" frame in the Expedite Figma file.
 *
 * @param title Page title, for example "Campsite Details".
 * @param onBackClick Called when the back button is tapped.
 * @param modifier [Modifier] applied to the row.
 */
@Composable
fun ScreenHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 24.dp),
    ) {
        Box(Modifier.align(Alignment.CenterStart)) {
            BackButton(onClick = onBackClick)
        }
        Text(
            text = title,
            style = AppTextStyles.PageTitle,
            color = AppColors.TextOnPrimary,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 380)
@Composable
private fun ScreenHeaderPreview() {
    ScreenHeader(title = "Campsite Details", onBackClick = {})
}
