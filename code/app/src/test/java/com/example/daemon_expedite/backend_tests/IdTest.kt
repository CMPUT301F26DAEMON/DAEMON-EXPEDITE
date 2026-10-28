package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.Id
import org.junit.Test
import kotlin.uuid.Uuid

class IdTest {
    @Test
    fun testIdGeneration() {
        val id1 = "01a17b5a-7d88-75c9-98a0-831fa7ffc7ce" // random UUID
        val id2 = "01a17b5a-c3ce-7535-8cf5-6c84dfa82c06" // another random UUID

        val createdId1 = Id(Uuid.parse(id1))
        val createdId2 = Id(Uuid.parse(id2))

        assert(createdId1.value == Uuid.parse(id1)) // check if id is correct
        assert(createdId2.value == Uuid.parse(id2)) // check if id is correct
    }

    @Test
    fun testIdTime() {
        val id1 = "01a17b5a-7d88-75c9-98a0-831fa7ffc7ce" // random UUID
        val id2 = "f7caf1ee-f1c2-4682-857a-06ca84acab35"

        val createdId = Id(Uuid.parse(id1))

        assert(
            createdId.getTime().toString() == "2026-10-27T03:14:50.376Z",
        )
        try {
            Id(Uuid.parse(id2))
            assert(false)
        } catch (e: IllegalArgumentException) {
            assert(e.message == "Invalid UUID version: $id2")
        }
    }
}
