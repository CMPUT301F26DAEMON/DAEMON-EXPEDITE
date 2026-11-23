package com.example.daemon_expedite.backend

/**
 * Represents a capped queue of users waiting for access to a lottery or booking opportunity.
 *
 * Users can be added or removed from the waiting list while preserving insertion order and
 * enforcing a maximum capacity.
 *
 * @property capacity Maximum number of users allowed in the list.
 */
class WaitingList(
    val capacity: Int = Int.MAX_VALUE,
    users: List<Id> = emptyList()
) {
    private val _users = users.toMutableList()
    val users: List<Id> get() = _users

    fun add(user: Id): Boolean {
        if (user in _users || isFull()) return false
        _users.add(user)
        return true
    }

    fun remove(user: Id): Boolean = _users.remove(user)

    fun isFull(): Boolean = _users.size >= capacity
}