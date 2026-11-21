package nl.vlugverkoop.model

import java.time.Year
import kotlin.math.roundToInt

/**
 * Base class for every vehicle that can be listed on VlugVerkoop.
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

    /**
     * Extra value factor that differs per vehicle type.
     * 1.0 means no change, 0.9 means 10% less value.
     */
    protected abstract fun valueFactor(): Double

    /**
     * Estimates what this vehicle is worth today.
     * The value goes down with age and mileage, and with the vehicle's own [valueFactor].
     *
     * @param currentYear the year to calculate with; defaults to this year.
     * @return the estimated value, rounded to 2 decimals.
     */
    fun estimatedValue(currentYear: Int = Year.now().value): Double {
        // 1. Age: 8% less value per year, for at most 10 years
        val age = (currentYear - year).coerceAtLeast(0)
        val ageFactor = 1.0 - minOf(age, MAX_AGE_YEARS) * DEPRECIATION_PER_YEAR

        // 2. Mileage: more kilometres means less value
        val mileageFactor = when {
            mileage < 50_000 -> 1.0
            mileage < 100_000 -> 0.9
            mileage < 150_000 -> 0.8
            else -> 0.7
        }

        // 3. Combine everything with the vehicle's own factor and round to 2 decimals
        val value = price * ageFactor * mileageFactor * valueFactor()
        return (value * 100).roundToInt() / 100.0
    }

    companion object {
        private const val DEPRECIATION_PER_YEAR = 0.08
        private const val MAX_AGE_YEARS = 10
    }
}