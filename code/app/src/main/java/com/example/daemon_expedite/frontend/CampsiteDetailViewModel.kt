package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.ui_components.LotteryStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Everything the campsite details screen shows, already formatted for display.
 *
 * @property averageRating Average score, or null when there are no reviews yet.
 * @property reviewCount Number of reviews.
 * @property policies The owner's rules for the campsite, shown on the Campsite Policies page.
 * @property imageUrls Download URLs for the campsite's photos (Storage paths can't be shown directly).
 * @property ownerName Null if the owner's profile couldn't be loaded.
 * @property lotteryStatus Where the viewer stands with this campsite's lottery.
 * @property lotteryId The lottery the card is about, for Join / Leave. Null when there is none.
 * @property lotteryEndsText When entries close, for example "12 Oct 2026 · 11:59 PM MDT".
 * @property lotteryOpensText When entries open, for example "1 Oct".
 * @property stayPeriods The lottery's bookable nights, shown on the calendar.
 */
data class CampsiteDetails(
    val id: Id,
    val name: String,
    val description: String,
    val averageRating: Double?,
    val reviewCount: Int,
    val policies: String,
    val amenities: List<String>,
    val maxOccupancy: Int,
    val pricePerNight: String,
    val imageUrls: List<String>,
    val ownerName: String?,
    val latitude: Double,
    val longitude: Double,
    val lotteryStatus: LotteryStatus,
    val lotteryId: Id? = null,
    val lotteryEndsText: String? = null,
    val lotteryOpensText: String? = null,
    val stayPeriods: List<DatePeriod> = emptyList(),
)

/**
 * Data for the entrant campsite details screen.
 *
 * The campsite id comes from the navigation route (see [Routes.CAMPSITE_DETAIL]).
 * Call [load] with the signed-in user once they are available.
 */
class CampsiteDetailViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val campsiteId: Id? = savedState.campsiteIdArg()

    private val _details = MutableStateFlow<UiState<CampsiteDetails>>(UiState.Loading)
    val details: StateFlow<UiState<CampsiteDetails>> = _details.asStateFlow()

    private var loadedFor: User? = null

    /** Loads the campsite for [viewer]. Only loads once per user unless [force] is true. */
    fun load(viewer: User, force: Boolean = false) {
        if (!force && loadedFor === viewer) return
        loadedFor = viewer
        val id = campsiteId ?: run {
            _details.value = UiState.Error(CAMPSITE_NOT_FOUND)
            return
        }
        viewModelScope.launch {
            _details.value = UiState.Loading
            _details.value = loadAsUiState("Couldn't load this campsite") {
                val campsite = Database.fetchCampsite(id)
                val lottery = campsite.currentLottery()
                val status = lottery?.entrantStatus(viewer.id) ?: LotteryStatus.NONE
                campsite.toDetails(lottery = lottery.takeIf { status != LotteryStatus.NONE }, lotteryStatus = status)
            }
        }
    }

    /** Loads again after an error, for example from a "try again" link. */
    fun retry() {
        loadedFor?.let { load(it, force = true) }
    }

    /**
     * Reloads after joining or leaving, keeping the page on screen meanwhile so it doesn't
     * flash a spinner. [viewer] is the signed-in user, already updated.
     */
    fun refresh(viewer: User) {
        val id = campsiteId ?: return
        loadedFor = viewer
        viewModelScope.launch {
            val fresh = loadAsUiState("Couldn't load this campsite") {
                val campsite = Database.fetchCampsite(id)
                val lottery = campsite.currentLottery()
                val status = lottery?.entrantStatus(viewer.id) ?: LotteryStatus.NONE
                campsite.toDetails(lottery = lottery.takeIf { status != LotteryStatus.NONE }, lotteryStatus = status)
            }
            if (fresh is UiState.Content || _details.value !is UiState.Content) _details.value = fresh
        }
    }
}

/**
 * What the lottery card shows to entrant [viewer] for this lottery. Entrants only act on a
 * lottery while it is upcoming or open; later phases (results, booking) come with the draw.
 */
