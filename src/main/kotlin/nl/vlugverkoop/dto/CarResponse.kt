package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.Car

/**
 * Car data as it is sent to the Android app in JSON.
 */
@Serializable
data class CarResponse(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val doors: Int,
    val listingFee: Double
)

/** Converts a Car into a CarResponse. */
fun Car.toResponse(): CarResponse = CarResponse(
    id = id,
    brand = brand,
    model = model,
    year = year,
    price = price,
    mileage = mileage,
    doors = doors,
    listingFee = calculateListingFee()
)