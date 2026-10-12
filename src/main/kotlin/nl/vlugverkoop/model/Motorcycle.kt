package nl.vlugverkoop.model

import java.time.Year

/**
 * A motorcycle listed for sale on VlugVerkoop.
 *
 * @property engineCc engine displacement in cubic centimetres.
 * @property powerKw engine power in kilowatts.
 * @property style the riding style, used for filtering.
 * @property licenseCategory the lowest licence needed to ride this motorcycle.
 */
class Motorcycle(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val engineCc: Int,
    val powerKw: Int,
    val style: MotorcycleStyle,
    val licenseCategory: LicenseCategory
) : Vehicle(id, brand, model, year, price, mileage) {

    override fun calculateListingFee(): Double = 4.95

    /**
     * Checks whether this motorcycle may be listed.
     * Returns a list of error messages; an empty list means the motorcycle is valid.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (brand.isBlank()) {
            errors.add("brand is required")
        }
        if (model.isBlank()) {
            errors.add("model is required")
        }
        if (price <= 0) {
            errors.add("price must be greater than 0")
        }
        if (mileage < 0) {
            errors.add("mileage cannot be negative")
        }
        if (year !in MIN_YEAR..Year.now().value) {
            errors.add("year must be between $MIN_YEAR and ${Year.now().value}")
        }
        if (engineCc !in MIN_ENGINE_CC..MAX_ENGINE_CC) {
            errors.add("engineCc must be between $MIN_ENGINE_CC and $MAX_ENGINE_CC")
        }
        if (powerKw !in MIN_POWER_KW..MAX_POWER_KW) {
            errors.add("powerKw must be between $MIN_POWER_KW and $MAX_POWER_KW")
        }
        if (licenseCategory < LicenseCategory.minimumFor(engineCc, powerKw)) {
            errors.add("licenseCategory $licenseCategory is not allowed for $engineCc cc and $powerKw kW")
        }

        return errors
    }

    companion object {
        private const val MIN_ENGINE_CC = 50
        private const val MAX_ENGINE_CC = 2500
        private const val MIN_POWER_KW = 1
        private const val MAX_POWER_KW = 300
        private const val MIN_YEAR = 1950
    }
}
