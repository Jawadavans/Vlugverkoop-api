package nl.vlugverkoop.model

/**
 * Colour of a Dutch scooter licence plate. The colour is tied to the legal top speed.
 *
 * @property maxSpeedKmh legal top speed in km/h that belongs to this plate colour.
 */
enum class PlateColor(val maxSpeedKmh: Int) {
    /** Snorfiets, max 25 km/h. */
    BLUE(25),

    /** Bromfiets, max 45 km/h. */
    YELLOW(45)
}

/**
 * A scooter listed for sale on VlugVerkoop.
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
) : Vehicle(id, brand, model, year, price, mileage) {

    val plateColor: PlateColor = requireNotNull(PlateColor.entries.find { it.maxSpeedKmh == maxSpeedKmh }) {
        "Speed class must be 25 or 45 km/h, was $maxSpeedKmh"
    }

    override fun calculateListingFee(): Double = 2.95
}
