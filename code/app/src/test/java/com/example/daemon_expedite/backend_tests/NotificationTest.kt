// AI use: parts of this file were written with help from Anthropic, Claude Code,
// "Send entrants a notification confirming their reservation", 2026-11-26.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend_tests

import com.example.daemon_expedite.backend.DatePeriod
import com.example.daemon_expedite.backend.NotificationKind
import com.example.daemon_expedite.backend.reservationCancelledText
import com.example.daemon_expedite.backend.reservationConfirmedText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Unit tests for reservation-confirmation notifications. No database calls, so nothing is mocked. */
class NotificationTest {

    private fun day(d: Int) = LocalDate.of(2026, 10, d)

    @Test
    fun confirmationTextNamesCampsiteNightsAndDates() {
        assertEquals(
            "Your stay at Pine Lake is booked: 2 nights, Oct 16, 2026 to check-out Oct 18, 2026.",
            reservationConfirmedText(" Pine Lake ", DatePeriod(day(16), day(18))),
        )
    }

    @Test
    fun confirmationTextSaysOneNightAndFallsBackWithoutName() {
        assertEquals(
            "Your stay at your campsite is booked: 1 night, Oct 16, 2026 to check-out Oct 17, 2026.",
            reservationConfirmedText("  ", DatePeriod(day(16), day(17))),
        )
        assertEquals(
            reservationConfirmedText(null, DatePeriod(day(16), day(17))),
            reservationConfirmedText("", DatePeriod(day(16), day(17))),
        )
    }

    @Test
    fun cancellationTextNamesCampsiteNightsAndDates() {
        assertEquals(
            "Your stay at Pine Lake for 2 nights, Oct 16, 2026 to check-out Oct 18, 2026, is cancelled.",
            reservationCancelledText(" Pine Lake ", DatePeriod(day(16), day(18))),
        )
        assertEquals(
            "Your stay at your campsite for 1 night, Oct 16, 2026 to check-out Oct 17, 2026, is cancelled.",
            reservationCancelledText(null, DatePeriod(day(16), day(17))),
        )
    }

    @Test
    fun storedKindsRoundTripAndOldDocumentsAreOwnerMessages() {
        NotificationKind.entries.forEach { assertEquals(it, NotificationKind.fromStored(it.stored)) }
        assertEquals(NotificationKind.OWNER_MESSAGE, NotificationKind.fromStored(null))
        assertEquals(NotificationKind.OWNER_MESSAGE, NotificationKind.fromStored("something_new"))
    }
}
