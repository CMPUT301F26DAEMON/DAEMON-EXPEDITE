package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.daemon_expedite.ui_components.AppColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.RatingSummaryCard
import com.example.daemon_expedite.ui_components.ReviewCard
import com.example.daemon_expedite.ui_components.ScreenHeader

/**
 * A campsite's reviews page: a summary card with the average score and star breakdown,
 * followed by every review.
 *
 * @param state The reviews to show, or the loading / error state (from [CampsiteReviewsViewModel.ratings]).
 * @param onBackClick Called when the back button is tapped.
 * @param onRetry Called when the error message is tapped, to try loading again.
 * @param canDelete Admin mode: shows a dustbin top right. Tap a review to select it, then the
 *   dustbin to delete it (after a confirmation dialog), like the admin's Profiles tab.
 * @param isDeleting True while a delete runs; the dustbin does nothing meanwhile.
 * @param onDeleteConfirmed Called with the review's id once the admin confirms the delete.
 * @param initialSelectedReviewId Review selected at first (for previews).
 * @param initialShowDeleteDialog Whether the delete dialog is open at first (for previews).
 */
@Composable
fun CampsiteReviews(
    state: UiState<CampsiteReviewsPage>,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    canDelete: Boolean = false,
    isDeleting: Boolean = false,
    onDeleteConfirmed: (Id) -> Unit = {},
    initialSelectedReviewId: String? = null,
    initialShowDeleteDialog: Boolean = false,
) {
    // Saved as the id's string so it survives rotation (Id isn't Saveable).
    var selectedReviewId by rememberSaveable { mutableStateOf(initialSelectedReviewId) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(initialShowDeleteDialog) }
    // Looked up in the current list, so a review that has gone (deleted, reloaded) can't stay selected.
    val selectedReview = (state as? UiState.Content)?.data?.reviews
        ?.firstOrNull { it.id.value.toString() == selectedReviewId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(
            title = "Reviews",
            onBackClick = onBackClick,
            actions = if (canDelete) {
                {
                    IconButton(
                        onClick = { if (selectedReview != null && !isDeleting) showDeleteDialog = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Dustbin,
                            contentDescription = "Delete review",
                            // Red once a review is selected, so the admin can see it's tappable.
                            tint = if (selectedReview != null) AppColors.Danger else AppColors.TextPrimary,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
            } else {
                null
            },
        )
        when (state) {
            UiState.Loading -> Centered { CircularProgressIndicator(color = AppColors.Primary) }

            is UiState.Error -> Centered {
                Text(
                    text = "${state.message}. Tap to try again.",
                    style = AppTextStyles.BodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(onClick = onRetry).padding(12.dp),
                )
            }

            is UiState.Content -> {
                val reviews = state.data.reviews
                // Campsite name, shown above the summary card (and above "No reviews yet").
                Text(
                    text = state.data.campsiteName,
                    style = AppTextStyles.PageTitle,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp),
                )
                if (reviews.isEmpty()) {
                    Centered {
                        Text(
                            text = "No reviews yet",
                            style = AppTextStyles.BodyMedium,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    // Average score and star breakdown, worked out from the same reviews listed below.
                    RatingSummaryCard(
                        scores = reviews.map { it.score },
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 12.dp),
                    )
                    Text(
                        text = "Reviews",
                        style = AppTextStyles.HeadingSmall,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(reviews, key = { it.id.value.toString() }) { review ->
                            val key = review.id.value.toString()
                            val isSelected = selectedReviewId == key
                            ReviewCard(
                                name = review.name,
                                score = review.score,
                                review = review.review,
                                imageUrl = review.imageUrl,
                                isSelected = isSelected,
                                // Tapping toggles the selection; only admins can select.
                                onClick = if (canDelete) {
                                    { selectedReviewId = if (isSelected) null else key }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedReview != null) {
        ConfirmDialog(
            title = "Delete this review?",
            message = "${selectedReview.name}'s review will be permanently removed " +
                "and the campsite's rating updated. This can't be undone.",
            confirmText = "Yes, delete review",
            dismissText = "Keep review",
            onConfirm = {
                showDeleteDialog = false
                selectedReviewId = null
                onDeleteConfirmed(selectedReview.id)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

/** Centers [content] in the remaining screen space; used for the loading, error and empty states. */
@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/** Preview of the reviews page with four sample reviews. */
@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 390, heightDp = 844)
@Composable
fun PreviewCampsiteReviews() {
    CampsiteReviews(
        state = UiState.Content(
            CampsiteReviewsPage(
                campsiteName = "Pine Creek Hideaway",
                reviews = listOf(
                    Reviews(
                        id = Id(),
                        name = "Maya Patel",
                        score = 5,
                        review = "A peaceful spot by the creek. The site was clean, easy to find, and perfect for a quiet weekend.",
                    ),
                    Reviews(id = Id(), name = "Alex Morgan", score = 5),
                    Reviews(
                        id = Id(),
                        name = "Jordan Lee",
                        score = 4,
                        review = "Beautiful trees and plenty of space. Bring an extra layer, the evenings get chilly by the water.",
                    ),
                    Reviews(
                        id = Id(),
                        name = "Sam Rivera",
                        score = 3,
                        review = "Nice view, but the fire pit was full of ash when we arrived.",
                    ),
                ),
            )
        ),
        onBackClick = {},
        onRetry = {},
    )
}

/** Preview of the reviews page for a campsite nobody has reviewed yet. */
@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCampsiteReviewsEmpty() {
    CampsiteReviews(
        state = UiState.Content(CampsiteReviewsPage(campsiteName = "Pine Creek Hideaway", reviews = emptyList())),
        onBackClick = {},
        onRetry = {},
    )
}

/** Preview of the admin view, with the first review selected so the dustbin is red. */
@Preview(showBackground = true, backgroundColor = 0xFF121514, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCampsiteReviewsAdminSelected() {
    val selected = Id()
    CampsiteReviews(
        state = UiState.Content(
            CampsiteReviewsPage(
                campsiteName = "Pine Creek Hideaway",
                reviews = listOf(
                    Reviews(id = selected, name = "Sam Rivera", score = 1, review = "Spam spam spam."),
                    Reviews(id = Id(), name = "Alex Morgan", score = 5),
                ),
            )
        ),
        onBackClick = {},
        onRetry = {},
        canDelete = true,
        initialSelectedReviewId = selected.value.toString(),
    )
}
