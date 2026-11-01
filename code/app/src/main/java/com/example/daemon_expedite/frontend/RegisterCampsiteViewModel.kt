package com.example.daemon_expedite.frontend

import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Image
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything typed into the Register campsite form so far.
 *
 * The price is kept as the text the owner typed (digits only) and turned into a number on
 * submit, so an empty field can be told apart from $0.
 *
 * @property latitude Set by the Select location map. Null until a spot has been picked.
 * @property addressFromMap True while [address] is the one the map filled in. Picking a new spot
 * only replaces the address if the owner hasn't typed their own.
 * @property amenities Short tags such as "Water", at most [RegisterCampsiteViewModel.MAX_AMENITY_LENGTH]
 * characters each. Saved as the campsite's tags.
 * @property maxOccupancy Guests allowed. Changed only with the + and − buttons, so it is always
 * between [RegisterCampsiteViewModel.MIN_OCCUPANCY] and [RegisterCampsiteViewModel.MAX_OCCUPANCY].
 * @property photos Photos picked from the gallery, cover photo first. Uploaded on submit.
 */
data class CampsiteDraft(
    val name: String = "",
    val description: String = "",
    val address: String = "",
    val addressFromMap: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val amenities: List<String> = emptyList(),
    val maxOccupancy: Int = RegisterCampsiteViewModel.MIN_OCCUPANCY,
    val pricePerNight: String = "",
    val photos: List<Uri> = emptyList(),
    val rules: String = "",
    val cancellationPolicy: String = "",
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
}

/** Problems found when the form is submitted. A null field means that value is fine. */
data class CampsiteDraftErrors(
    val name: String? = null,
    val address: String? = null,
    val location: String? = null,
    val pricePerNight: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(name, address, location, pricePerNight).all { it == null }
}

/** Outcome of [RegisterCampsiteViewModel.submit]. */
enum class SubmitResult {
    /** Campsite saved and added to the owner. */
    SAVED,

    /** Form incomplete. Nothing was sent; the problems are now in [RegisterCampsiteViewModel.errors]. */
    INVALID,

    /** Saving failed, usually no connection. The form is kept so the owner can try again. */
    FAILED,
}

/**
 * Holds the Register campsite form and saves it.
 *
 * Shared by the Register campsite screen and the Select location map (the map route gets it
 * from the register route's back stack entry), so the map can write the picked spot straight
 * into the form.
 */
class RegisterCampsiteViewModel : ViewModel() {

    private val _draft = MutableStateFlow(CampsiteDraft())
    val draft: StateFlow<CampsiteDraft> = _draft.asStateFlow()

    private val _errors = MutableStateFlow(CampsiteDraftErrors())
    val errors: StateFlow<CampsiteDraftErrors> = _errors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Errors are only shown after the first submit, then kept up to date as the owner fixes them. */
    private var showErrors = false

    /** Applies a change from the form, for example `update { it.copy(name = text) }`. */
    fun update(change: (CampsiteDraft) -> CampsiteDraft) {
        _draft.update(change)
        if (showErrors) _errors.value = validate(_draft.value)
    }

    /** Address typed by the owner. Once they type, the map stops overwriting it. */
    fun onAddressChange(text: String) = update { it.copy(address = text, addressFromMap = false) }

    /**
     * Saves the spot picked on the map. [address] (looked up from the spot) fills the address
     * field unless the owner has typed their own.
     */
    fun setLocation(latitude: Double, longitude: Double, address: String?) = update {
        val replaceAddress = address != null && (it.address.isBlank() || it.addressFromMap)
        it.copy(
            latitude = latitude,
            longitude = longitude,
            address = if (replaceAddress) address!! else it.address,
            addressFromMap = if (replaceAddress) true else it.addressFromMap,
        )
    }

    /**
     * Adds an amenity tag. Ignores blank, too long, or duplicate tags (any letter case), and
     * returns false if the tag wasn't added.
     */
    fun addAmenity(text: String): Boolean {
        val tag = text.trim()
        if (tag.isEmpty() || tag.length > MAX_AMENITY_LENGTH) return false
        if (_draft.value.amenities.any { it.equals(tag, ignoreCase = true) }) return false
        update { it.copy(amenities = it.amenities + tag) }
        return true
    }

    /** The + (1) and − (-1) buttons. Stays within [MIN_OCCUPANCY]..[MAX_OCCUPANCY]. */
    fun changeOccupancy(by: Int) = update {
        it.copy(maxOccupancy = (it.maxOccupancy + by).coerceIn(MIN_OCCUPANCY, MAX_OCCUPANCY))
    }

    /** Price typed by the owner. Anything that isn't a digit is dropped. */
    fun onPriceChange(text: String) = update {
        it.copy(pricePerNight = text.filter { c -> c in '0'..'9' }.take(MAX_PRICE_DIGITS))
    }

