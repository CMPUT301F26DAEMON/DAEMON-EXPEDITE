package com.example.daemon_expedite.frontend

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

/**
 * Data for the admin's Images screen ([AdminImagesScreen]): every uploaded image, plus deleting
 * a selection of them.
 *
 * Call [load] every time the screen is shown. Deleting goes through
 * [Database.deleteImageEverywhere], which also clears the image from any user's profile photo
 * and any campsite's photo list.
 */
class AdminImageScreenViewModel : ViewModel() {

    private val _images = MutableStateFlow<UiState<List<AdminImage>>>(UiState.Loading)
    val images: StateFlow<UiState<List<AdminImage>>> = _images.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)

    /** True while a delete runs. Only one runs at a time. */
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    /** Loads (or reloads) every image. What is on screen stays there while it reloads. */
    fun load() {
        viewModelScope.launch {
            if (_images.value !is UiState.Content) _images.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load images") { fetch() }
            if (fresh is UiState.Content || _images.value !is UiState.Content) _images.value = fresh
        }
    }

    /**
     * Deletes the images with [ids] (as on [AdminImage.id]). Each one is deleted on its own, so
     * one failure doesn't stop the rest; deleted images leave the list straight away.
     * Ignored while another delete is running.
     *
     * @param onResult Called with how many of [ids] couldn't be deleted (0 means all are gone).
     */
    fun deleteImages(ids: Set<String>, onResult: (failed: Int) -> Unit) {
        if (ids.isEmpty() || _isDeleting.value) return onResult(0)
        _isDeleting.value = true
        viewModelScope.launch {
            val deleted = coroutineScope {
                ids.map { id ->
                    async {
                        val imageId = id.toIdOrNull() ?: return@async null
                        val ok = runCatchingAll("Deleting image $id failed") {
                            Database.deleteImageEverywhere(imageId)
                        }
                        id.takeIf { ok }
                    }
                }.awaitAll().filterNotNull().toSet()
            }
            (_images.value as? UiState.Content)?.let { current ->
                _images.value = UiState.Content(current.data.filter { it.id !in deleted })
            }
            _isDeleting.value = false
            onResult(ids.size - deleted.size)
        }
    }

    private suspend fun fetch(): List<AdminImage> = coroutineScope {
        val images = Database.fetchAllImages()
        // Many images share an uploader, so each uploader's name is looked up once.
        val names = images.map { it.uploader }.distinct().map { uploader ->
            async { uploader to uploaderName(uploader) }
        }.awaitAll().toMap()
        images.map { image ->
            async {
                AdminImage(
                    id = image.id.value.toString(),
                    imageUrl = quietly("Couldn't get a URL for image ${image.id}") { Database.getImage(image.path) },
                    uploaderName = names[image.uploader] ?: UNKNOWN_UPLOADER,
                )
            }
        }.awaitAll()
    }

    /** The uploader's name, or [UNKNOWN_UPLOADER] if they are gone or have no name. */
    private suspend fun uploaderName(uploader: Id): String =
        quietly("Couldn't load uploader $uploader") { Database.fetchUser(uploader) }
            ?.get("name")
            ?.takeIf { it.isNotBlank() }
            ?: UNKNOWN_UPLOADER

    private companion object {
        const val UNKNOWN_UPLOADER = "Unknown user"
    }
}
