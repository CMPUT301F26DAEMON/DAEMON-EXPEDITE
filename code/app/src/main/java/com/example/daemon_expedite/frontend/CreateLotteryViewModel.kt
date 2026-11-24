// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the My Lotteries, Create/Edit Lottery and lottery details screens from our design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daemon_expedite.backend.Campsite
import com.example.daemon_expedite.backend.CampsiteInvite
import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.InviteStatus
import com.example.daemon_expedite.backend.Lottery
import com.example.daemon_expedite.backend.LotteryEditRejectedException
import com.example.daemon_expedite.backend.LotteryPhase
import com.example.daemon_expedite.backend.LotteryProblems
import com.example.daemon_expedite.backend.LotteryRules
import com.example.daemon_expedite.backend.Owner
import com.example.daemon_expedite.backend.WaitingList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/** A campsite the owner can put in a lottery. */
data class CampsiteOption(val id: Id, val name: String)

/**
 * Another owner who can be invited to a lottery, with the campsites of theirs that can be
 * invited (not in another current lottery, plus any already invited to the lottery being edited).
 */
data class CollaboratorOption(val id: Id, val name: String, val campsites: List<CampsiteOption>)

/**
 * Everything entered on the Create / Edit lottery form so far.
 *
 * Number fields keep the digits typed, so an empty field can be told apart from 0.
 *
 * @property campsites Picked campsites that are in the lottery: the owner's own (and, when
 *   editing, ones collaborators already accepted). Only these count toward [maxWinners].
 * @property collaborators Owners picked as collaborators.
 * @property invitedCampsites Collaborators' campsites picked; each gets an invite to accept.
 * @property entryFirstDay First day entrants can join.
 * @property entryLastDay Last day entrants can join; winners are drawn at the end of it.
 * @property stayPeriods Nights winners can book, sorted by first night.
 * @property winnerCount Digits typed in "Number of winners".
 * @property waitlistCapacity Digits typed in "Waitlist capacity"; empty means no limit.
 */
data class LotteryDraft(
    val campsites: Set<Id> = emptySet(),
    val collaborators: Set<Id> = emptySet(),
    val invitedCampsites: Set<Id> = emptySet(),
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
 * What can't be changed while editing a lottery whose entries are open (see
 * [LotteryRules.checkEdit]). Everything is unlocked for a new lottery or one not open yet.
 *
 * @property campsites Campsites that can't be removed.
 * @property openDateLocked True if the open date can't change.
 * @property earliestClose The close date can't move before this, or null for no limit.
 * @property stayPeriods Stay periods that can't be removed.
 * @property minWinners The winner count can't go below this, or null for no limit.
 * @property minCapacity The waitlist limit can't go below this, or null for no limit.
 * @property collaborators Owners already invited. Invites can't be taken back, so they stay picked.
 * @property invitedCampsites Campsites already invited, whatever the answer. They can't change.
 */
data class LotteryEditLocks(
    val campsites: Set<Id> = emptySet(),
    val collaborators: Set<Id> = emptySet(),
    val invitedCampsites: Set<Id> = emptySet(),
    val openDateLocked: Boolean = false,
    val earliestClose: LocalDate? = null,
    val stayPeriods: Set<DatePeriod> = emptySet(),
    val minWinners: Int? = null,
    val minCapacity: Int? = null,
) {
    val anyLocked: Boolean get() = openDateLocked || campsites.isNotEmpty()
}

/**
 * Holds the Create lottery form and saves it. Also edits an existing lottery when the route
 * has a [Routes.LOTTERY_ID_ARG]: call [startEditing] and [submit] updates it instead.
 *
 * Call [load] with the owner and their campsites. Campsites already in another lottery that
 * isn't finished or cancelled are left out of the picker: a campsite can only be in one at a time.
 */