    fun removeAmenity(tag: String) = update { it.copy(amenities = it.amenities - tag) }

    /** Adds picked photos, keeping at most [MAX_PHOTOS]. Photos already added are skipped. */
    fun addPhotos(uris: List<Uri>) = update {
        it.copy(photos = (it.photos + uris).distinct().take(MAX_PHOTOS))
    }

    fun removePhoto(uri: Uri) = update { it.copy(photos = it.photos - uri) }

    /**
     * Checks the form and, if it is complete, saves the campsite for [owner].
     *
     * Order matters:
     * 1. Photos are uploaded first, under a new campsite id, because the campsite document
     *    stores their image ids.
     * 2. The campsite is saved with [Database.addCampsite].
     * 3. Its id is added to the owner with [Owner.addCampsite], which saves the owner.
     *
     * If anything fails before the campsite is saved, the photos already uploaded are deleted
     * again so no unused files are left behind.
     *
     * [onResult] says how it went (see [SubmitResult]). The form is cleared after a successful save.
     */
    fun submit(owner: Owner, onResult: (SubmitResult) -> Unit) {
        if (_isSaving.value) return
        val draft = _draft.value
        showErrors = true
        val errors = validate(draft)
        _errors.value = errors
        if (!errors.isEmpty) return onResult(SubmitResult.INVALID)

        _isSaving.value = true
        viewModelScope.launch {
            val campsiteId = Id()
            val uploaded = mutableListOf<Image>()
            var campsiteSaved = false
            val ok = runCatchingAll("Registering campsite failed") {
                for (uri in draft.photos) {
                    uploaded += Database.uploadCampsiteImage(campsiteId, owner.id, uri)
                }
                val campsite = draft.toCampsite(campsiteId, owner.id, uploaded)
                Database.addCampsite(campsite)
                campsiteSaved = true
                owner.addCampsite(campsite)
            }
            if (!ok && !campsiteSaved) {
                uploaded.forEach { image ->
                    runCatchingAll("Couldn't delete unused image ${image.id.value}") {
                        Database.deleteImage(image.id)
                    }
                }
            }
            _isSaving.value = false
            if (ok) reset()
            onResult(if (ok) SubmitResult.SAVED else SubmitResult.FAILED)
        }
    }

    /** Empties the form, for example after Cancel. */
    fun reset() {
        showErrors = false
        _draft.value = CampsiteDraft()
        _errors.value = CampsiteDraftErrors()
    }

    private fun validate(draft: CampsiteDraft) = CampsiteDraftErrors(
        name = if (draft.name.isBlank()) "Give your campsite a name" else null,
        address = if (draft.address.isBlank()) "Enter the campsite's address" else null,
        location = if (!draft.hasLocation) "Pick the campsite's spot on the map" else null,
        pricePerNight = if (draft.pricePerNight.isEmpty()) "Enter a price in whole dollars" else null,
    )

    companion object {
        /** Longest amenity tag. Anything longer belongs in the description. */
        const val MAX_AMENITY_LENGTH = 15

        /** Most photos a campsite can have ("Up to 8 images" in the design). */
        const val MAX_PHOTOS = 8

        /** Fewest and most guests the occupancy buttons allow. */
        const val MIN_OCCUPANCY = 1
        const val MAX_OCCUPANCY = 50

        /** Up to $99,999 a night, which also keeps the price well inside an Int. */
        const val MAX_PRICE_DIGITS = 5
    }
}

/** Builds the [Campsite] to save. Only call once the draft has passed validation. */
private fun CampsiteDraft.toCampsite(id: Id, ownerId: Id, images: List<Image>) = Campsite(
    id = id,
    name = name.trim(),
    description = description.trim(),
    tags = amenities,
    location = Location("").apply {
        latitude = this@toCampsite.latitude!!
        longitude = this@toCampsite.longitude!!
    },
    maxOccupancy = maxOccupancy,
    policies = combinePolicies(rules, cancellationPolicy),
    pricePerNight = pricePerNight.toInt(),
    images = images,
    ownerId = ownerId,
    rating = Rating(),
    address = address.trim(),
)

/**
 * [Campsite] has a single `policies` text, so the form's two guideline fields are saved in it
 * as headed sections:
 * ```
 * Campsite rules:
 * No pets.
 *
 * Cancellation policy:
 * Full refund up to 48 hours before.
 * ```
 * Empty sections are left out. Screens showing policies can split on the two headings.
 */
internal fun combinePolicies(rules: String, cancellationPolicy: String): String =
    buildList {
        if (rules.isNotBlank()) add("$RULES_HEADING\n${rules.trim()}")
        if (cancellationPolicy.isNotBlank()) add("$CANCELLATION_HEADING\n${cancellationPolicy.trim()}")
    }.joinToString("\n\n")

internal const val RULES_HEADING = "Campsite rules:"
internal const val CANCELLATION_HEADING = "Cancellation policy:"
