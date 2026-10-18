package nl.vlugverkoop

import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.Scooter
import nl.vlugverkoop.repository.ScooterRepository
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Unit tests for the scooter rules and filters. No server needed. */
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
    fun `a valid scooter has no validation errors`() {
        assertTrue(scooter().validate().isEmpty())
    }

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
    fun `a scooter from before 1950 is rejected`() {
        val errors = scooter(year = 1890).validate()

        assertTrue(errors.any { it.startsWith("year must be between") }, "expected a year error, got $errors")
    }

    @Test
    fun `the listing fee for a scooter is 2 euro 95`() {
        assertEquals(2.95, scooter().calculateListingFee())
    }

    @Test
    fun `filtering combines the plate colour and the maximum price`() {
        val found = ScooterRepository().find(plateColor = PlateColor.BLUE, maxPrice = 1500.0)

        assertTrue(found.isNotEmpty())
        assertTrue(found.all { it.plateColor == PlateColor.BLUE && it.price <= 1500.0 }, "wrong result: $found")
    }

    @Test
    fun `searching ignores upper and lower case`() {
        val found = ScooterRepository().find(search = "vESpA")

        assertEquals(listOf("Vespa"), found.map { it.brand })
    }
}
