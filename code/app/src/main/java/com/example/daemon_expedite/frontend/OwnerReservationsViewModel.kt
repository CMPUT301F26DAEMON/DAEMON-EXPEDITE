package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.BookingRejectedException
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.Reservation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Data for the owner's Reservations screen: every reservation at the owner's campsites.
 *
 * Works like [EntrantReservationsViewModel]. [load] fetches everything once, and the screen
 * filters by tab without fetching again. [reject] saves the change and moves the card to
 * Cancelled without reloading the page.
 */
class OwnerReservationsViewModel : ViewModel() {

    private val _reservations = MutableStateFlow<UiState<List<OwnerReservationUi>>>(UiState.Loading)
    val reservations: StateFlow<UiState<List<OwnerReservationUi>>> = _reservations.asStateFlow()

    /** The reservation being rejected right now, or null. */
    private val _rejectingId = MutableStateFlow<Id?>(null)
    val rejectingId: StateFlow<Id?> = _rejectingId.asStateFlow()

    private var owner: Owner? = null

    // What the screen is showing, kept so [reject] can update one card without fetching again.
    private var loaded: List<Reservation> = emptyList()
    private var campsites: Map<Id, Campsite> = emptyMap()
    private var guestNames: Map<Id, String> = emptyMap()

    /** Call every time the screen is shown. What is on screen stays there while it reloads. */
    fun load(owner: Owner) {
        this.owner = owner
        viewModelScope.launch {
            if (_reservations.value !is UiState.Content) _reservations.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load your reservations") { fetch(owner) }
            if (fresh is UiState.Content || _reservations.value !is UiState.Content) _reservations.value = fresh
        }
    }

    fun retry() {
        owner?.let(::load)
    }

    /**
     * Rejects an active reservation through [Database.rejectReservation], which also frees its
     * lottery booking, then moves its card to Cancelled. [onResult] gets null if it worked, or a
     * message to show. Ignored while another reject is still saving.
     */
    fun reject(reservationId: Id, onResult: (String?) -> Unit) {
        if (_rejectingId.value != null) return
        val viewer = owner ?: return onResult(REJECT_FAILED)
        if (loaded.none { it.id == reservationId }) return onResult(REJECT_FAILED)
        _rejectingId.value = reservationId
        viewModelScope.launch {
            val result = try {
                Result.success(Database.rejectReservation(reservationId, viewer.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Rejecting reservation $reservationId failed", e)
                Result.failure(e)
            }
            result.getOrNull()?.let { rejected ->
                loaded = loaded.map { if (it.id == reservationId) rejected else it }
                _reservations.value = UiState.Content(ownerReservationCards(loaded, campsites, guestNames))
            }
            _rejectingId.value = null
            onResult(
                result.exceptionOrNull()?.let { e ->
                    generateSequence(e) { it.cause }.filterIsInstance<BookingRejectedException>().firstOrNull()?.message
                        ?: REJECT_FAILED
                },
            )
        }
    }

    private suspend fun fetch(owner: Owner): List<OwnerReservationUi> = coroutineScope {
        val sites = owner.ownerCampsites.distinct().map { id ->
            async { quietly("Couldn't load campsite $id") { Database.fetchCampsite(id) } }
        }.awaitAll().filterNotNull().associateBy { it.id }

        // A connection error here fails the whole load, so the owner can retry.
        val reservations = Database.fetchReservationsForCampsites(owner.ownerCampsites)

        // Look each guest up once, even if they have several stays.
        val names = reservations.map { it.reservedBy }.distinct().map { id ->
            async {
                val name = quietly("Couldn't load guest $id") { Database.fetchUser(id) }
                    ?.get("name")?.takeIf { it.isNotBlank() }
                id to (name ?: "Unnamed guest")
            }
        }.awaitAll().toMap()

        loaded = reservations
        campsites = sites
        guestNames = names
        ownerReservationCards(reservations, sites, names)
    }

    private companion object {
        const val TAG = "OwnerReservations"
        const val REJECT_FAILED = "Couldn't reject the reservation. Check your connection and try again."
    }
}

/**
 * Formats [reservations] for the owner's cards, in tab order: active ones soonest first, then
 * past and cancelled ones most recent first. [campsites] and [guestNames] are looked up by id;
 * a reservation whose campsite is missing still gets a card.
 */
internal fun ownerReservationCards(
    reservations: List<Reservation>,
    campsites: Map<Id, Campsite>,
    guestNames: Map<Id, String>,
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<OwnerReservationUi> {
    val byStatus = reservations.groupBy { it.status(now, zone) }
    val ordered = byStatus[ReservationStatus.ACTIVE].orEmpty().sortedBy { it.period.start } +
        byStatus[ReservationStatus.PAST].orEmpty().sortedByDescending { it.period.start } +
        byStatus[ReservationStatus.CANCELLED].orEmpty().sortedByDescending { it.period.start }
    return ordered.map { reservation ->
        val campsite = campsites[reservation.campsite]
        val status = when (reservation.status(now, zone)) {
            ReservationStatus.ACTIVE -> OwnerReservationTab.ACTIVE
            ReservationStatus.PAST -> OwnerReservationTab.PAST
            ReservationStatus.CANCELLED -> OwnerReservationTab.CANCELLED
        }
        OwnerReservationUi(
            id = reservation.id.value.toString(),
            campsiteName = campsite?.name ?: "Campsite unavailable",
            location = campsite?.address ?: "It may have been removed",
            status = status,
            pricePerNight = campsite?.pricePerNight,
            rating = campsite?.takeIf { it.ratings.isNotEmpty() }?.averageRating(),
            confirmed = status == OwnerReservationTab.ACTIVE,
            guestName = guestNames[reservation.reservedBy],
            dateRange = reservation.datesText(now, zone),
            nights = reservation.period.nights,
            cancelledByGuest = reservation.isCancelled && !reservation.rejectedByOwner,
        )
    }
}
