package nl.vlugverkoop

import nl.vlugverkoop.model.Car
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the validation rules of [Car].
 */
class CarTest {

    /** Creates a valid car. Change one value to test one rule. */
    private fun car(
        brand: String = "Toyota",
        model: String = "Yaris",
        year: Int = 2020,
        price: Double = 16950.0,
        mileage: Int = 42000,
        doors: Int = 5
    ) = Car(1, brand, model, year, price, mileage, doors)

    @Test
    fun `valid car has no validation errors`() {
        val errors = car().validate()

        assertTrue(errors.isEmpty(), "Expected no errors but got $errors")
    }

    @Test
    fun `car with a price of zero is rejected`() {
        val errors = car(price = 0.0).validate()

        assertEquals(listOf("price must be greater than 0"), errors)
    }

    @Test
    fun `car with too few or too many doors is rejected`() {
        assertEquals(listOf("doors must be between 2 and 7"), car(doors = 1).validate())
        assertEquals(listOf("doors must be between 2 and 7"), car(doors = 8).validate())
    }
}