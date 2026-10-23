package nl.vlugverkoop

import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.Motorcycle
import nl.vlugverkoop.model.MotorcycleStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the validation and licence rules of [Motorcycle].
 */
class MotorcycleTest {

    /** Creates a valid motorcycle. Change one value to test one rule. */
    private fun motorcycle(
        engineCc: Int = 689,
        powerKw: Int = 35,
        licenseCategory: LicenseCategory = LicenseCategory.A2
    ) = Motorcycle(1, "Yamaha", "MT-07", 2019, 6450.0, 18200, engineCc, powerKw, MotorcycleStyle.NAKED, licenseCategory)

    @Test
    fun `valid motorcycle has no validation errors`() {
        val errors = motorcycle().validate()

        assertTrue(errors.isEmpty(), "Expected no errors but got $errors")
    }

    @Test
    fun `minimum licence is A1 up to 125 cc and 11 kW, A2 up to 35 kW and A above that`() {
        assertEquals(LicenseCategory.A1, LicenseCategory.minimumFor(125, 11))
        assertEquals(LicenseCategory.A2, LicenseCategory.minimumFor(126, 11))
        assertEquals(LicenseCategory.A2, LicenseCategory.minimumFor(125, 12))
        assertEquals(LicenseCategory.A2, LicenseCategory.minimumFor(689, 35))
        assertEquals(LicenseCategory.A, LicenseCategory.minimumFor(689, 36))
    }

    @Test
    fun `motorcycle with a licence that is too low is rejected`() {
        val errors = motorcycle(powerKw = 54, licenseCategory = LicenseCategory.A2).validate()

        assertEquals(listOf("licenseCategory A2 is not allowed for 689 cc and 54 kW"), errors)
    }
}
