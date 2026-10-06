package nl.vlugverkoop.model

/**
 * A car listed for sale on Vlugverkoop.
 *
 * @property doors number of doors of the car.
 */
class Car(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val doors: Int
) : Vehicle(id, brand, model, year, price, mileage) {

    override fun calculateListingFee(): Double = 9.95
}