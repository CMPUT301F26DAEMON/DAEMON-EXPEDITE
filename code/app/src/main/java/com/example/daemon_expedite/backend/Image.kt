package com.example.daemon_expedite.backend

<<<<<<< Updated upstream
/**
 * Represents an image uploaded for a campsite or user profile.
 *
 * @property id Unique identifier for the image.
 * @property uploader Identifier of the user who uploaded the image.
 * @property path Storage path or URL where the image is located.
 */
class Image (
    val id: Id = Id(),
    val uploader: Id,
    val path: String
)
=======
class Image (
    val id: Id = Id(),
    uploader: Id,
    path: String
){
    var uploader: Id = uploader
        private set

    var path: String = path
        private set
}
>>>>>>> Stashed changes
