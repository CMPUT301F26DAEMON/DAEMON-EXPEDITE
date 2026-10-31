package com.example.daemon_expedite.backend

import org.junit.Test
import org.junit.Assert.*

class ImageTest {

    @Test
    fun testUploaderIsStoredCorrectly() {
        val uploader = Id()

        val image = Image(
            uploader = uploader,
            path = "images/test.png"
        )

        assertEquals(uploader.value, image.uploader.value)
    }

    @Test
    fun testPathIsStoredCorrectly() {
        val image = Image(
            uploader = Id(),
            path = "images/test.png"
        )

        assertEquals("images/test.png", image.path)
    }


}