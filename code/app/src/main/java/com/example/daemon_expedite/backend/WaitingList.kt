package com.example.daemon_expedite.backend

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