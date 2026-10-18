package nl.vlugverkoop.model

import java.time.Year

/**
 * Colour of a Dutch scooter licence plate. The colour is tied to the legal top speed.
 *
 * @property maxSpeedKmh legal top speed in km/h that belongs to this plate colour.
 */
enum class PlateColor(val maxSpeedKmh: Int) {
    /** Snorfiets, max 25 km/h. */
    BLUE(25),

    /** Bromfiets, max 45 km/h. */
    YELLOW(45);

    companion object {
        /** Every speed class a Dutch scooter is allowed to have. */
        val allowedSpeeds: Set<Int> get() = entries.map { it.maxSpeedKmh }.toSet()

        /** Returns the plate colour that belongs to [maxSpeedKmh], or null when no colour matches. */
        fun forSpeed(maxSpeedKmh: Int): PlateColor? = entries.find { it.maxSpeedKmh == maxSpeedKmh }
    }
}

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
     *
     * The speed class is not checked here: an invalid speed never gets this far,
     * because the constructor refuses to build such a scooter at all.
     */
    override fun validate(): List<String> = buildList {
        if (brand.isBlank()) {
            add("brand is required")
        }
        if (model.isBlank()) {
            add("model is required")
        }
        if (price <= 0) {
            add("price must be greater than 0")
        }
        if (price > MAX_PRICE) {
            add("price must not be higher than $MAX_PRICE")
        }
        if (mileage < 0) {
            add("mileage cannot be negative")
        }
        if (year !in MIN_YEAR..Year.now().value) {
            add("year must be between $MIN_YEAR and ${Year.now().value}")
        }
    }

    companion object {
        /** What the seller pays to place a scooter listing. */
        const val LISTING_FEE = 2.95

        private const val MIN_YEAR = 1950
        private const val MAX_PRICE = 25_000.0
    }
}
