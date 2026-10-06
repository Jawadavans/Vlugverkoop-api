package nl.vlugverkoop.model

/**
 * A scooter listed for sale on Vlugverkoop.
 *
 * @property maxSpeedKmh legal top speed in km/h (25 or 45 in the Netherlands).
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

    override fun calculateListingFee(): Double = 2.95
}