internal fun Lottery.entrantStatus(viewer: Id, now: Instant = Clock.System.now()): LotteryStatus =
    when (phase(now)) {
        LotteryPhase.UPCOMING -> LotteryStatus.UPCOMING
        LotteryPhase.OPEN -> when {
            viewer in waitingList.users -> LotteryStatus.JOINED
            waitingList.isFull() -> LotteryStatus.FULL
            else -> LotteryStatus.OPEN
        }
        else -> LotteryStatus.NONE
    }

// region Shared with OwnerCampsiteDetailViewModel

internal const val CAMPSITE_NOT_FOUND = "Couldn't find this campsite"

/** The campsite id from a route containing [Routes.CAMPSITE_ID_ARG], or null if it's missing or malformed. */
internal fun SavedStateHandle.campsiteIdArg(): Id? = get<String>(Routes.CAMPSITE_ID_ARG).toIdOrNull()

/** Parses an id taken from a route, or returns null if it's missing or malformed. */
@OptIn(ExperimentalUuidApi::class)
internal fun String?.toIdOrNull(): Id? = this?.let { runCatching { Id(Uuid.parse(it)) }.getOrNull() }

/**
 * Turns a [Campsite] into the [CampsiteDetails] the details screens draw. Photo URLs and the
 * owner's name are looked up at the same time.
 *
 * @param lottery The lottery to show the end time and booking period of, if any.
 * @param lotteryStatus What the lottery card and photo badge should show.
 */
internal suspend fun Campsite.toDetails(lottery: Lottery?, lotteryStatus: LotteryStatus): CampsiteDetails =
    coroutineScope {
        val urls = async { imageUrls() }
        val owner = async { ownerName() }
        val zone = ZoneId.systemDefault()
        CampsiteDetails(
            id = id,
            name = name,
            description = description,
            averageRating = rating.average().takeIf { rating.count > 0 },
            reviewCount = rating.count,
            policies = policies,
            amenities = tags,
            maxOccupancy = maxOccupancy,
            pricePerNight = "$$pricePerNight",
            imageUrls = urls.await(),
            ownerName = owner.await(),
            latitude = location.latitude,
            longitude = location.longitude,
            lotteryStatus = lotteryStatus,
            lotteryId = lottery?.id,
            // One minute before midnight reads as "end of that day".
            lotteryEndsText = lottery?.entryEnd?.minus(1.minutes)?.format(zone, END_FORMAT),
            lotteryOpensText = lottery?.entryFirstDay(zone)?.format(OPENS_FORMAT),
            stayPeriods = lottery?.stayPeriods.orEmpty(),
        )
    }

/** Download URLs for the photos, in order. Photos whose URL can't be found are skipped. */
internal suspend fun Campsite.imageUrls(): List<String> = coroutineScope {
    images.map { image ->
        async { quietly("Couldn't get URL for image ${image.id}") { Database.getImage(image.path) } }
    }.awaitAll().filterNotNull()
}

private suspend fun Campsite.ownerName(): String? =
    quietly("Couldn't load owner $ownerId") { Database.fetchUser(ownerId)["name"] }
        ?.takeIf { it.isNotBlank() }

/**
 * The lottery this campsite is in that isn't finished or cancelled, or null. A campsite is in
 * at most one such lottery at a time.
 */
internal suspend fun Campsite.currentLottery(): Lottery? =
    Database.fetchLotteriesForCampsite(id).firstOrNull { !it.phase().isPast }

/** Runs [block], logging and returning null if it fails, so one missing piece doesn't fail the page. */
internal suspend fun <T> quietly(message: String, block: suspend () -> T): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, message, e)
        null
    }

private const val TAG = "CampsiteDetails"
private val END_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy · h:mm a z")
private val OPENS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

private fun Instant.toZoned(zone: ZoneId) =
    java.time.Instant.ofEpochMilli(toEpochMilliseconds()).atZone(zone)

private fun Instant.format(zone: ZoneId, formatter: DateTimeFormatter): String = toZoned(zone).format(formatter)

// endregion
