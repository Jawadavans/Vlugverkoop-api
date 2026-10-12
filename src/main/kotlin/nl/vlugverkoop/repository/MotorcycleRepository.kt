package nl.vlugverkoop.repository

import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.Motorcycle
import nl.vlugverkoop.model.MotorcycleStyle

/**
 * Stores motorcycles in memory. Will be replaced by a real database later.
 */
class MotorcycleRepository {

    private val motorcycles = mutableListOf(
        Motorcycle(1, "Yamaha", "MT-07", 2019, 6450.0, 18200, 689, 35, MotorcycleStyle.NAKED, LicenseCategory.A2),
        Motorcycle(2, "Honda", "CB125R", 2021, 3950.0, 6400, 125, 11, MotorcycleStyle.NAKED, LicenseCategory.A1),
        Motorcycle(3, "BMW", "R 1250 GS", 2020, 15900.0, 32500, 1254, 100, MotorcycleStyle.TOURING, LicenseCategory.A),
        Motorcycle(4, "Kawasaki", "Ninja 650", 2018, 5250.0, 24100, 649, 35, MotorcycleStyle.SPORT, LicenseCategory.A2),
        Motorcycle(5, "KTM", "690 Enduro R", 2019, 8750.0, 15300, 693, 55, MotorcycleStyle.ENDURO, LicenseCategory.A),
        Motorcycle(
            6, "Harley-Davidson", "Street Bob", 2018, 12450.0, 21000, 1746, 64,
            MotorcycleStyle.CHOPPER, LicenseCategory.A
        )
    )

    /** Returns all motorcycles. */
    fun getAll(): List<Motorcycle> = motorcycles

    /**
     * Returns the motorcycles that pass every filter. A filter that is null is skipped.
     *
     * @param search text that must appear in the brand or model, e.g. "yamaha".
     * @param license the buyer's licence; returns every motorcycle the buyer is allowed to ride.
     */
    fun find(
        search: String? = null,
        style: MotorcycleStyle? = null,
        license: LicenseCategory? = null,
        minCc: Int? = null,
        maxCc: Int? = null,
        maxPrice: Double? = null
    ): List<Motorcycle> = motorcycles.filter {
        (search.isNullOrBlank() || "${it.brand} ${it.model}".contains(search.trim(), ignoreCase = true)) &&
            (style == null || it.style == style) &&
            (license == null || it.licenseCategory <= license) &&
            (minCc == null || it.engineCc >= minCc) &&
            (maxCc == null || it.engineCc <= maxCc) &&
            (maxPrice == null || it.price <= maxPrice)
    }

    /** Returns the motorcycle with this [id], or null when it does not exist. */
    fun getById(id: Int): Motorcycle? = motorcycles.find { it.id == id }

    /** Stores a new motorcycle. */
    fun add(motorcycle: Motorcycle) {
        motorcycles.add(motorcycle)
    }

    /** Returns the id the next new motorcycle should get. */
    fun nextId(): Int = (motorcycles.maxOfOrNull { it.id } ?: 0) + 1

    /** Removes the motorcycle with this [id]. Returns false when it did not exist. */
    fun delete(id: Int): Boolean = motorcycles.removeIf { it.id == id }
}
