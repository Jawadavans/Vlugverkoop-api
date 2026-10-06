package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.Scooter

/**
 * Scooter data as it is sent to the Android app in JSON.
 */
@Serializable
data class ScooterResponse(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val maxSpeedKmh: Int,
    val plateColor: PlateColor,
    val listingFee: Double
)

/** Converts a Scooter into a ScooterResponse. */
fun Scooter.toResponse(): ScooterResponse = ScooterResponse(
    id = id,
    brand = brand,
    model = model,
    year = year,
    price = price,
    mileage = mileage,
    maxSpeedKmh = maxSpeedKmh,
    plateColor = plateColor,
    listingFee = calculateListingFee()
)
