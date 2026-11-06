
package com.example.daemon_expedite.frontend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.daemon_expedite.R
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.FormTextField
import com.example.daemon_expedite.ui_components.GreenButton

/**
 * First-time welcome screen for Expedite. Displays the application logo, collects a preferred
 * name, and shows a personalized greeting below the name field once a name is entered.
 *
 * The initial instructions disappear when the name is non-blank and return when it is cleared.
 * The space above the name field is preserved so the field does not move between states.
 *
 * Continue is enabled only when a non-blank name is entered. Navigation and persistent storage
 * are handled by the caller.
 *
 * @param name The user's preferred display name.
 * @param onNameChange Called whenever the user edits their name.
 * @param onContinue Called when the user taps "Continue" with a valid name.
 * @param onAppPoliciesClick Called when the user taps "View App Policies".
 */
@Composable
fun WelcomeScreenInteractive(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onAppPoliciesClick: () -> Unit
) {
    val displayName = name.trim()
    val canContinue = displayName.isNotEmpty()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .imePadding()
    ) {
        // GreenButton accepts a width in dp rather than a Modifier.
        val buttonWidth = (maxWidth - 56.dp).value.toInt().coerceAtLeast(1)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Spacer(modifier = Modifier.height(64.dp))

                // Application logo and branding.
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Expedite application logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                Spacer(modifier = Modifier.height(42.dp))

                // Keep the name field at the same position when the instructions disappear.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                ) {
                    if (!canContinue) {
                        Text(
                            text = "Let's get you set up.",
                            style = AppTextStyles.PageTitle,
                            color = AppColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "What should we call you? Enter your preferred name to get started.",
                            style = AppTextStyles.BodyLarge,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Name field remains visible and stationary in both states.
                FormTextField(
                    label = "Name",
                    value = name,
                    onValueChange = onNameChange,
                    icon = AppIcons.Profile,
                    modifier = Modifier.fillMaxWidth()
                )

                // Personalized greeting appears below the name field.
                if (canContinue) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Hi, $displayName! 👋",
                        style = AppTextStyles.PageTitle,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "You're all set! Tap Continue to start exploring.",
                        style = AppTextStyles.BodyLarge,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // Continue button and App Policies link.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GreenButton(
                    onClick = {
                        if (canContinue) {
                            onContinue()
                        }
                    },
                    buttonText = "Continue",
                    icon = AppIcons.ArrowForward,
                    enabled = canContinue,
                    customWidth = buttonWidth
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClick = onAppPoliciesClick
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Shield,
                        contentDescription = null,
                        tint = AppColors.Secondary
                    )

                    Text(
                        text = "View App Policies",
                        style = AppTextStyles.BodyMedium.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        color = AppColors.Secondary
                    )
                }
            }
        }
    }
}

/**
 * Stores the preferred name locally and displays [WelcomeScreenInteractive].
 *
 * The name survives supported configuration changes through rememberSaveable. It is not saved
 * permanently; navigation and persistence are handled separately.
 *
 * @param onContinue Called when the user taps "Continue".
 * @param onAppPoliciesClick Called when the user taps "View App Policies".
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onAppPoliciesClick: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }

    WelcomeScreenInteractive(
        name = name,
        onNameChange = { name = it },
        onContinue = onContinue,
        onAppPoliciesClick = onAppPoliciesClick
    )
}

/** Preview of the initial welcome screen with an empty name field and disabled Continue button. */
@Preview(
    name = "Welcome Screen - Empty",
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        onContinue = {},
        onAppPoliciesClick = {}
    )
}

/** Preview with a personalized greeting and enabled Continue button. */
@Preview(
    name = "Welcome Screen - Name Entered",
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun WelcomeScreenFilledPreview() {
    WelcomeScreenInteractive(
        name = "Michael Scott",
        onNameChange = {},
        onContinue = {},
        onAppPoliciesClick = {}
    )
}
