package com.example.daemon_expedite.frontend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.R

/**
 * Displays the initial loading screen for the EXPEDITE application.
 *
 * It contains the Expedite logo and three concentric circles which make the background.
 */
@Composable
fun InitLoadScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff212121)),
        contentAlignment = Alignment.Center
    ){
        Canvas(
            modifier = Modifier.fillMaxSize()
        ){
            val circleColor = Color(0xFF2B302E)
            val center = Offset(
                x = size.width * 0.20f,
                y = size.height * 0.48f
            )
            val radii = listOf(
                size.width * 0.95f,
                size.width * 0.75f,
                size.width * 0.55f
            )

            for (radius in radii){
                drawCircle(
                    color = circleColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                    )
            }
        }
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Expedite Logo",
            modifier = Modifier.size(350.dp)
        )
    }
}

/**
 * Gives preview of the initial loading screen inside Android Studio.
 */
@Preview(showBackground = true)
@Composable
fun InitLoadScreenPreview() {
    InitLoadScreen()
}

