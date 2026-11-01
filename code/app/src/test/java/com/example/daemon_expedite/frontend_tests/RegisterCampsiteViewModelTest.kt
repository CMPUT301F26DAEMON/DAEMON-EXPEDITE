package com.example.daemon_expedite.frontend_tests

import com.example.daemon_expedite.frontend.RegisterCampsiteViewModel
import com.example.daemon_expedite.frontend.combinePolicies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterCampsiteViewModelTest {

    @Test
    fun addAmenity_acceptsShortTagsAndTrims() {
        val form = RegisterCampsiteViewModel()
        assertTrue(form.addAmenity("  Water "))
        assertEquals(listOf("Water"), form.draft.value.amenities)
    }

    @Test
    fun addAmenity_rejectsBlankTooLongAndDuplicates() {
        val form = RegisterCampsiteViewModel()
        assertFalse(form.addAmenity("   "))
        assertFalse(form.addAmenity("a".repeat(RegisterCampsiteViewModel.MAX_AMENITY_LENGTH + 1)))
        assertTrue(form.addAmenity("a".repeat(RegisterCampsiteViewModel.MAX_AMENITY_LENGTH)))
        assertTrue(form.addAmenity("Firepit"))
        assertFalse(form.addAmenity("FIREPIT"))
        assertEquals(2, form.draft.value.amenities.size)
    }

    @Test
    fun setLocation_fillsAddressOnlyIfOwnerHasNotTypedOne() {
        val form = RegisterCampsiteViewModel()
        form.setLocation(1.0, 2.0, "Map address")
        assertEquals("Map address", form.draft.value.address)

        form.setLocation(3.0, 4.0, "Newer map address")
        assertEquals("Newer map address", form.draft.value.address)

        form.onAddressChange("Typed address")
        form.setLocation(5.0, 6.0, "Ignored map address")
        assertEquals("Typed address", form.draft.value.address)
        assertEquals(5.0, form.draft.value.latitude!!, 0.0)
    }

    @Test
    fun changeOccupancy_staysWithinLimits() {
        val form = RegisterCampsiteViewModel()
        form.changeOccupancy(-1)
        assertEquals(RegisterCampsiteViewModel.MIN_OCCUPANCY, form.draft.value.maxOccupancy)
        form.changeOccupancy(1)
        assertEquals(2, form.draft.value.maxOccupancy)
        repeat(100) { form.changeOccupancy(1) }
        assertEquals(RegisterCampsiteViewModel.MAX_OCCUPANCY, form.draft.value.maxOccupancy)
    }

    @Test
    fun onPriceChange_keepsOnlyDigits() {
        val form = RegisterCampsiteViewModel()
        form.onPriceChange("$4a5.-50 ")
        assertEquals("4550", form.draft.value.pricePerNight)
        form.onPriceChange("1234567")
        assertEquals("12345", form.draft.value.pricePerNight)
    }

    @Test
    fun combinePolicies_headsEachSectionAndSkipsEmptyOnes() {
        assertEquals(
            "Campsite rules:\nNo pets\n\nCancellation policy:\nFull refund",
            combinePolicies(" No pets ", "Full refund"),
        )
        assertEquals("Cancellation policy:\nFull refund", combinePolicies("", "Full refund"))
        assertEquals("", combinePolicies(" ", ""))
    }
}
