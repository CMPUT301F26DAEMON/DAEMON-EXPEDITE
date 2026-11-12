package com.example.daemon_expedite.frontend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.R
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.FormTextField
import com.example.daemon_expedite.ui_components.GreenButton
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * Displays the first-launch welcome screen for Expedite.
 *
 * Allows users to enter their name and accept the App Policies
 * before proceeding. The Get started button is enabled only when
 * both requirements are satisfied.
 *
 * @param onContinue Called when the user taps Get started.
 * @param onAppPoliciesClick Called when the user taps App Policies.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit = {},
    onAppPoliciesClick: () -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf("") }

    var agreedToPolicies by rememberSaveable { mutableStateOf(false) }

    val canContinue = name.isNotBlank() && agreedToPolicies

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .imePadding()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val circleCenter = Offset(
                x = size.width * 0.22f,
                y = size.height * 0.29f
            )

            val circleColor = Color(0xFF303330)

            listOf(220.dp, 280.dp, 340.dp).forEach { radius ->
                drawCircle(
                    color = circleColor,
                    radius = radius.toPx(),
                    center = circleCenter,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Expedite logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 143.dp)
                .size(330.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 375.dp)
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp
                    )
                )
                .background(AppColors.SurfaceDeep)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 28.dp,
                        end = 28.dp,
                        top = 30.dp,
                        bottom = 24.dp
                    )
            ) {
                Text(
                    text = "Welcome to Expedite",
                    style = AppTextStyles.PageTitle,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Let's get you set up. What should we call you?",
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(26.dp))

                FormTextField(
                    label = "Your First & last name",
                    value = name,
                    onValueChange = { name = it },
                    icon = AppIcons.Profile,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToPolicies,
                        onCheckedChange = { agreedToPolicies = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AppColors.Primary,
                            uncheckedColor = AppColors.TextSecondary
                        )
                    )

                    Text(
                        text = "I agree to the ",
                        style = AppTextStyles.BodySmall,
                        color = AppColors.TextPrimary
                    )

                    Text(
                        text = "App Policies",
                        style = AppTextStyles.BodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        color = AppColors.TextPrimary,
                        modifier = Modifier.clickable {
                            onAppPoliciesClick()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(15.dp))

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GreenButton(
                        onClick = onContinue,
                        buttonText = "Get started",
                        enabled = canContinue,
                        customWidth = maxWidth.value.toInt(),
                        customHeight = 56
                    )
                }

            }
        }
    }
}

@Preview(
    name = "Welcome Screen - Empty",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        onContinue = {},
        onAppPoliciesClick = {}
    )
}