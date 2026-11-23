package com.example.daemon_expedite.backend

import android.location.Location

<<<<<<< Updated upstream
/**
 * Represents a campsite listing that can be published by an owner and booked through a lottery.
 *
 * The class keeps track of the campsite's public details, pricing, occupancy limits,
 * and associated images. Its lotteries are found with [Database.fetchLotteriesForCampsite].
 *
 * @property id Unique identifier for the campsite.
 * @property ownerId Identifier of the owner who manages this campsite.
 * @property ratings Every review left for the campsite; see [averageRating].
 */
=======

>>>>>>> Stashed changes
class Campsite(
    val id: Id = Id(),
    name: String,
    description: String,
    tags: List<String> = emptyList(),
    location: Location,
    maxOccupancy: Int,
    policies: String = "",
    pricePerNight: Int,
    images: List<Image> = emptyList(),
    val ownerId: Id,
<<<<<<< Updated upstream
    ratings: List<Rating> = emptyList(),
    address: String
=======
    rating: Rating
>>>>>>> Stashed changes
) {
    var name: String = name
        private set
    var description: String = description
        private set
    var location: Location = location
        private set
    var maxOccupancy: Int = maxOccupancy
        private set
    var pricePerNight: Int = pricePerNight
        private set
    var policies: String = policies
        private set
<<<<<<< Updated upstream
    var address: String = address
=======
    var rating: Rating = rating
>>>>>>> Stashed changes
        private set

    private val _tags = tags.toMutableList()
    val tags: List<String> get() = _tags

    private val _images = images.toMutableList()
    val images: List<Image> get() = _images

<<<<<<< Updated upstream
    private val _ratings = ratings.toMutableList()
    val ratings: List<Rating> get() = _ratings

    /** Changes the details */
=======
    private val _lotteryIds = mutableListOf<Id>()
    val lotteryIds: List<Id> get() = _lotteryIds

>>>>>>> Stashed changes
    fun updateDetails(newName: String, newRate: Int, newMaxOccupancy: Int) {
        this.name = newName
        this.pricePerNight = newRate
        this.maxOccupancy = newMaxOccupancy
<<<<<<< Updated upstream
    }

    /**
     * Adds [image].
     */
    fun addImage(image: Image): Boolean {
        if (image in _images) return false
        return _images.add(image)
    }

    fun addRating(rating: Rating): Boolean {
        if (rating in _ratings) return false
        return _ratings.add(rating)
    }

    fun averageRating(): Double {
        if (_ratings.isEmpty()) return 0.0
        var sum = 0
        for (rating in _ratings){
            sum += rating.score
        }
        return sum.toDouble() / _ratings.size
    }

=======

        Database.updateCampsite(this)
    }

    fun addLottery(lottery: Lottery) {
        if (lottery.id !in _lotteryIds) {
            _lotteryIds.add(lottery.id)
            Database.addLottery(lottery)
        }
    }

    fun addImage(image: Image) {
        if (image !in _images) {
            _images.add(image)
            Database.addImage(image)
        }
    }

    fun generateQRCode(){
       // generate QR Code library function
    }

    fun updateRating(rating: Int) {
        this.rating.addRating(rating)
        Database.updateCampsite(this)
    }
>>>>>>> Stashed changes
}