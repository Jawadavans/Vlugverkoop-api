package nl.vlugverkoop.repository

import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.Scooter

/**
 * Stores scooters in memory. Will be replaced by a real database later.
 */
class ScooterRepository {

    private val scooters = mutableListOf(
        Scooter(1, "Vespa", "Primavera", 2021, 2650.0, 4200, 45),
        Scooter(2, "Piaggio", "Zip", 2019, 1150.0, 9800, 25),
        Scooter(3, "Kymco", "Agility", 2020, 1395.0, 6100, 25),
        Scooter(4, "Peugeot", "Kisbee", 2022, 1750.0, 2500, 45),
        Scooter(5, "Yamaha", "Neo's", 2023, 2995.0, 900, 25),
        Scooter(6, "SYM", "Fiddle IV", 2021, 1895.0, 7400, 45)
    )

    /** Returns all scooters. */
    fun getAll(): List<Scooter> = scooters

    /**
     * Returns the scooters that pass every filter. A filter that is null is skipped.
     *
     * @param search text that must appear in the brand or model, e.g. "vespa".
     * @param plateColor only scooters with this licence plate colour.
     * @param maxPrice highest price the buyer wants to pay.
     * @param maxMileage highest mileage the buyer accepts.
     */
    fun find(
        search: String? = null,
        plateColor: PlateColor? = null,
        maxPrice: Double? = null,
        maxMileage: Int? = null
    ): List<Scooter> = scooters.filter {
        (search.isNullOrBlank() || "${it.brand} ${it.model}".contains(search.trim(), ignoreCase = true)) &&
            (plateColor == null || it.plateColor == plateColor) &&
            (maxPrice == null || it.price <= maxPrice) &&
            (maxMileage == null || it.mileage <= maxMileage)
    }

    /** Returns the scooter with this [id], or null when it does not exist. */
    fun getById(id: Int): Scooter? = scooters.find { it.id == id }

    /** Returns the id the next new scooter should get. */
    fun nextId(): Int = (scooters.maxOfOrNull { it.id } ?: 0) + 1

    /** Stores a new scooter. */
    fun add(scooter: Scooter) {
        scooters.add(scooter)
    }

    /**
     * Replaces the stored scooter that has the same id as [scooter].
     *
     * @return false when no scooter with that id exists.
     */
    fun update(scooter: Scooter): Boolean {
        val index = scooters.indexOfFirst { it.id == scooter.id }
        if (index == -1) {
            return false
        }
        scooters[index] = scooter
        return true
    }

    /** Removes the scooter with this [id]. Returns false when it did not exist. */
    fun delete(id: Int): Boolean = scooters.removeIf { it.id == id }
}
