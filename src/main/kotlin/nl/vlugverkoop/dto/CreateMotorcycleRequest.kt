package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.Motorcycle
import nl.vlugverkoop.model.MotorcycleStyle

/**
 * Data the Android app sends to create a new motorcycle.
 * It has no id: the server decides the id.
 */
@Serializable
data class CreateMotorcycleRequest(
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val engineCc: Int,
    val powerKw: Int,
    val style: MotorcycleStyle,
    val licenseCategory: LicenseCategory
)

/** Converts the request into a Motorcycle with the given [id]. */
fun CreateMotorcycleRequest.toMotorcycle(id: Int): Motorcycle = Motorcycle(
    id = id,
    brand = brand,
    model = model,
    year = year,
    price = price,
    mileage = mileage,
    engineCc = engineCc,
    powerKw = powerKw,
    style = style,
    licenseCategory = licenseCategory
)
