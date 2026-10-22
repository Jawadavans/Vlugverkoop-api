package nl.vlugverkoop

import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.Scooter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Unit tests for the scooter rules. No server needed. */
class ScooterTest {

    private fun scooter(
        price: Double = 2650.0,
        mileage: Int = 4200,
        year: Int = 2021,
        brand: String = "Vespa",
        maxSpeedKmh: Int = 45
    ) = Scooter(
        id = 1,
        brand = brand,
        model = "Primavera",
        year = year,
        price = price,
        mileage = mileage,
        maxSpeedKmh = maxSpeedKmh
    )

    @Test
    fun `25 kmh gives a blue plate and 45 kmh gives a yellow plate`() {
        assertEquals(PlateColor.BLUE, scooter(maxSpeedKmh = 25).plateColor)
        assertEquals(PlateColor.YELLOW, scooter(maxSpeedKmh = 45).plateColor)
    }

    @Test
    fun `a speed class other than 25 or 45 cannot be built at all`() {
        val error = assertFailsWith<IllegalArgumentException> { scooter(maxSpeedKmh = 80) }

        assertEquals("maxSpeedKmh must be one of [25, 45], was 80", error.message)
    }

    @Test
    fun `a negative price and a negative mileage are both reported`() {
        val errors = scooter(price = -1.0, mileage = -50).validate()

        assertEquals(2, errors.size)
        assertContains(errors, "price must be greater than 0")
        assertContains(errors, "mileage cannot be negative")
    }
}
