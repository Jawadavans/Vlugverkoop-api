package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable

/**
 * Data the Android app sends to create a new scooter.
 * The plate colour is not sent: the scooter derives it from [maxSpeedKmh].
 */
@Serializable
data class CreateScooterRequest(
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val maxSpeedKmh: Int
)
