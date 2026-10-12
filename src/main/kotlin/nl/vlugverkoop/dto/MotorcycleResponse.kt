package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.Motorcycle
import nl.vlugverkoop.model.MotorcycleStyle

/**
 * Motorcycle data as it is sent to the Android app in JSON.
 */
@Serializable
data class MotorcycleResponse(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val engineCc: Int,
    val powerKw: Int,
    val style: MotorcycleStyle,
    val licenseCategory: LicenseCategory,
    val listingFee: Double
)

/** Converts a Motorcycle into a MotorcycleResponse. */
fun Motorcycle.toResponse(): MotorcycleResponse = MotorcycleResponse(
    id = id,
    brand = brand,
    model = model,
    year = year,
    price = price,
    mileage = mileage,
    engineCc = engineCc,
    powerKw = powerKw,
    style = style,
    licenseCategory = licenseCategory,
    listingFee = calculateListingFee()
)
