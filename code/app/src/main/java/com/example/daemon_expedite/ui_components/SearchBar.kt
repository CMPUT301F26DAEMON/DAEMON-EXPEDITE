package com.example.daemon_expedite.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchBar(
    query: String, // text currently typed into search bar
    onQueryChange: (String) -> Unit, // called when user types search
    onQRCodeClicked: () -> Unit, // called when QR code button clicked
    placeholderText: String, // shown when search bar is empty
    showQrIcon: Boolean // whether to display QR icon
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(AppColors.SearchBarBackground, RoundedCornerShape(50)),
        singleLine = true,
        textStyle = TextStyle(
            color = AppColors.SearchBarIcon,
            fontFamily = AppFonts.Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(AppColors.SearchBarIcon),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = "Search",
                    tint = AppColors.SearchBarIcon,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholderText,
                            color = AppColors.TextPlaceholder,
                            fontFamily = AppFonts.Inter,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                    innerTextField()
                }

                if (showQrIcon) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onQRCodeClicked,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.QRCode,
                            contentDescription = "Scan QR code",
                            tint = AppColors.SearchBarIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SearchBarPreview() {
    var query by remember { mutableStateOf("") }

    Box(modifier = Modifier.padding(16.dp)) {
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholderText = "Where do you want to go?",
            showQrIcon = true,
            onQRCodeClicked = {}
        )
    }
}

