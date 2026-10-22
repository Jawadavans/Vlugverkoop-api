package nl.vlugverkoop.model

import java.time.Year

/**
 * A car listed for sale on VlugVerkoop.
 *
 * @property doors number of doors of the car (2 to 7).
 */
class Car(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val doors: Int
) : Vehicle(id, brand, model, year, price, mileage), Validatable {

    override fun calculateListingFee(): Double = LISTING_FEE

    /**
     * Checks whether this car may be listed.
     * Returns a list of error messages; an empty list means the car is valid.
     */
    override fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val currentYear = Year.now().value

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
        if (year !in MIN_YEAR..currentYear) {
            errors.add("year must be between $MIN_YEAR and $currentYear")
        }
        if (doors !in MIN_DOORS..MAX_DOORS) {
            errors.add("doors must be between $MIN_DOORS and $MAX_DOORS")
        }
        return errors
    }

    companion object {
        private const val LISTING_FEE = 9.95
        private const val MIN_YEAR = 1950
        private const val MIN_DOORS = 2
        private const val MAX_DOORS = 7
    }
}