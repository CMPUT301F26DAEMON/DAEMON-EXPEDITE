package com.example.daemon_expedite.frontend

import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import kotlin.coroutines.cancellation.CancellationException

/**
 * The three states every screen that loads data can be in.
 *
 * A ViewModel exposes one of these, and the screen shows a spinner, an error, or the content.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}

/**
 * Runs [block] and wraps the result in a [UiState].
 *
 * Catches [Throwable], not just [Exception], on purpose: database functions that are still
 * `TODO()` throw `NotImplementedError`, which is an `Error`. Catching it shows an error message
 * on screen instead of crashing the app while the backend is unfinished.
 */
suspend fun <T> loadAsUiState(errorMessage: String, block: suspend () -> T): UiState<T> =
    try {
        UiState.Content(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        android.util.Log.e("UiState", errorMessage, e)
        UiState.Error(errorMessage)
    }

/**
 * Everything a [com.example.daemon_expedite.ui_components.CampsiteCard] needs, already
 * formatted for display. Screens show these instead of raw [Campsite] objects so they don't
 * need to know how prices are formatted or how image URLs are looked up.
 */
data class CampsiteSummary(
    val id: Id,
    val name: String,
    val location: String,
    val pricePerNight: String,
    val rating: Double?,
    val maxOccupancy: Int,
    val imageUrl: String?,
    /** Who registered it. Used to hide your own campsites on entrant home. */
    val ownerId: Id? = null,
)

/**
 * Converts a [Campsite] into a [CampsiteSummary].
 *
 * Images are stored as Firebase Storage paths, which can't be shown directly, so the first
 * image's path is turned into a download URL. If that fails the card shows its default image.
 */
suspend fun Campsite.toSummary(): CampsiteSummary {
    val imageUrl = images.firstOrNull()?.let { image ->
        try {
            Database.getImage(image.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    return CampsiteSummary(
        id = id,
        name = name,
        location = address,
        pricePerNight = "$$pricePerNight",
        rating = rating.takeIf { it.count > 0 }?.average(),
        maxOccupancy = maxOccupancy,
        imageUrl = imageUrl,
        ownerId = ownerId,
    )
}
