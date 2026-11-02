package com.example.daemon_expedite.frontend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.CampsiteCard

/**
 * A scrolling list of campsite cards that also handles loading, error and empty states.
 *
 * Used by both the entrant home page and the owner home page.
 *
 * @param state What to show: a spinner, an error with a retry link, or the campsites.
 * @param emptyText Shown when the list loaded but has no campsites.
 * @param onRetry Called when the error message is tapped.
 * @param onCampsiteClick Called with the id of the tapped campsite.
 */
@Composable
fun CampsiteList(
    state: UiState<List<CampsiteSummary>>,
    emptyText: String,
    onRetry: () -> Unit,
    onCampsiteClick: (Id) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading -> CenteredBox(modifier) {
            CircularProgressIndicator(color = AppColors.Primary)
        }

        is UiState.Error -> CenteredBox(modifier) {
            Text(
                text = "${state.message}. Tap to try again.",
                style = AppTextStyles.BodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(onClick = onRetry).padding(12.dp),
            )
        }

        is UiState.Content -> if (state.data.isEmpty()) {
            CenteredBox(modifier) {
                Text(
                    text = emptyText,
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.data, key = { it.id.value.toString() }) { campsite ->
                    CampsiteCard(
                        name = campsite.name,
                        location = campsite.location,
                        pricePerNight = campsite.pricePerNight,
                        imageUrl = campsite.imageUrl,
                        rating = campsite.rating,
                        maxOccupancy = campsite.maxOccupancy,
                        onClick = { onCampsiteClick(campsite.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121514, heightDp = 300)
@Composable
private fun CampsiteListPreview() {
    Column {
        CampsiteList(
            state = UiState.Content(
                listOf(
                    CampsiteSummary(Id(), "Pine Creek Hideaway", "Big Sur, California", "$45", 4.9, 4, null),
                )
            ),
            emptyText = "No campsites yet",
            onRetry = {},
            onCampsiteClick = {},
        )
    }
}