class CreateLotteryViewModel(savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** The lottery being edited, from the route, or null when creating a new one. */
    private val editingId: Id? = savedState.get<String>(Routes.LOTTERY_ID_ARG).toIdOrNull()

    val isEditing: Boolean get() = editingId != null

    private val _options = MutableStateFlow<UiState<List<CampsiteOption>>>(UiState.Loading)
    val options: StateFlow<UiState<List<CampsiteOption>>> = _options.asStateFlow()

    private val _collaborators = MutableStateFlow<UiState<List<CollaboratorOption>>>(UiState.Loading)
    val collaborators: StateFlow<UiState<List<CollaboratorOption>>> = _collaborators.asStateFlow()

    /** Collaborators' campsites as loaded, so new invites can be sent when an edit is saved. */
    private var invitableCampsites: Map<Id, Campsite> = emptyMap()

    private val _draft = MutableStateFlow(LotteryDraft())
    val draft: StateFlow<LotteryDraft> = _draft.asStateFlow()

    private val _problems = MutableStateFlow(LotteryProblems())
    val problems: StateFlow<LotteryProblems> = _problems.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /** Content once the lottery being edited is in the form. Only used when editing. */
    private val _editLoad = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val editLoad: StateFlow<UiState<Unit>> = _editLoad.asStateFlow()

    private val _locks = MutableStateFlow(LotteryEditLocks())
    val locks: StateFlow<LotteryEditLocks> = _locks.asStateFlow()

    /** The saved lottery being edited and its phase when the form opened. */
    private var original: Lottery? = null
    private var originalPhase: LotteryPhase? = null

    /** Problems are only shown after the first submit, then kept up to date as they are fixed. */
    private var showProblems = false

    private var loadedFor: Owner? = null
    private var lastCampsites: List<CampsiteSummary> = emptyList()

    /** The owner's today. Entries can't open before it. */
    val today: LocalDate get() = LocalDate.now(zone)

    /**
     * Works out which of [campsites] (the owner's, as shown on Owner Home) are free to use: those
     * not in another lottery that isn't past. Only loads once per owner.
     */
    fun load(owner: Owner, campsites: List<CampsiteSummary>) {
        if (loadedFor === owner) return
        loadedFor = owner
        lastCampsites = campsites
        viewModelScope.launch {
            _options.value = UiState.Loading
            _options.value = loadAsUiState("Couldn't load your campsites") {
                val busy = Database.fetchOwnerLotteries(owner.id)
                    .filterNot { it.phase().isPast || it.id == editingId }
                    .flatMap { it.campsites }
                    .toSet()
                campsites.filter { it.id !in busy }.map { CampsiteOption(it.id, it.name) }
            }
        }
        loadCollaborators(owner)
    }

    /**
     * Loads the other owners with at least one campsite that can be invited: one not in another
     * current lottery. When editing, campsites already invited to this lottery are included
     * whatever their state, so they show as picked.
     */
    private fun loadCollaborators(owner: Owner) {
        viewModelScope.launch {
            _collaborators.value = UiState.Loading
            _collaborators.value = loadAsUiState("Couldn't load other owners") {
                coroutineScope {
                    val invites = editingId?.let { Database.fetchLottery(it).invites }.orEmpty()
                    val invited = invites.map { it.campsite }.toSet()
                    val busy = Database.fetchBusyCampsiteIds(exceptLottery = editingId)
                    val owners = Database.fetchAllOwnerCampsites() - owner.id

                    val loaded = owners.map { (ownerId, campsiteIds) ->
                        async {
                            val usable = campsiteIds.filter { it !in busy || it in invited }
                            if (usable.isEmpty()) return@async null
                            val campsites = usable.map { id ->
                                async { quietly("Couldn't load campsite $id") { Database.fetchCampsite(id) } }
                            }.awaitAll().filterNotNull()
                            if (campsites.isEmpty()) return@async null
                            val name = quietly("Couldn't load owner $ownerId") { Database.fetchUser(ownerId)["name"] }
                            name.orEmpty().ifBlank { "Unnamed owner" } to campsites.sortedBy { it.name }
                        }
                    }.awaitAll()

                    invitableCampsites = loaded.filterNotNull().flatMap { it.second }.associateBy { it.id }
                    owners.keys.zip(loaded).mapNotNull { (ownerId, entry) ->
                        entry?.let { (name, campsites) ->
                            CollaboratorOption(ownerId, name, campsites.map { CampsiteOption(it.id, it.name) })
                        }
                    }.sortedBy { it.name.lowercase() }
                }
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

    /** Loads the other owners again after an error. */
    fun retryCollaborators() {
        loadedFor?.let(::loadCollaborators)
    }

    /**
     * Fills the form with the lottery being edited, and works out what is locked. Does nothing
     * when creating. Fails (see [editLoad]) once entries have closed.
     */
    fun startEditing() {
        val id = editingId ?: return
        viewModelScope.launch {
            _editLoad.value = UiState.Loading
            var tooLate = false
            val loaded = loadAsUiState("Couldn't load this lottery") {
                val lottery = Database.fetchLottery(id)
                val phase = lottery.phase()
                if (phase != LotteryPhase.UPCOMING && phase != LotteryPhase.OPEN) {
                    tooLate = true
                    return@loadAsUiState
                }
                original = lottery
                originalPhase = phase
                showProblems = false
                _problems.value = LotteryProblems()
                _draft.value = LotteryDraft(
                    campsites = lottery.campsites.toSet(),
                    collaborators = lottery.invites.map { it.campsiteOwner }.toSet(),
                    // Declined invites stay unticked (and locked: they can't be sent again).
                    invitedCampsites = lottery.invites.filter { it.status != InviteStatus.DECLINED }.map { it.campsite }.toSet(),
                    entryFirstDay = lottery.entryFirstDay(zone),
                    entryLastDay = lottery.entryLastDay(zone),
                    stayPeriods = lottery.stayPeriods,
                    winnerCount = lottery.winnerCount.toString(),
                    waitlistCapacity = lottery.waitlistCapacity?.toString().orEmpty(),
                )
                val joined = lottery.waitingList.users.size
                val invitedOwners = lottery.invites.map { it.campsiteOwner }.toSet()
                val invitedCampsites = lottery.invites.map { it.campsite }.toSet()
                _locks.value = if (phase == LotteryPhase.OPEN) {
                    LotteryEditLocks(
                        campsites = lottery.campsites.toSet(),
                        collaborators = invitedOwners,
                        invitedCampsites = invitedCampsites,
                        openDateLocked = true,
                        earliestClose = lottery.entryLastDay(zone),
                        stayPeriods = lottery.stayPeriods.toSet(),
                        minWinners = lottery.winnerCount,
                        minCapacity = maxOf(lottery.waitlistCapacity ?: 0, joined).takeIf { it > 0 },
                    )
                } else {
                    LotteryEditLocks(
                        collaborators = invitedOwners,
                        invitedCampsites = invitedCampsites,
                        minCapacity = joined.takeIf { it > 0 },
                    )
                }
            }
            _editLoad.value = if (tooLate) UiState.Error("Entries have closed, so this lottery can't be edited any more") else loaded
        }
    }

    // region Edits

    fun toggleCampsite(id: Id) {
        if (id in _locks.value.campsites) return
        update { it.copy(campsites = if (id in it.campsites) it.campsites - id else it.campsites + id) }
    }

    /** Picks or unpicks another owner. Unpicking also drops the campsites of theirs that were picked. */
    fun toggleCollaborator(id: Id) {
        if (id in _locks.value.collaborators) return
        val theirs = (_collaborators.value as? UiState.Content)?.data?.firstOrNull { it.id == id }?.campsites.orEmpty().map { it.id }
        update {
            if (id in it.collaborators) {
                it.copy(collaborators = it.collaborators - id, invitedCampsites = it.invitedCampsites - theirs.toSet())
            } else {
                it.copy(collaborators = it.collaborators + id)
            }
        }
    }

    /** Picks or unpicks a collaborator's campsite to invite. */
    fun toggleInvitedCampsite(id: Id) {
        if (id in _locks.value.invitedCampsites) return
        update { it.copy(invitedCampsites = if (id in it.invitedCampsites) it.invitedCampsites - id else it.invitedCampsites + id) }
    }

    fun setEntryFirstDay(day: LocalDate) {
        if (_locks.value.openDateLocked) return
        update {
            // Keep the close date from ending up before the open date.
            val last = it.entryLastDay?.takeUnless { last -> last.isBefore(day) }
            it.copy(entryFirstDay = day, entryLastDay = last)
        }
    }

    fun setEntryLastDay(day: LocalDate) = update { it.copy(entryLastDay = day) }

    /** Adds the nights from [firstNight] to the day before [checkOut]. */
    fun addStayPeriod(firstNight: LocalDate, checkOut: LocalDate) = update {
        it.copy(stayPeriods = (it.stayPeriods + DatePeriod(firstNight, checkOut)).sortedBy { p -> p.start })
    }

    fun removeStayPeriod(index: Int) {
        val period = _draft.value.stayPeriods.getOrNull(index) ?: return
        if (period in _locks.value.stayPeriods) return
        update { it.copy(stayPeriods = it.stayPeriods - period) }
    }

    fun onWinnerCountChange(text: String) = update { it.copy(winnerCount = text.digits()) }

    fun onWaitlistCapacityChange(text: String) = update { it.copy(waitlistCapacity = text.digits()) }

    private fun update(change: (LotteryDraft) -> LotteryDraft) {
        _draft.update(change)
        if (showProblems) _problems.value = check(_draft.value)
    }

    // endregion

    /**
     * Checks the form and, if it is fine, saves it. [onResult] says how it went; the form is
     * kept on failure so the owner can try again.
     *
     * - Creating: saves with [Database.createLottery], which also records it on the owner's
     *   document, then records it on the in-memory owner with [Owner.addOwnerLottery].
     * - Editing: saves with [Database.updateLotterySettings], which checks the edit again
     *   against the latest copy of the lottery. If that fails (for example more entrants joined
     *   than the new limit allows) the problems are shown and the result is INVALID.
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
            val editing = original
            val result = if (editing == null) {
                val ok = runCatchingAll("Creating lottery failed") {
                    // Invites go in with the lottery itself, in the same write.
                    val lottery = draft.toLottery(owner.id, invites = draft.newInvites())
                    Database.createLottery(lottery)
                    owner.addOwnerLottery(lottery)
                }
                if (ok) SubmitResult.SAVED else SubmitResult.FAILED
            } else {
                val saved = saveEdit(editing, draft)
                if (saved == SubmitResult.SAVED) sendNewInvites(editing, owner, draft) else saved
            }
            _isSaving.value = false
            onResult(result)
        }
    }

    /** Invites for the collaborators' campsites picked that weren't invited before. */
    private fun LotteryDraft.newInvites(): List<CampsiteInvite> =
        (invitedCampsites - _locks.value.invitedCampsites).mapNotNull { id ->
            invitableCampsites[id]?.let { CampsiteInvite(it.id, it.ownerId) }
        }

    /**
     * After an edit is saved, invites the collaborators' campsites newly picked. Settings are
     * already saved by then, so a failure here is [SubmitResult.INVITES_FAILED], not FAILED.
     */
    private suspend fun sendNewInvites(editing: Lottery, owner: Owner, draft: LotteryDraft): SubmitResult {
        var allSent = true
        draft.newInvites().forEach { invite ->
            val campsite = invitableCampsites[invite.campsite] ?: return@forEach
            val sent = runCatchingAll("Inviting campsite ${campsite.id} failed") {
                Database.sendCampsiteInvite(editing.id, owner.id, campsite)
            }
            if (!sent) allSent = false
        }
        return if (allSent) SubmitResult.SAVED else SubmitResult.INVITES_FAILED
    }

    private suspend fun saveEdit(editing: Lottery, draft: LotteryDraft): SubmitResult =
        try {
            Database.updateLotterySettings(draft.toLottery(editing.ownerId, base = editing))
            SubmitResult.SAVED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Firestore may wrap exceptions thrown inside a transaction.
            val rejected = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<LotteryEditRejectedException>()
                .firstOrNull()
            if (rejected != null) {
                _problems.value = rejected.problems
                SubmitResult.INVALID
            } else {
                Log.e(TAG, "Saving lottery ${editing.id} failed", e)
                SubmitResult.FAILED
            }
        }

    private fun check(draft: LotteryDraft): LotteryProblems {
        val editing = original
        val phase = originalPhase
        return if (editing != null && phase != null) {
            LotteryRules.checkEdit(
                original = editing,
                phase = phase,
                campsites = draft.campsites,
                entryFirstDay = draft.entryFirstDay,
                entryLastDay = draft.entryLastDay,
                stayPeriods = draft.stayPeriods,
                winnerCount = draft.winnerCount.toIntOrNull(),
                waitlistCapacity = draft.waitlistCapacity.toIntOrNull(),
                today = today,
                zone = zone,
            )
        } else {
            LotteryRules.check(
                campsiteCount = draft.campsites.size,
                entryFirstDay = draft.entryFirstDay,
                entryLastDay = draft.entryLastDay,
                stayPeriods = draft.stayPeriods,
                winnerCount = draft.winnerCount.toIntOrNull(),
                waitlistCapacity = draft.waitlistCapacity.toIntOrNull(),
                today = today,
            )
        }
    }

    /**
     * Builds the lottery to save. Only call once the draft has passed [check]. When editing,
     * [base] supplies everything the form doesn't change (id, waitlist, winners, ...).
     */
    private fun LotteryDraft.toLottery(
        ownerId: Id,
        base: Lottery? = null,
        invites: List<CampsiteInvite> = base?.invites.orEmpty(),
    ): Lottery {
        val (entryStart, entryEnd) = LotteryRules.entryWindow(entryFirstDay!!, entryLastDay!!, zone)
        val capacity = waitlistCapacity.toIntOrNull() ?: Int.MAX_VALUE
        return Lottery(
            id = base?.id ?: Id(),
            ownerId = ownerId,
            campsites = campsites.toList(),
            entryStart = entryStart,
            entryEnd = entryEnd,
            stayPeriods = stayPeriods,
            winnerCount = winnerCount.toInt(),
            waitingList = WaitingList(capacity = capacity, users = base?.waitingList?.users.orEmpty()),
            winners = base?.winners.orEmpty(),
            bookings = base?.bookings.orEmpty(),
            drawn = base?.drawn ?: false,
            cancelled = base?.cancelled ?: false,
            invites = invites,
        )
    }

    private companion object {
        const val TAG = "CreateLotteryViewModel"

        /** Keeps the number fields well inside an Int. */
        const val MAX_DIGITS = 5

        fun String.digits() = filter { it in '0'..'9' }.take(MAX_DIGITS)
    }
}
