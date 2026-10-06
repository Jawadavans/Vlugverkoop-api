package nl.vlugverkoop.model

/**
 * Base class for every vehicle that can be listed on Vlugverkoop.
 */
abstract class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int
) {
    /** Fee the seller pays to place a listing. Differs per vehicle type. */
    abstract fun calculateListingFee(): Double
}