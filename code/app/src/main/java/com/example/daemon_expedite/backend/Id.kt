package com.example.daemon_expedite.backend

import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
*  Represent a basic id object used to identify objects in the database.
*
*  @param value: Uuid. This is the value of the id. If no id is provided, it generates a random v7 Uuid.
*  @throws IllegalArgumentException: If the id is not a valid v7 Uuid or Uuid is not 36 characters long.
* */

class Id
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        val value: Uuid = Uuid.generateV7(),
    ) {
        init {
            val uuidString = value.toString()
            if (uuidString.length != 36) {
                throw IllegalArgumentException("Invalid UUID format: $uuidString")
            }
            if ((uuidString.getOrNull(14) ?: '0') != '7') {
                throw IllegalArgumentException("Invalid UUID version: $uuidString")
            }
        }

        /**
         * Function to get the time of the id creation used in different functions that require timestamp of the object creation.
         *
         * @return Instant: The time of the id creation.
         * */
        fun getTime(): Instant =
            Instant.ofEpochMilli((value.toString().substring(0..7) + value.toString().substring(9..12)).toLong(radix = 16))

        /**
         * Two ids are equal when their UUIDs are equal, even if they are different objects.
         *
         * Needed because ids read from the database are always new objects. Without this,
         * `campsite.id in owner.ownerCampsites` and `list.remove(id)` compare object identity
         * and never match an id that was loaded separately.
         */
        override fun equals(other: Any?): Boolean = other is Id && other.value == value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = value.toString()
    }
