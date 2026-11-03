package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.ScreenHeader

/**
 * The owner's rules for a campsite. Opened from the "Campsite Policies" link on Campsite Details.
 *
 * Built from the "Campsite Policies" frame in the Figma file.
 *
 * @param policies The policy text written by the owner. Blank shows a short placeholder.
 * @param onBackClick Called when the back button is tapped.
 */
@Composable
fun CampsitePoliciesScreen(
    policies: String,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = "Campsite Policies", onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            if (policies.isBlank()) {
                Text(
                    text = "The owner hasn't added any policies for this campsite yet.",
                    style = AppTextStyles.BodySmallMultiline,
                    color = AppColors.TextSecondary,
                )
            } else {
                Text(text = policies, style = AppTextStyles.BodySmallMultiline, color = AppColors.TextPrimary)
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun CampsitePoliciesScreenPreview() {
    CampsitePoliciesScreen(
        policies = "Quiet hours are 10 PM to 7 AM.\n\nPets must be leashed at all times.",
        onBackClick = {},
    )
}
