package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.ui_components.LotteryStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException
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
 * @property lotteryEndsText When the lottery closes, for example "12 Oct 2026 · 6:00 PM MDT".
 * @property bookingStart First day of the lottery's booking period, shown on the calendar.
 * @property bookingEnd Last day of the lottery's booking period.
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
    val lotteryEndsText: String? = null,
    val bookingStart: LocalDate? = null,
    val bookingEnd: LocalDate? = null,
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
                // TODO: there is no way yet to find an open lottery the viewer hasn't joined
                //  (campsites don't store their lottery ids). Once Database can look up a campsite's
                //  lotteries, return LotteryStatus.OPEN for an active one the viewer isn't in.
                val joined = campsite.activeLotteryAmong(viewer.lotteryIds)
                campsite.toDetails(
                    lottery = joined,
                    lotteryStatus = if (joined != null) LotteryStatus.JOINED else LotteryStatus.NONE,
                )
            }
        }
    }

    /** Loads again after an error, for example from a "try again" link. */
    fun retry() {
        loadedFor?.let { load(it, force = true) }
    }
}

// region Shared with OwnerCampsiteDetailViewModel

internal const val CAMPSITE_NOT_FOUND = "Couldn't find this campsite"

/** The campsite id from a route containing [Routes.CAMPSITE_ID_ARG], or null if it's missing or malformed. */
internal fun SavedStateHandle.campsiteIdArg(): Id? = get<String>(Routes.CAMPSITE_ID_ARG).toCampsiteIdOrNull()

/** Parses a campsite id taken from a route, or returns null if it's missing or malformed. */
@OptIn(ExperimentalUuidApi::class)
internal fun String?.toCampsiteIdOrNull(): Id? = this?.let { runCatching { Id(Uuid.parse(it)) }.getOrNull() }

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
            lotteryEndsText = lottery?.lotteryEnd?.format(zone, END_FORMAT),
            bookingStart = lottery?.bookingStartDate?.toLocalDate(zone),
            bookingEnd = lottery?.bookingEndDate?.toLocalDate(zone),
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
 * The first lottery in [lotteryIds] that includes this campsite and is still running, or null.
 * Lotteries that can't be loaded are skipped.
 */
internal suspend fun Campsite.activeLotteryAmong(lotteryIds: List<Id>): Lottery? = coroutineScope {
    lotteryIds.map { lotteryId ->
        async { quietly("Couldn't load lottery $lotteryId") { Database.fetchLottery(lotteryId) } }
    }.awaitAll().firstOrNull { it != null && id in it.campsites && it.isActive() }
}

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

private fun Instant.toZoned(zone: ZoneId) =
    java.time.Instant.ofEpochMilli(toEpochMilliseconds()).atZone(zone)

private fun Instant.toLocalDate(zone: ZoneId): LocalDate = toZoned(zone).toLocalDate()

private fun Instant.format(zone: ZoneId, formatter: DateTimeFormatter): String = toZoned(zone).format(formatter)

// endregion
