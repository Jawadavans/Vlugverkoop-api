package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable

/**
 * Data the Android app sends to create a new car.
 */
@Serializable
data class CreateCarRequest(
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val doors: Int
)