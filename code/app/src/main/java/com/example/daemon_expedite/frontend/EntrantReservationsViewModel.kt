package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.BookingRejectedException
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Reservation
import com.example.daemon_expedite.backend.User
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
import kotlin.time.toJavaInstant

/** Which tab of My Reservations a reservation is listed under. See [Reservation.status]. */
enum class ReservationStatus { ACTIVE, PAST, CANCELLED }

/**
 * Cancelled if [Reservation.isCancelled], otherwise past once its check-out day has arrived on
 * [now] in [zone] (see [Reservation.isPast]), otherwise active. A cancelled stay stays under
 * Cancelled even after its dates have gone by.
 */
internal fun Reservation.status(now: Instant, zone: ZoneId = ZoneId.systemDefault()): ReservationStatus = when {
    isCancelled -> ReservationStatus.CANCELLED
    isPast(now.toJavaInstant().atZone(zone).toLocalDate()) -> ReservationStatus.PAST
    else -> ReservationStatus.ACTIVE
}

/** Everything one reservation card shows, already formatted. */
data class ReservationSummary(
    val id: Id,
    val campsite: CampsiteSummary,
    /** For example "16 Oct – 18 Oct". */
    val datesText: String,
)

/** The user's reservations split for the Active, Past and Cancelled tabs. */
data class EntrantReservations(
    val active: List<ReservationSummary>,
    val past: List<ReservationSummary>,
    val cancelled: List<ReservationSummary>,
)

/**
 * Data for the entrant's My Reservations screen.
 *
 * [load] fetches every reservation in [User.reservationIds] once and splits them into the three
 * tabs; switching tabs doesn't fetch anything. [cancel] saves the change and moves the card
 * to Cancelled without reloading the page.
 */
class EntrantReservationsViewModel : ViewModel() {

    private val _reservations = MutableStateFlow<UiState<EntrantReservations>>(UiState.Loading)
    val reservations: StateFlow<UiState<EntrantReservations>> = _reservations.asStateFlow()

    /** The reservation being cancelled right now, or null. Its button shows "Cancelling…". */
    private val _cancellingId = MutableStateFlow<Id?>(null)
    val cancellingId: StateFlow<Id?> = _cancellingId.asStateFlow()

    private var user: User? = null

    // What the screen is showing, kept so [cancel] can update one reservation and regroup
    // the cards without fetching everything again.
    private var loaded: List<Reservation> = emptyList()
    private var campsites: Map<Id, CampsiteSummary> = emptyMap()

    /** Call every time the screen is shown. What is on screen stays there while it reloads. */
    fun load(user: User) {
        this.user = user
        viewModelScope.launch {
            if (_reservations.value !is UiState.Content) _reservations.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load your reservations") { fetch(user) }
            if (fresh is UiState.Content || _reservations.value !is UiState.Content) _reservations.value = fresh
        }
    }

    fun retry() {
        user?.let(::load)
    }

    /**
     * Cancels an active reservation through [Database.cancelReservation], which also frees its
     * lottery booking, then moves its card to the Cancelled tab. [onResult] gets null if it
     * worked, or a message to show. Ignored while another cancellation is still saving.
     */
    fun cancel(reservationId: Id, onResult: (String?) -> Unit) {
        if (_cancellingId.value != null) return
        val viewer = user ?: return onResult(CANCEL_FAILED)
        if (loaded.none { it.id == reservationId }) return onResult(CANCEL_FAILED)
        _cancellingId.value = reservationId
        viewModelScope.launch {
            // Only the saved copy replaces the card, so if the save fails nothing here has
            // changed and the card stays under Active.
            val result = try {
                Result.success(Database.cancelReservation(reservationId, viewer.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cancelling reservation $reservationId failed", e)
                Result.failure(e)
            }
            result.getOrNull()?.let { cancelled ->
                loaded = loaded.map { if (it.id == reservationId) cancelled else it }
                _reservations.value = UiState.Content(groupReservations(loaded, campsites))
            }
            _cancellingId.value = null
            onResult(
                result.exceptionOrNull()?.let { e ->
                    generateSequence(e) { it.cause }.filterIsInstance<BookingRejectedException>().firstOrNull()?.message
                        ?: CANCEL_FAILED
                },
            )
        }
    }

    private suspend fun fetch(user: User): EntrantReservations = coroutineScope {
        // The user only stores ids, so fetch each reservation in parallel. Missing ones are
        // left out (fetchReservation returns null); a connection error fails the whole load.
        val reservations = user.reservationIds.distinct()
            .map { id -> async { Database.fetchReservation(id) } }
            .awaitAll()
            .filterNotNull()

        // Look each campsite up once, even if the user has several stays there.
        val summaries = reservations.map { it.campsite }.distinct().map { id ->
            async { id to quietly("Couldn't load campsite $id") { Database.fetchCampsite(id).toSummary() } }
        }.awaitAll().mapNotNull { (id, summary) -> summary?.let { id to it } }.toMap()

        loaded = reservations
        campsites = summaries
        groupReservations(reservations, summaries)
    }

    private companion object {
        const val TAG = "EntrantReservations"
        const val CANCEL_FAILED = "Couldn't cancel the reservation. Check your connection and try again."
    }
}

/**
 * Splits [reservations] into the three tabs and formats each one for its card.
 *
 * Active stays are listed soonest first; past and cancelled ones most recent first.
 * [campsites] maps campsite ids to their card details. A reservation whose campsite is
 * missing from it (deleted, or failed to load) still gets a card, so it can be cancelled.
 */
internal fun groupReservations(
    reservations: List<Reservation>,
    campsites: Map<Id, CampsiteSummary>,
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): EntrantReservations {
    val byStatus = reservations.groupBy { it.status(now, zone) }
    fun list(status: ReservationStatus, newestFirst: Boolean): List<ReservationSummary> {
        val group = byStatus[status].orEmpty()
        val sorted = if (newestFirst) {
            group.sortedByDescending { it.period.start }
        } else {
            group.sortedBy { it.period.start }
        }
        return sorted.map { it.toSummary(campsites[it.campsite] ?: unavailableCampsite(it.campsite), now, zone) }
    }
    return EntrantReservations(
        active = list(ReservationStatus.ACTIVE, newestFirst = false),
        past = list(ReservationStatus.PAST, newestFirst = true),
        cancelled = list(ReservationStatus.CANCELLED, newestFirst = true),
    )
}

private fun Reservation.toSummary(campsite: CampsiteSummary, now: Instant, zone: ZoneId) =
    ReservationSummary(id = id, campsite = campsite, datesText = datesText(now, zone))

/** "16 Oct – 18 Oct", from the first night to the check-out day. */
internal fun Reservation.datesText(
    now: Instant = Clock.System.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val thisYear = now.toJavaInstant().atZone(zone).year
    return "${period.start.shortDate(thisYear)} – ${period.end.shortDate(thisYear)}"
}

/** Card details for a campsite that couldn't be loaded, so its reservation still shows. */
internal fun unavailableCampsite(id: Id) = CampsiteSummary(
    id = id,
    name = "Campsite unavailable",
    location = "It may have been removed",
    pricePerNight = "—",
    rating = null,
    maxOccupancy = 0,
    imageUrl = null,
)
