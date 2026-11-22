package com.example.daemon_expedite.frontend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * One review of a campsite, as shown on [CampsiteReviews].
 *
 * @property id The rating's id (unique per review, so it's safe as a list key).
 * @property name The reviewer's name, or "Unnamed entrant" if it is blank or couldn't be loaded.
 * @property imageUrl Download URL of the reviewer's profile picture, or null to show the default.
 * @property score Stars given, from 1 to [com.example.daemon_expedite.backend.Rating.MAX_RATING].
 * @property review The written review, or null if the reviewer only gave stars.
 */
data class Reviews(val id: Id, val name: String, val imageUrl: String? = null, val score: Int, val review: String? = null)

/**
 * Everything [CampsiteReviews] shows once loaded.
 *
 * @property campsiteName The campsite's name, shown as the page title.
 * @property reviews Every review left for the campsite.
 */
data class CampsiteReviewsPage(val campsiteName: String, val reviews: List<Reviews>)

/**
 * Data for a campsite's reviews page: every rating left for the campsite, with each
 * reviewer's name and profile picture.
 *
 * The campsite id comes from the navigation route ([Routes.CAMPSITE_ID_ARG]).
 * Call [load] every time the page is shown, so new reviews show up.
 */
class CampsiteReviewsViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val campsiteId: Id? = savedState.campsiteIdArg()

    private val _ratings = MutableStateFlow<UiState<CampsiteReviewsPage>>(UiState.Loading)

    /** The campsite name and its reviews, or the loading / error state. */
    val ratings: StateFlow<UiState<CampsiteReviewsPage>> = _ratings.asStateFlow()

    /** Loads (or reloads) the reviews. What is on screen stays there while it reloads. */
    fun load() {
        val id = campsiteId ?: run {
            _ratings.value = UiState.Error("Couldn't find this campsite")
            return
        }
        viewModelScope.launch {
            if (_ratings.value !is UiState.Content) _ratings.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load reviews") { fetch(id) }
            if (fresh is UiState.Content || _ratings.value !is UiState.Content) _ratings.value = fresh
        }
    }

    /**
     * Fetches the campsite's ratings and, for each one, the author's name and picture.
     * Authors are looked up in parallel; if one can't be loaded, that review still shows
     * with a default name and picture.
     */
    private suspend fun fetch(id: Id): CampsiteReviewsPage = coroutineScope {
        val campsite = Database.fetchCampsite(id)
        val reviews = campsite.ratings.map { rating ->
            async {
                val user = quietly("Couldn't load reviewer ${rating.authorId}") { Database.fetchUser(rating.authorId) }
                val name = user?.get("name")?.takeIf { it.isNotBlank() } ?: "Unnamed entrant"
                val imageUrl = user?.get("profileImage")?.takeIf { it.isNotBlank() }?.let { imageId ->
                    quietly("Couldn't load profile picture for reviewer ${rating.authorId}") { profileImageUrl(imageId) }
                }
                Reviews(
                    id = rating.id,
                    name = name,
                    imageUrl = imageUrl,
                    score = rating.score,
                    review = rating.description,
                )
            }
        }.awaitAll()
        CampsiteReviewsPage(campsiteName = campsite.name, reviews = reviews)
    }

    /** Turns a user's profile image record id into a download URL an image loader can show. */
    private suspend fun profileImageUrl(imageId: String): String {
        val image = Database.fetchImage(Id(Uuid.parse(imageId)))
        return Database.getImage(image.path)
    }
}
