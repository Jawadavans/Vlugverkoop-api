package nl.vlugverkoop.model

/**
 * A motorcycle listed for sale on VlugVerkoop.
 *
 * @property engineCc engine displacement in cubic centimetres.
 * @property style the riding style, used for filtering.
 * @property licenseCategory the lowest license needed to ride this motorcycle.
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

    /**
     * Checks the motorcycle-specific rules.
     * Returns a list of error messages; an empty list means the motorcycle is valid.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (engineCc !in MIN_ENGINE_CC..MAX_ENGINE_CC) {
            errors.add("engineCc must be between $MIN_ENGINE_CC and $MAX_ENGINE_CC")
        }
        if (licenseCategory < LicenseCategory.minimumFor(engineCc)) {
            errors.add("LicenseCategory $licenseCategory is not allowed for $engineCc cc")
        }
        return errors
    }
    companion object {
        private const val LISTING_FEE = 4.95
        private const val MIN_ENGINE_CC = 50
        private const val MAX_ENGINE_CC = 2500
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