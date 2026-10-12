package nl.vlugverkoop

import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.Motorcycle
import nl.vlugverkoop.model.MotorcycleStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Unit tests for the motorcycle rules. No server needed. */
class MotorcycleTest {

    private fun motorcycle(
        engineCc: Int = 689,
        powerKw: Int = 35,
        license: LicenseCategory = LicenseCategory.A2
    ) = Motorcycle(
        id = 1,
        brand = "Yamaha",
        model = "MT-07",
        year = 2019,
        price = 6450.0,
        mileage = 18200,
        engineCc = engineCc,
        powerKw = powerKw,
        style = MotorcycleStyle.NAKED,
        licenseCategory = license
    )

    @Test
    fun `valid motorcycle has no validation errors`() {
        assertTrue(motorcycle().validate().isEmpty())
    }

    @Test
    fun `A1 licence is rejected for a motorcycle above 125 cc`() {
        val errors = motorcycle(engineCc = 689, license = LicenseCategory.A1).validate()

        assertEquals(listOf("licenseCategory A1 is not allowed for 689 cc and 35 kW"), errors)
    }

    @Test
    fun `A2 licence is rejected for a motorcycle above 35 kW`() {
        val errors = motorcycle(powerKw = 54, license = LicenseCategory.A2).validate()

        assertEquals(listOf("licenseCategory A2 is not allowed for 689 cc and 54 kW"), errors)
    }
}
