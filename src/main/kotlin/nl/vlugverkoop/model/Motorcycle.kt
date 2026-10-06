package nl.vlugverkoop.model

/**
 * A motorcycle listed for sale on Vlugverkoop.
 *
 * @property engineCc engine displacement in cubic centimetres.
 * @property style the riding style, usee for filtering.
 * @propery LicenseCategory the lowest license needed to ride this motorcycle.
 */
class Motorcycle(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val engineCc: Int,
    val style: MotorcycleStyle,
    val licenseCategory: LicenseCategory
) : Vehicle(id, brand, model, year, price, mileage) {

    override fun calculateListingFee(): Double = LISTING_FEE

    companion object {
        private const val LISTING_FEE = 4.95
    }
}

fun main() {
    val bike = Motorcycle(
        id = 1,
        brand = "Yamaha",
        model = "MT-07",
        year = 2019,
        price = 6450.0,
        mileage = 18200,
        engineCc = 689,
        style = MotorcycleStyle.NAKED,
        licenseCategory = LicenseCategory.A2
    )
    println(bike.engineCc)
    println(bike.style)
    println(bike.licenseCategory)
    println(LicenseCategory.minimumFor(bike.engineCc))
    println(bike.calculateListingFee())
}