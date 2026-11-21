package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.A2Restriction
import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.VehicleType
import nl.vlugverkoop.rdw.RdwVehicle
import kotlin.math.ceil

/**
 * The result of a licence plate check, as it is sent to the Android app in JSON.
 *
 * The top fields are for showing the vehicle. Fields RDW does not know are null.
 *
 * Exactly one of [carForm], [motorcycleForm] and [scooterForm] is filled, the one that matches [vehicleType].
 * The app opens that "Place listing" form and fills it in with these values.
 *
 * @property licenseCategory only for a motorcycle: the lowest licence needed.
 * @property a2Restriction only for a motorcycle: whether it can be restricted for A2.
 */
@Serializable
data class PlateCheckResponse(
    val plate: String,
    val vehicleType: VehicleType,
    val brand: String,
    val model: String,
    val year: Int?,
    val engineCc: Int?,
    val doors: Int?,
    val powerKw: Double?,
    val maxSpeedKmh: Int?,
    val licenseCategory: LicenseCategory?,
    val a2Restriction: A2Restriction?,
    val carForm: CarForm? = null,
    val motorcycleForm: MotorcycleForm? = null,
    val scooterForm: ScooterForm? = null
)

/**
 * The known part of the car form; the fields match [CreateCarRequest].
 * Price and mileage are not known by RDW, so the user fills those in.
 */
@Serializable
data class CarForm(
    val brand: String,
    val model: String,
    val year: Int?,
    val doors: Int?
)

/**
 * The known part of the motorcycle form; the fields match [CreateMotorcycleRequest].
 * Price, mileage and riding style are filled in by the user.
 *
 * @property powerKw whole kilowatts, as the motorcycle form expects; rounded up so the licence stays correct
 *                   (11.3 kW becomes 12, which still needs A2, just like 11.3).
 */
@Serializable
data class MotorcycleForm(
    val brand: String,
    val model: String,
    val year: Int?,
    val engineCc: Int?,
    val powerKw: Int?,
    val licenseCategory: LicenseCategory?
)

/**
 * The known part of the scooter form; the fields match [CreateScooterRequest].
 * Price and mileage are filled in by the user.
 *
 * @property maxSpeedKmh only filled in when it is a legal speed class (25 or 45 km/h).
 */
@Serializable
data class ScooterForm(
    val brand: String,
    val model: String,
    val year: Int?,
    val maxSpeedKmh: Int?
)

/**
 * Converts RDW data into the answer for the app.
 *
 * @return the answer, or null when the vehicle is not a car, motorcycle or scooter.
 */
fun RdwVehicle.toPlateCheckResponse(): PlateCheckResponse? {
    val type = VehicleType.fromRdwKind(rdwKind) ?: return null
    val brand = brand.orEmpty()
    val model = model.orEmpty()

    // The answer without a form; the matching form is added below.
    val answer = PlateCheckResponse(
        plate = plate,
        vehicleType = type,
        brand = brand,
        model = model,
        year = year,
        engineCc = engineCc,
        doors = doors,
        powerKw = powerKw,
        maxSpeedKmh = maxSpeedKmh,
        licenseCategory = null,
        a2Restriction = null
    )

    return when (type) {
        VehicleType.CAR -> answer.copy(
            carForm = CarForm(brand, model, year, doors)
        )

        VehicleType.MOTORCYCLE -> {
            // The licence rules need both cc and power; without them there is no advice.
            var licenseCategory: LicenseCategory? = null
            var a2Restriction: A2Restriction? = null
            var wholePowerKw: Int? = null
            if (powerKw != null) {
                a2Restriction = A2Restriction.forPower(powerKw)
                wholePowerKw = ceil(powerKw).toInt()
                if (engineCc != null) {
                    licenseCategory = LicenseCategory.minimumFor(engineCc, powerKw)
                }
            }

            answer.copy(
                licenseCategory = licenseCategory,
                a2Restriction = a2Restriction,
                motorcycleForm = MotorcycleForm(brand, model, year, engineCc, wholePowerKw, licenseCategory)
            )
        }

        VehicleType.SCOOTER -> {
            // Only a legal speed class (25 or 45 km/h) can go into the form.
            val legalSpeed = if (maxSpeedKmh != null && PlateColor.forSpeed(maxSpeedKmh) != null) maxSpeedKmh else null

            answer.copy(
                scooterForm = ScooterForm(brand, model, year, legalSpeed)
            )
        }
    }
}
