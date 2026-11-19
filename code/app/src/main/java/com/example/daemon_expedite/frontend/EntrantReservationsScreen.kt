package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Rating
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BottomNavBarSpacer
import com.example.daemon_expedite.ui_components.CampsiteCard
import com.example.daemon_expedite.ui_components.CampsitePill
import com.example.daemon_expedite.ui_components.ConfirmDialog
import com.example.daemon_expedite.ui_components.GreenButton
import com.example.daemon_expedite.ui_components.PillVariant
import com.example.daemon_expedite.ui_components.RatingPopup
import com.example.daemon_expedite.ui_components.RedButton
import com.example.daemon_expedite.ui_components.ToggleButtonWithContent

/**
 * The entrant's reservations, with Active, Past and Cancelled tabs.
 * Accessible through the Reservations tab in the shared bottom navigation bar.
 *
 *
 * Laid out like [OwnerLotteriesScreen], with campsite cards in place of lottery cards.
 * Active cards have a "Cancel Reservation" button, which asks first; past cards have
 * "Rate Your Stay", which opens a [RatingPopup] (or show the stars given once rated);
 * cancelled cards have no button.
 *
 * @param state The reservations, or a loading/error state.
 * @param cancellingId ID of the reservation currently being cancelled, or null.
 * @param ratingId ID of the reservation whose rating is being saved, or null.
 * @param onRetry Called when the user retries loading reservations.
 * @param onCancelConfirmed Called after confirming reservation cancellation.
 * @param onRatingSubmitted Called with the reservation, stars and review text when the
 *        rating popup is submitted.
 */
@Composable
fun EntrantReservationsScreen(
    state: UiState<EntrantReservations>,
    cancellingId: Id?,
    ratingId: Id?,
    onRetry: () -> Unit,
    onCancelConfirmed: (Id) -> Unit,
    onRatingSubmitted: (Id, Int, String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // The reservation waiting for the user to confirm cancelling, or null when no dialog is open.
    var askCancel by remember { mutableStateOf<ReservationSummary?>(null) }
    // The past stay being rated in the popup, or null when it's closed.
    var rating by remember { mutableStateOf<ReservationSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
    ) {
        Text(
            text = "My reservations",
            style = AppTextStyles.PageTitle,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(
                start = 28.dp,
                top = 24.dp,
                bottom = 20.dp
            )
        )

        ToggleButtonWithContent(
            options = TABS.map { it.label },
            selectedIndex = tab,
            onOptionSelected = { tab = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp)
                .padding(top = 12.dp, bottom = 16.dp),
        ) { index ->
            // Use index, not tab: during the slide both the old and the new tab are drawn.
            val shown = TABS[index]
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
                is UiState.Content -> ReservationList(
                    reservations = when (shown.status) {
                        ReservationStatus.ACTIVE -> state.data.active
                        ReservationStatus.PAST -> state.data.past
                        ReservationStatus.CANCELLED -> state.data.cancelled
                    },
                    tab = shown,
                    cancellingId = cancellingId,
                    ratingId = ratingId,
                    onCancelClick = { if (cancellingId == null) askCancel = it },
                    onRateStayClick = { if (ratingId == null) rating = it },
                )
            }
        }
        // Room for the shared bottom bar AppNavigation draws on top.
        BottomNavBarSpacer()
    }

    askCancel?.let { reservation ->
        ConfirmDialog(
            title = "Cancel reservation?",
            message = "Your stay at ${reservation.campsite.name} (${reservation.datesText}) will be " +
                "cancelled. This can't be undone.",
            confirmText = "Cancel Reservation",
            dismissText = "Keep it",
            onConfirm = {
                askCancel = null
                onCancelConfirmed(reservation.id)
            },
            onDismiss = { askCancel = null },
        )
    }

    rating?.let { reservation ->
        RatingPopup(
            title = reservation.campsite.name,
            onDismiss = { rating = null },
            onSubmit = { stars ->
                rating = null
                onRatingSubmitted(reservation.id, stars, "")
            },
            onSubmitWithReview = { stars, review ->
                rating = null
                onRatingSubmitted(reservation.id, stars, review)
            },
        )
    }
}

/** One toggle option: its label, the pill on its cards, and what it says when empty. */
private enum class ReservationTab(
    val status: ReservationStatus,
    val label: String,
    val pillText: String,
    val pillVariant: PillVariant,
    val emptyText: String,
) {
    Active(
        ReservationStatus.ACTIVE, "Active", "Confirmed", PillVariant.Outlined,
        "No upcoming stays. Win a lottery to reserve a campsite.",
    ),
    Past(
        ReservationStatus.PAST, "Past", "Completed", PillVariant.Muted,
        "Finished stays will show here.",
    ),
    Cancelled(
        ReservationStatus.CANCELLED, "Cancelled", "Cancelled", PillVariant.Muted,
        "Cancelled reservations will show here.",
    ),
}

