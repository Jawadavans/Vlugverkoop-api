package nl.vlugverkoop.repository

import nl.vlugverkoop.model.Scooter

/**
 * Stores scooters in memory. Will be replaced by a real database later.
 */
class ScooterRepository {

    private val scooters = mutableListOf(
        Scooter(1, "Vespa", "Primavera", 2021, 2650.0, 4200, 45),
        Scooter(2, "Piaggio", "Zip", 2019, 1150.0, 9800, 25),
        Scooter(3, "Kymco", "Agility", 2020, 1395.0, 6100, 25)
    )

    /** Returns all scooters. */
    fun getAll(): List<Scooter> = scooters
}
