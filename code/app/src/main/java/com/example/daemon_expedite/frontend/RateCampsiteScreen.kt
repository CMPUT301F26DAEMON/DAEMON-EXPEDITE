package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daemon_expedite.ui_components.*

@Composable
fun RateCampsiteScreen(
    campsiteName: String = "Campsite",
    onBackClick: () -> Unit = {},
    onSubmitRating: (Int) -> Unit = {}
) {
    //Reset rating
    var selectedRating by rememberSaveable(campsiteName) {
        mutableIntStateOf(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val cardWidth = minOf(maxWidth -36.dp, 356.dp)
        val buttonWidth = (cardWidth- 36.dp).value.toInt()
        val cardShape = RoundedCornerShape(24.dp)

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .scale(1.2f)
            ) {
                BackButton(onClick = onBackClick)
            }

            Text(
                text = "Rate your stay",
                style = AppTextStyles.PageTitle.copy(
                    fontSize = 28.sp
                ),
                color = AppColors.TextOnPrimary,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-90).dp)  //Rating card positioning
                .width(cardWidth)
                .background(AppColors.SurfaceDeep, cardShape)
                .border(1.dp, AppColors.OutlineSubtle, cardShape)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = campsiteName,
                fontFamily = AppFonts.Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppColors.TextOnPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            //Stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rate $star stars",
                        tint = if (star <= selectedRating) {
                            Color(0xFFFFB54D)
                        } else {
                            AppColors.Secondary
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                selectedRating = star
                            }
                            .padding(5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Rating
            GreenButton(
                onClick = {
                    onSubmitRating(selectedRating)
                },
                buttonText = "Submit Rating",
                customHeight = 58,
                customWidth = buttonWidth
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF121514,
    widthDp = 402,
    heightDp = 850
)
@Composable
private fun RateCampsiteScreenPreview() {
    RateCampsiteScreen(
        campsiteName = "Pine Creek Hideaway"
    )
}