private val TABS = ReservationTab.entries

@Composable
private fun ReservationList(
    reservations: List<ReservationSummary>,
    tab: ReservationTab,
    cancellingId: Id?,
    ratingId: Id?,
    onCancelClick: (ReservationSummary) -> Unit,
    onRateStayClick: (ReservationSummary) -> Unit,
) {
    if (reservations.isEmpty()) {
        Centered {
            Text(tab.emptyText, style = AppTextStyles.BodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(reservations, key = { it.id.value.toString() }) { reservation ->
            val campsite = reservation.campsite
            CampsiteCard(
                name = campsite.name,
                location = campsite.location,
                pricePerNight = campsite.pricePerNight,
                imageUrl = campsite.imageUrl,
                rating = campsite.rating,
                containerColor = AppColors.Surface,
                borderColor = AppColors.OutlineAccent,
                pill = { CampsitePill(tab.pillText, variant = tab.pillVariant) },
                action = {
                    ReservationActions(
                        datesText = reservation.datesText,
                        tab = tab,
                        isCancelling = reservation.id == cancellingId,
                        isRating = reservation.id == ratingId,
                        yourRating = reservation.yourRating,
                        onCancelClick = { onCancelClick(reservation) },
                        onRateStayClick = { onRateStayClick(reservation) },
                    )
                },
            )
        }
    }
}

/**
 * The stay's dates, then the tab's button: Cancel Reservation, Rate Your Stay, or none.
 * A past stay that's already rated shows the stars given instead of the button.
 */
@Composable
private fun ReservationActions(
    datesText: String,
    tab: ReservationTab,
    isCancelling: Boolean,
    isRating: Boolean,
    yourRating: Int?,
    onCancelClick: () -> Unit,
    onRateStayClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(datesText, style = AppTextStyles.BodyMedium, color = AppColors.TextPrimary)
        if (tab == ReservationTab.Cancelled) return@Column
        if (tab == ReservationTab.Past && yourRating != null) {
            YourRating(yourRating)
            return@Column
        }
        // The buttons take a fixed width, so measure the card to make them fill it.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt()
            if (tab == ReservationTab.Active) {
                RedButton(
                    onClick = onCancelClick,
                    icon = AppIcons.Cross,
                    buttonText = if (isCancelling) "Cancelling…" else "Cancel Reservation",
                    customWidth = width,
                )
            } else {
                GreenButton(
                    onClick = onRateStayClick,
                    icon = AppIcons.Star,
                    buttonText = if (isRating) "Saving…" else "Rate Your Stay",
                    customWidth = width,
                )
            }
        }
    }
}

/** "You rated this stay" with the stars given, in place of the Rate Your Stay button. */
@Composable
private fun YourRating(stars: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "You rated this stay $stars out of ${Rating.MAX_RATING} stars"
        },
    ) {
        Text("You rated this stay", style = AppTextStyles.BodySmall, color = AppColors.TextSecondary)
        Row {
            repeat(Rating.MAX_RATING) { index ->
                Icon(
                    imageVector = if (index < stars) AppIcons.Star else AppIcons.StarOutline,
                    contentDescription = null,
                    tint = if (index < stars) AppColors.Accent else AppColors.Secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun EntrantReservationsScreenPreview() {
    fun sample(name: String, location: String, price: String, rating: Double, dates: String) = ReservationSummary(
        id = Id(),
        campsite = CampsiteSummary(
            id = Id(),
            name = name,
            location = location,
            pricePerNight = price,
            rating = rating,
            maxOccupancy = 4,
            imageUrl = null,
        ),
        datesText = dates,
    )
    EntrantReservationsScreen(
        state = UiState.Content(
            EntrantReservations(
                active = listOf(
                    sample("Pine Creek Hideaway", "Big Sur, California", "$45", 4.9, "16 Oct – 18 Oct"),
                    sample("Redwood Meadow", "Felton, California", "$38", 4.8, "23 Oct – 25 Oct"),
                ),
                past = listOf(sample("Lakeside Retreat", "Banff, Alberta", "$65", 4.7, "2 Sep – 5 Sep").copy(yourRating = 4)),
                cancelled = emptyList(),
            )
        ),
        cancellingId = null,
        ratingId = null,
        onRetry = {},
        onCancelConfirmed = {},
        onRatingSubmitted = { _, _, _ -> },
    )
}
