package nl.vlugverkoop.model

import java.time.Year

/**
 * A scooter listed for sale on VlugVerkoop.
 *
 * The plate colour is never supplied by the app: it is derived from [maxSpeedKmh],
 * so a listing with a speed and a plate colour that contradict each other cannot exist.
 *
 * @property maxSpeedKmh legal top speed in km/h (25 or 45 in the Netherlands).
 * @property plateColor licence plate colour that matches [maxSpeedKmh].
 * @throws IllegalArgumentException if [maxSpeedKmh] is not 25 or 45.
 */
class Scooter(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val maxSpeedKmh: Int
) : Vehicle(id, brand, model, year, price, mileage), Validatable {

    val plateColor: PlateColor = requireNotNull(PlateColor.forSpeed(maxSpeedKmh)) {
        "maxSpeedKmh must be one of ${PlateColor.allowedSpeeds}, was $maxSpeedKmh"
    }

    override fun calculateListingFee(): Double = LISTING_FEE

    /**
     * Checks whether this scooter may be listed.
     * Returns a list of error messages; an empty list means the scooter is valid.
     *
     * The speed class is not checked here: an invalid speed never gets this far,
     * because the constructor refuses to build such a scooter at all.
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
        if (price > MAX_PRICE) {
            errors.add("price must not be higher than $MAX_PRICE")
        }
        if (mileage < 0) {
            errors.add("mileage cannot be negative")
        }
        if (year !in MIN_YEAR..currentYear) {
            errors.add("year must be between $MIN_YEAR and $currentYear")
        }
        return errors
    }

    companion object {
        private const val LISTING_FEE = 2.95
        private const val MIN_YEAR = 1950
        private const val MAX_PRICE = 25_000.0
    }
}
