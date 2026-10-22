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
     * Returns the cars that match every filter that is given.
     * A filter that is null is ignored.
     *
     * @param search text that must appear in the brand or model (not case sensitive).
     * @param maxPrice highest price in euro.
     * @param maxMileage highest mileage in km.
     * @param doors exact number of doors.
     */
    fun find(
        search: String? = null,
        maxPrice: Double? = null,
        maxMileage: Int? = null,
        doors: Int? = null
    ): List<Car> = cars.filter { car ->
        (search == null || car.brand.contains(search, ignoreCase = true) || car.model.contains(search, ignoreCase = true)) &&
                (maxPrice == null || car.price <= maxPrice) &&
                (maxMileage == null || car.mileage <= maxMileage) &&
                (doors == null || car.doors == doors)
    }

    /** Returns the car with the given id, or null if it does not exist. */
    fun getById(id: Int): Car? = cars.find { it.id == id }

    /** Returns the id the next new car will get. */
    fun nextId(): Int = (cars.maxOfOrNull { it.id } ?: 0) + 1

    /** Adds a car that was already validated. Returns the saved car. */
    fun add(car: Car): Car {
        cars.add(car)
        return car
    }

    /** Replaces the car with the given id. Returns false if that car does not exist. */
    fun update(id: Int, car: Car): Boolean {
        val index = cars.indexOfFirst { it.id == id }
        if (index == -1) {
            return false
        }
        cars[index] = car
        return true
    }

    /** Deletes the car with the given id. Returns true if a car was removed. */
    fun delete(id: Int): Boolean = cars.removeIf { it.id == id }
}