package nl.vlugverkoop.repository

import nl.vlugverkoop.model.Car

/**
 * Stores cars in memory. Will be replaced by a real database later.
 */
class CarRepository {

    private val cars = mutableListOf(
        Car(1, "Volkswagen", "Golf", 2018, 14500.0, 85000, 5),
        Car(2, "Toyota", "Yaris", 2020, 16950.0, 42000, 5),
        Car(3, "BMW", "3 Series", 2016, 18750.0, 120000, 4)
    )

    /** Returns all cars. */
    fun getAll(): List<Car> = cars

    /** Deletes the car with the given id. Returns true if a car was removed. */
    fun delete(id: Int): Boolean = cars.removeIf { it.id == id }

    /** Adds a new car with a new id. Returns the saved car. */
    fun add(brand: String, model: String, year: Int, price: Double, mileage: Int, doors: Int): Car {
        val newId = (cars.maxOfOrNull { it.id } ?: 0) + 1
        val car = Car(newId, brand, model, year, price, mileage, doors)
        cars.add(car)
        return car
    }
}