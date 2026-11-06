package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.backend.WaitingList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [WaitingList]. It has no database calls, so nothing is mocked. */
class WaitingListTest {

    @Test
    fun add_toEmptyListSucceeds() {
        val list = WaitingList(capacity = 2)
        val user = Id()
        assertTrue(list.add(user))
        assertEquals(listOf(user), list.users)
    }

    @Test
    fun add_keepsJoinOrder() {
        val list = WaitingList()
        val first = Id()
        val second = Id()
        list.add(first)
        list.add(second)
        assertEquals(listOf(first, second), list.users)
    }

    @Test
    fun add_sameUserTwiceIsRefused() {
        val user = Id()
        val list = WaitingList(users = listOf(user))
        assertFalse(list.add(user))
        assertEquals(1, list.users.size)
    }

    @Test
    fun add_lastFreeSpotSucceedsThenListIsFull() {
        val list = WaitingList(capacity = 2, users = listOf(Id()))
        assertFalse(list.isFull())
        assertTrue(list.add(Id())) // fills the last spot (boundary)
        assertTrue(list.isFull())
        assertFalse(list.add(Id())) // one over capacity
        assertEquals(2, list.users.size)
    }

    @Test
    fun zeroCapacity_isAlwaysFull() {
        val list = WaitingList(capacity = 0)
        assertTrue(list.isFull())
        assertFalse(list.add(Id()))
    }

    @Test
    fun defaultCapacity_isEffectivelyUnlimited() {
        val list = WaitingList()
        repeat(1000) { assertTrue(list.add(Id())) }
        assertFalse(list.isFull())
    }

    @Test
    fun remove_presentUserSucceedsAndFreesASpot() {
        val user = Id()
        val list = WaitingList(capacity = 1, users = listOf(user))
        assertTrue(list.remove(user))
        assertTrue(list.users.isEmpty())
        assertTrue(list.add(Id())) // the freed spot can be taken
    }

    @Test
    fun remove_absentUserReturnsFalse() {
        val list = WaitingList(users = listOf(Id()))
        assertFalse(list.remove(Id()))
        assertEquals(1, list.users.size)
    }

    @Test
    fun overCapacityFromDatabase_isFull() {
        // e.g. the owner lowered the limit; the saved list can be bigger than the capacity.
        val list = WaitingList(capacity = 1, users = listOf(Id(), Id()))
        assertTrue(list.isFull())
        assertFalse(list.add(Id()))
    }
}
