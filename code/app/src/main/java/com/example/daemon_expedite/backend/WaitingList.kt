package com.example.daemon_expedite.backend

/**
 * Represents a capped queue of users waiting for access to a lottery or booking opportunity.
 *
 * Users can be added or removed from the waiting list while preserving insertion order and
 * enforcing a maximum capacity.
 *
 * @property capacity Maximum number of entrants allowed in the list.
 */
class WaitingList(
    val capacity: Int = Int.MAX_VALUE,
    entrants: List<Id> = emptyList()
) {
    private val _entrants = entrants.toMutableList()
    val entrants: List<Id> get() = _entrants

    fun add(user: Id): Boolean {
        if (user in _entrants || isFull()) return false
        _entrants.add(user)
        return true
    }

    fun remove(user: Id): Boolean = _entrants.remove(user)

    fun isFull(): Boolean = _entrants.size >= capacity
}