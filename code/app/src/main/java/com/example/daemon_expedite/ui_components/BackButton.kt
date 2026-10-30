package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.daemon_expedite.ui_components.AppIcons.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

/**
 * A  back button
 *
 * @param onClick Action to perform when the button is clicked.
 */
@Composable
fun BackButton(
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = AppColors.TextOnPrimary,
            containerColor = AppColors.OutlineStrong
        )

    ) {
        Icon(
            imageVector = ArrowBack,
            contentDescription = "Go back",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun BackButtonPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        BackButton(
            onClick = {}
        )
    }
}