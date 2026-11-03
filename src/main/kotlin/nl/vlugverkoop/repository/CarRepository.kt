package nl.vlugverkoop.repository

import nl.vlugverkoop.model.Car

/**
 * Stores cars in memory. Will be replaced by a real database later.
 */
class CarRepository {

    private val cars = mutableListOf(
        Car(1, "Volkswagen", "Golf", 2018, 14500.0, 85000, 5),
        Car(2, "Toyota", "Yaris", 2020, 16950.0, 42000, 5),
        Car(3, "BMW", "3 Series", 2016, 18750.0, 120000, 4),
        Car(4, "Volkswagen", "Polo", 2021, 17250.0, 31000, 5),
        Car(5, "Peugeot", "208", 2015, 6950.0, 135000, 3),
        Car(6, "Tesla", "Model 3", 2022, 34900.0, 48000, 4)
    )

    /** Returns all cars. */
    fun getAll(): List<Car> = cars.toList()

    /**
     * Returns the cars that pass every filter. A filter that is null is skipped.
     *
     * @param search text that must appear in the brand or model, e.g. "volkswagen".
     * @param maxPrice highest price the buyer wants to pay.
     * @param maxMileage highest mileage the buyer accepts.
     * @param doors exact number of doors.
     */
    fun find(
        search: String? = null,
        maxPrice: Double? = null,
        maxMileage: Int? = null,
        doors: Int? = null
    ): List<Car> = cars.filter {
        (search.isNullOrBlank() || "${it.brand} ${it.model}".contains(search.trim(), ignoreCase = true)) &&
            (maxPrice == null || it.price <= maxPrice) &&
            (maxMileage == null || it.mileage <= maxMileage) &&
            (doors == null || it.doors == doors)
    }

    /** Returns the car with this [id], or null when it does not exist. */
    fun getById(id: Int): Car? = cars.find { it.id == id }

    /** Returns the id the next new car should get. */
    fun nextId(): Int = (cars.maxOfOrNull { it.id } ?: 0) + 1

    /** Stores a new car. */
    fun add(car: Car) {
        cars.add(car)
    }

    /**
     * Replaces the stored car that has the same id as [car].
     *
     * @return false when no car with that id exists.
     */
    fun update(car: Car): Boolean {
        val index = cars.indexOfFirst { it.id == car.id }
        if (index == -1) {
            return false
        }
        cars[index] = car
        return true
    }

    /** Removes the car with this [id]. Returns false when it did not exist. */
    fun delete(id: Int): Boolean = cars.removeIf { it.id == id }
}
