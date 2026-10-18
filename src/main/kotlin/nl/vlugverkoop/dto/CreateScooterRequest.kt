package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.Scooter

/**
 * Data the Android app sends to create or update a scooter listing.
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

/**
 * Builds a scooter with the given [id] from this request.
 *
 * @throws IllegalArgumentException if [CreateScooterRequest.maxSpeedKmh] is not 25 or 45.
 */
fun CreateScooterRequest.toScooter(id: Int): Scooter = Scooter(
    id = id,
    brand = brand.trim(),
    model = model.trim(),
    year = year,
    price = price,
    mileage = mileage,
    maxSpeedKmh = maxSpeedKmh
)
