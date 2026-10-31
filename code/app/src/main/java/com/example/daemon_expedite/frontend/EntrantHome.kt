package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppFonts
import com.example.daemon_expedite.ui_components.BottomNavBar
import com.example.daemon_expedite.ui_components.SearchBar

/**
* The main Entrant Home screen.
* Displays a search bar with a QR code scanner, filter and profile buttons
* Includes a list of recommended campsites.
* Includes a bottom navigation bar for accessing reservations, lotteries,
* and messages.
*
*/
@Preview(showBackground = true)
@Composable
fun EntrantHomeScreen() {

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 15.dp,
                    top =30.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Search BAr
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp)

            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholderText = "WHERE DO YOU WANT TO GO?",
                    showQrIcon = true,
                    onQRCodeClicked = {}
                )
            }
            IconButton(onClick = {}) {
                Icon(  //Filter icon
                    imageVector = AppIcons.Filter,
                    contentDescription = "Filter",
                    tint = AppColors.TextOnPrimary
                )
            }
            IconButton(onClick = {}) {
                Icon(  //Profile icon
                    imageVector = AppIcons.Profile,
                    contentDescription = "Profile",
                    tint = AppColors.TextOnPrimary
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 25.dp,
                    end = 25.dp,
                    top = 65.dp
                )
        ) {
            // Heading
            Text(
                text = "Find your next hideaway",
                fontFamily = AppFonts.Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppColors.TextPrimary
            )
            Text(
                text = "Campsites for you",
                fontFamily = AppFonts.Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        // Empty LazyColumn for later
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Campsite cards go here later
        }
        BottomNavBar(
            labels = listOf(
                "Reservations",
                "Lotteries",
                "Messages"
            ),
            icons = listOf(
                AppIcons.Reservation,
                AppIcons.LotteryTicket,
                AppIcons.Chat
            ),
            onClicks = listOf(
                {},
                {},
                {}
            )
        )
    }
}