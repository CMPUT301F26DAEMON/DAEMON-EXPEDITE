package com.example.daemon_expedite.frontend_tests

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.daemon_expedite.ui_components.SearchBar
import junit.framework.TestCase
import org.junit.Rule
import org.junit.Test

class SearchBarTest {
        @get:Rule
        val composeTestRule = createComposeRule()

        @Test
        fun placeholderIsDisplayed() {
            composeTestRule.setContent {
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    onQRCodeClicked = {},
                    placeholderText = "Where do you want to go?",
                    showQrIcon = true
                )
            }

            composeTestRule
                .onNodeWithText("Where do you want to go?")
                .assertIsDisplayed()
        }

        @Test
        fun typingUpdatesSearchQuery() {
            val currentQuery = mutableStateOf("")

            composeTestRule.setContent {
                SearchBar(
                    query = currentQuery.value,
                    onQueryChange = { currentQuery.value = it },
                    onQRCodeClicked = {},
                    placeholderText = "Search...",
                    showQrIcon = true
                )
            }

            composeTestRule
                .onNodeWithText("Search...")
                .performTextInput("Edmonton")

            composeTestRule.runOnIdle {
                TestCase.assertEquals("Edmonton", currentQuery.value)
            }
        }

        @Test
        fun qrButtonCallsCallback() {
            var qrClicked = false

            composeTestRule.setContent {
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    onQRCodeClicked = { qrClicked = true },
                    placeholderText = "Search...",
                    showQrIcon = true
                )
            }

            composeTestRule
                .onNodeWithContentDescription("Scan QR code")
                .performClick()

            composeTestRule.runOnIdle {
                TestCase.assertTrue(qrClicked)
            }
        }

        @Test
        fun qrButtonIsHiddenWhenDisabled() {
            composeTestRule.setContent {
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    onQRCodeClicked = {},
                    placeholderText = "Search...",
                    showQrIcon = false
                )
            }

            composeTestRule
                .onNodeWithContentDescription("Scan QR code")
                .assertDoesNotExist()
        }
    }