package nl.vlugverkoop.model

/**
 * A motorcycle listed for sale on Vlugverkoop.
 *
 * @property engineCc engine displacement in cubic centimetres.
 */
class Motorcycle(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val engineCc: Int
) : Vehicle(id, brand, model, year, price, mileage) {

    override fun calculateListingFee(): Double = 4.95
}