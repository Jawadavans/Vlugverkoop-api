package nl.vlugverkoop

import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.Scooter
import java.time.Year
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for the validation and plate colour rules of [Scooter].
 */
class ScooterTest {

    /** Creates a valid scooter. Change one value to test one rule. */
    private fun scooter(
        price: Double = 2650.0,
        mileage: Int = 4200,
        year: Int = 2021,
        brand: String = "Vespa",
        model: String = "Primavera",
        maxSpeedKmh: Int = 45
    ) = Scooter(
        id = 1,
        brand = brand,
        model = model,
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

    @Test
    fun `brand, model, mileage, year and price rules hold at their limits`() {
        val currentYear = Year.now().value
        val yearError = listOf("year must be between 1950 and $currentYear")

        assertEquals(listOf("brand is required"), scooter(brand = "  ").validate())
        assertEquals(listOf("model is required"), scooter(model = "").validate())
        assertEquals(listOf("mileage cannot be negative"), scooter(mileage = -1).validate())
        assertEquals(yearError, scooter(year = 1949).validate())
        assertEquals(yearError, scooter(year = currentYear + 1).validate())
        assertEquals(listOf("price must not be higher than 25000.0"), scooter(price = 25_000.01).validate())

        assertTrue(scooter(mileage = 0).validate().isEmpty())
        assertTrue(scooter(year = 1950).validate().isEmpty())
        assertTrue(scooter(year = currentYear).validate().isEmpty())
        assertTrue(scooter(price = 25_000.0).validate().isEmpty())
    }
}
