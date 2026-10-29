
package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight


import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview


/**
 * Font families used by the Expedite app, extracted from the Figma file.
 */
@Composable
fun SearchBar(
    query: String, // text currently typed into search bar
    onQueryChange: (String) -> Unit, // called when user types search
    onQRCodeClicked: () -> Unit, // called when QR code button clicked
    placeholderText: String, // shown when search bar is empty
    showQrIcon: Boolean // whether to display QR icon
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),

        placeholder = {
            Text(
                text = placeholderText,
                fontFamily = AppFonts.Inter,
                fontWeight = FontWeight.Normal
            )
        },

        textStyle = TextStyle(
            fontFamily = AppFonts.Inter,
            fontWeight = FontWeight.Normal
        ),
        leadingIcon = {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = "Search")

        },
        trailingIcon = {
            if (showQrIcon) {
                IconButton(onQRCodeClicked) {
                    Icon(
                        imageVector = AppIcons.QRCode,
                        contentDescription = "Scan QR code"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = AppColors.SearchBarBackground,
            focusedContainerColor = AppColors.SearchBarBackground,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            cursorColor = AppColors.TextSecondary
        )
    )

}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SearchBarPreview() {
    var query by remember { mutableStateOf("") }

    SearchBar(
        query = query,
        onQueryChange = { query = it },
        placeholderText = "Where do you want to go?",
        showQrIcon = true,
        onQRCodeClicked = {}
    )
}

