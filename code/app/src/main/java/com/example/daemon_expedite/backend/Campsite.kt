package com.example.daemon_expedite.backend

import android.location.Location


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
    rating: Rating,
    address: String
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
    var rating: Rating = rating
        private set
    var address: String = address
        private set

    private val _tags = tags.toMutableList()
    val tags: List<String> get() = _tags

    private val _images = images.toMutableList()
    val images: List<Image> get() = _images

    private val _lotteryIds = mutableListOf<Id>()
    val lotteryIds: List<Id> get() = _lotteryIds

    suspend fun updateDetails(newName: String, newRate: Int, newMaxOccupancy: Int) {
        this.name = newName
        this.pricePerNight = newRate
        this.maxOccupancy = newMaxOccupancy

        Database.updateCampsite(this)
    }

    suspend fun addLottery(lottery: Lottery) {
        if (lottery.id !in _lotteryIds) {
            _lotteryIds.add(lottery.id)
            Database.addLottery(lottery)
        }
    }

    suspend fun addImage(image: Image) {
        if (image !in _images) {
            _images.add(image)
            Database.addImage(image)
        }
    }

    fun generateQRCode(){
       // generate QR Code library function
    }

    suspend fun updateRating(rating: Int) {
        this.rating.addRating(rating)
        Database.updateCampsite(this)
    }
}