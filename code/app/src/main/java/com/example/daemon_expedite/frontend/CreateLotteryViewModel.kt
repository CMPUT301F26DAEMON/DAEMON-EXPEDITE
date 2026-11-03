package com.example.daemon_expedite.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryProblems
import com.example.daemon_expedite.backend.LotteryRules
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.WaitingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** A campsite the owner can put in a new lottery. */
data class CampsiteOption(val id: Id, val name: String)

/**
 * Everything entered on the Create lottery form so far.
 *
 * Number fields keep the digits typed, so an empty field can be told apart from 0.
 *
 * @property campsites Picked campsites.
 * @property entryFirstDay First day entrants can join.
 * @property entryLastDay Last day entrants can join; winners are drawn at the end of it.
 * @property stayPeriods Nights winners can book, sorted by first night.
 * @property winnerCount Digits typed in "Number of winners".
 * @property waitlistCapacity Digits typed in "Waitlist capacity"; empty means no limit.
 */
data class LotteryDraft(
    val campsites: Set<Id> = emptySet(),
    val entryFirstDay: LocalDate? = null,
    val entryLastDay: LocalDate? = null,
    val stayPeriods: List<DatePeriod> = emptyList(),
    val winnerCount: String = "",
    val waitlistCapacity: String = "",
) {
    /** Most winners allowed with the campsites and nights picked so far. */
    val maxWinners: Int get() = LotteryRules.maxWinners(campsites.size, stayPeriods)
}

/**
 * Holds the Create lottery form and saves it.
 *
 * Call [load] with the owner and their campsites. Campsites already in a lottery that isn't
 * finished or cancelled are left out of the picker: a campsite can only be in one at a time.
 */
class CreateLotteryViewModel(
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _options = MutableStateFlow<UiState<List<CampsiteOption>>>(UiState.Loading)
    val options: StateFlow<UiState<List<CampsiteOption>>> = _options.asStateFlow()

    private val _draft = MutableStateFlow(LotteryDraft())
    val draft: StateFlow<LotteryDraft> = _draft.asStateFlow()

    private val _problems = MutableStateFlow(LotteryProblems())
    val problems: StateFlow<LotteryProblems> = _problems.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Problems are only shown after the first submit, then kept up to date as they are fixed. */
    private var showProblems = false

    private var loadedFor: Owner? = null
    private var lastCampsites: List<CampsiteSummary> = emptyList()

    /** The owner's today. Entries can't open before it. */
    val today: LocalDate get() = LocalDate.now(zone)

    /**
     * Works out which of [campsites] (the owner's, as shown on Owner Home) are free to use.
     * Only loads once per owner.
     */
    fun load(owner: Owner, campsites: List<CampsiteSummary>) {
        if (loadedFor === owner) return
        loadedFor = owner
        lastCampsites = campsites
        viewModelScope.launch {
            _options.value = UiState.Loading
            _options.value = loadAsUiState("Couldn't load your campsites") {
                val busy = Database.fetchOwnerLotteries(owner.id)
                    .filterNot { it.phase().isPast }
                    .flatMap { it.campsites }
                    .toSet()
                campsites.filter { it.id !in busy }.map { CampsiteOption(it.id, it.name) }
            }
        }
    }

    /** Loads the campsite options again after an error. */
    fun retry() {
        val owner = loadedFor ?: return
        val campsites = lastCampsites
        loadedFor = null
        load(owner, campsites)
    }

    // region Edits

    fun toggleCampsite(id: Id) = update {
        it.copy(campsites = if (id in it.campsites) it.campsites - id else it.campsites + id)
    }

    fun setEntryFirstDay(day: LocalDate) = update {
        // Keep the close date from ending up before the open date.
        val last = it.entryLastDay?.takeUnless { last -> last.isBefore(day) }
        it.copy(entryFirstDay = day, entryLastDay = last)
    }

    fun setEntryLastDay(day: LocalDate) = update { it.copy(entryLastDay = day) }

    /** Adds the nights from [firstNight] to the day before [checkOut]. */
    fun addStayPeriod(firstNight: LocalDate, checkOut: LocalDate) = update {
        it.copy(stayPeriods = (it.stayPeriods + DatePeriod(firstNight, checkOut)).sortedBy { p -> p.start })
    }

    fun removeStayPeriod(index: Int) = update {
        it.copy(stayPeriods = it.stayPeriods.filterIndexed { i, _ -> i != index })
    }

    fun onWinnerCountChange(text: String) = update { it.copy(winnerCount = text.digits()) }

    fun onWaitlistCapacityChange(text: String) = update { it.copy(waitlistCapacity = text.digits()) }

    private fun update(change: (LotteryDraft) -> LotteryDraft) {
        _draft.update(change)
        if (showProblems) _problems.value = check(_draft.value)
    }

    // endregion

    /**
     * Checks the form and, if it is fine, saves the lottery for [owner] with
     * [Owner.addOwnerLottery], which also records it on the owner in the same write.
     * [onResult] says how it went. The form is kept on failure so the owner can try again.
     */
    fun submit(owner: Owner, onResult: (SubmitResult) -> Unit) {
        if (_isSaving.value) return
        val draft = _draft.value
        showProblems = true
        val problems = check(draft)
        _problems.value = problems
        if (!problems.isEmpty) return onResult(SubmitResult.INVALID)

        _isSaving.value = true
        viewModelScope.launch {
            val ok = runCatchingAll("Creating lottery failed") {
                owner.addOwnerLottery(draft.toLottery(owner.id))
            }
            _isSaving.value = false
            onResult(if (ok) SubmitResult.SAVED else SubmitResult.FAILED)
        }
    }

    private fun check(draft: LotteryDraft) = LotteryRules.check(
        campsiteCount = draft.campsites.size,
        entryFirstDay = draft.entryFirstDay,
        entryLastDay = draft.entryLastDay,
        stayPeriods = draft.stayPeriods,
        winnerCount = draft.winnerCount.toIntOrNull(),
        waitlistCapacity = draft.waitlistCapacity.toIntOrNull(),
        today = today,
    )

    /** Builds the lottery to save. Only call once the draft has passed [check]. */
    private fun LotteryDraft.toLottery(ownerId: Id): Lottery {
        val (entryStart, entryEnd) = LotteryRules.entryWindow(entryFirstDay!!, entryLastDay!!, zone)
        val capacity = waitlistCapacity.toIntOrNull()
        return Lottery(
            ownerId = ownerId,
            campsites = campsites.toList(),
            entryStart = entryStart,
            entryEnd = entryEnd,
            stayPeriods = stayPeriods,
            winnerCount = winnerCount.toInt(),
            waitingList = WaitingList(capacity = capacity ?: Int.MAX_VALUE),
        )
    }

    private companion object {
        /** Keeps the number fields well inside an Int. */
        const val MAX_DIGITS = 5

        fun String.digits() = filter { it in '0'..'9' }.take(MAX_DIGITS)
    }
}
