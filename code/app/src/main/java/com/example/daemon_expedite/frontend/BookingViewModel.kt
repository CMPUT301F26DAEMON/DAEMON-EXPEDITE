// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the lottery booking and decline flow from our rules", 2026-11-05.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.BookingRejectedException
import com.example.daemon_expedite.backend.BookingRules
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * What the booking screen needs once loaded.
 *
 * @property lottery The lottery being booked, as last loaded (its bookings decide which nights are free).
 * @property campsites The lottery's campsites, in order. Deleted ones are left out.
 * @property ownerName The campsite owner's name, or null if it couldn't be loaded.
 */
data class BookingOptions(
    val lottery: Lottery,
    val campsites: List<CampsiteSummary>,
    val ownerName: String?,
)

/**
 * The winner's picks so far.
 *
 * @property campsite The campsite picked.
 * @property firstNight First night picked, or null.
 * @property checkOut Check-out day picked, or null while only the first night is picked.
 * @property agreedToRules True once "I have read the rules" is ticked.
 */
data class BookingSelection(
    val campsite: Id? = null,
    val firstNight: LocalDate? = null,
    val checkOut: LocalDate? = null,
    val agreedToRules: Boolean = false,
) {
    /** The picked nights, or null until both ends are picked. */
    val period: DatePeriod?
        get() = if (firstNight != null && checkOut != null && checkOut.isAfter(firstNight)) DatePeriod(firstNight, checkOut) else null
}

/**
 * Data for the "Make a Reservation" screen, where a lottery winner books their stay or declines.
 *
 * The lottery id (and the campsite to pick first) come from the route
 * (see [Routes.BOOK_LOTTERY_STAY]). Booking itself goes through [SessionViewModel.bookLotteryStay],
 * because it changes the signed-in user; declining doesn't, so it is done here.
 */
class BookingViewModel(savedState: SavedStateHandle) : ViewModel() {

    private val lotteryId: Id? = savedState.get<String>(Routes.LOTTERY_ID_ARG).toIdOrNull()
    private val firstCampsite: Id? = savedState.get<String>(Routes.CAMPSITE_ID_ARG).toIdOrNull()

    private val _options = MutableStateFlow<UiState<BookingOptions>>(UiState.Loading)
    val options: StateFlow<UiState<BookingOptions>> = _options.asStateFlow()

    private val _selection = MutableStateFlow(BookingSelection(campsite = firstCampsite))
    val selection: StateFlow<BookingSelection> = _selection.asStateFlow()

    private val _isDeclining = MutableStateFlow(false)
    val isDeclining: StateFlow<Boolean> = _isDeclining.asStateFlow()

    init {
        load()
    }

    /** Loads (or reloads, for example after a booking was refused) the lottery and its campsites. */
    fun load() {
        val id = lotteryId ?: run {
            _options.value = UiState.Error("Couldn't find this lottery")
            return
        }
        viewModelScope.launch {
            if (_options.value !is UiState.Content) _options.value = UiState.Loading
            val fresh = loadAsUiState("Couldn't load this lottery") {
                coroutineScope {
                    val lottery = Database.fetchLottery(id)
                    val campsites = lottery.campsites.map { campsiteId ->
                        async { quietly("Couldn't load campsite $campsiteId") { Database.fetchCampsite(campsiteId).toSummary() } }
                    }
                    val owner = async {
                        quietly("Couldn't load owner ${lottery.ownerId}") { Database.fetchUser(lottery.ownerId)["name"] }
                    }
                    BookingOptions(lottery, campsites.awaitAll().filterNotNull(), owner.await()?.takeIf { it.isNotBlank() })
                }
            }
            if (fresh is UiState.Content || _options.value !is UiState.Content) _options.value = fresh
            // Pick the first campsite if none (or one not in the lottery) is picked.
            (fresh as? UiState.Content)?.data?.campsites?.let { campsites ->
                if (campsites.none { it.id == _selection.value.campsite }) {
                    _selection.update { it.copy(campsite = campsites.firstOrNull()?.id, firstNight = null, checkOut = null) }
                }
            }
        }
    }

    /** Picks another campsite. Its free nights differ, so the dates are cleared. */
    fun selectCampsite(id: Id) = _selection.update {
        if (it.campsite == id) it else it.copy(campsite = id, firstNight = null, checkOut = null)
    }

    fun onDatesChange(firstNight: LocalDate?, checkOut: LocalDate?) =
        _selection.update { it.copy(firstNight = firstNight, checkOut = checkOut) }

    fun setAgreedToRules(agreed: Boolean) = _selection.update { it.copy(agreedToRules = agreed) }

    /**
     * What's wrong with the current pick for [viewer], or null if it can be booked. Uses the
     * same [BookingRules.check] the database runs, against the lottery as last loaded.
     */
    fun problem(viewer: Id): String? {
        val options = (_options.value as? UiState.Content)?.data ?: return null
        val selection = _selection.value
        val campsite = selection.campsite ?: return "Pick a campsite"
        val period = selection.period ?: return null // still picking
        return BookingRules.check(options.lottery, options.lottery.phase(), viewer, campsite, period)
    }

    /**
     * Turns the win down for [viewer]. [onResult] gets null when it worked, or a message to show.
     */
    fun decline(viewer: Id, onResult: (String?) -> Unit) {
        val id = lotteryId ?: return onResult("Couldn't find this lottery")
        if (_isDeclining.value) return
        _isDeclining.value = true
        viewModelScope.launch {
            val problem = try {
                Database.declineLottery(id, viewer)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                generateSequence<Throwable>(e) { it.cause }.filterIsInstance<BookingRejectedException>().firstOrNull()?.message
                    ?: "Something went wrong. Check your connection and try again.".also {
                        Log.e("BookingViewModel", "Declining lottery $id failed", e)
                    }
            }
            _isDeclining.value = false
            onResult(problem)
        }
    }
}
