package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.Car

/**
 * Data the Android app sends to create or update a car listing.
 * It has no id: the server decides the id.
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

/** Builds a car with the given [id] from this request. */
fun CreateCarRequest.toCar(id: Int): Car = Car(
    id = id,
    brand = brand.trim(),
    model = model.trim(),
    year = year,
    price = price,
    mileage = mileage,
    doors = doors
)