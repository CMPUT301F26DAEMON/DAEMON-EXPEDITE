package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.ui_components.RatingPopup

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class RatingPopupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickingStarReturnsSelectedRating() {
        var submittedRating: Int? = null

        composeTestRule.setContent {
            RatingPopup(
                title = "Pine Creek Hideaway",
                onDismiss = {},
                onSubmit = { rating ->
                    submittedRating = rating
                }
            )
        }

        // Select the third star
        composeTestRule
            .onNodeWithContentDescription("Rate 3 stars")
            .performClick()

        // Submit rating
        composeTestRule
            .onNodeWithText("Submit Rating")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(3, submittedRating)
        }
    }

    @Test
    fun clickingFirstStarReturnsOne() {
        var submittedRating: Int? = null

        composeTestRule.setContent {
            RatingPopup(
                title = "Pine Creek Hideaway",
                onDismiss = {},
                onSubmit = { rating ->
                    submittedRating = rating
                }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Rate 1 star")
            .performClick()

        composeTestRule
            .onNodeWithText("Submit Rating")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, submittedRating)
        }
    }

    @Test
    fun clickingLastStarReturnsMaximumRating() {
        var submittedRating: Int? = null

        composeTestRule.setContent {
            RatingPopup(
                title = "Pine Creek Hideaway",
                onDismiss = {},
                onSubmit = { rating ->
                    submittedRating = rating
                },
                maxStars = 5
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Rate 5 stars")
            .performClick()

        composeTestRule
            .onNodeWithText("Submit Rating")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(5, submittedRating)
        }
    }

    @Test
    fun submittingWithoutSelectingStarDoesNotSubmit() {
        var submittedRating: Int? = null

        composeTestRule.setContent {
            RatingPopup(
                title = "Pine Creek Hideaway",
                onDismiss = {},
                onSubmit = { rating ->
                    submittedRating = rating
                }
            )
        }

        composeTestRule
            .onNodeWithText("Submit Rating")
            .performClick()

        composeTestRule.runOnIdle {
            assertNull(submittedRating)
        }
    }

    @Test
    fun selectingAnotherStarUpdatesRating() {
        var submittedRating: Int? = null

        composeTestRule.setContent {
            RatingPopup(
                title = "Pine Creek Hideaway",
                onDismiss = {},
                onSubmit = { rating ->
                    submittedRating = rating
                }
            )
        }

        // First select 2 stars
        composeTestRule
            .onNodeWithContentDescription("Rate 2 stars")
            .performClick()

        // Then change selection to 4 stars
        composeTestRule
            .onNodeWithContentDescription("Rate 4 stars")
            .performClick()

        composeTestRule
            .onNodeWithText("Submit Rating")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(4, submittedRating)
        }
    }
}