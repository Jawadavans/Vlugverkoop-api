package nl.vlugverkoop

import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.Motorcycle
import nl.vlugverkoop.model.MotorcycleStyle
import java.time.Year
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the validation and licence rules of [Motorcycle].
 */
class MotorcycleTest {

    /** Creates a valid motorcycle. Change one value to test one rule. */
    private fun motorcycle(
        brand: String = "Yamaha",
        model: String = "MT-07",
        year: Int = 2019,
        mileage: Int = 18200,
        engineCc: Int = 689,
        powerKw: Int = 35,
        licenseCategory: LicenseCategory = LicenseCategory.A2
    ) = Motorcycle(1, brand, model, year, 6450.0, mileage, engineCc, powerKw, MotorcycleStyle.NAKED, licenseCategory)

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
    fun `motorcycle with a licence other than the minimum is rejected`() {
        val tooLow = motorcycle(powerKw = 54, licenseCategory = LicenseCategory.A2).validate()
        val tooHigh = motorcycle(engineCc = 125, powerKw = 11, licenseCategory = LicenseCategory.A).validate()

        assertEquals(listOf("licenseCategory A2 is not allowed for 689 cc and 54 kW"), tooLow)
        assertEquals(listOf("licenseCategory A is not allowed for 125 cc and 11 kW"), tooHigh)
    }

    @Test
    fun `brand, model, mileage, year, engine and power rules hold at their limits`() {
        val currentYear = Year.now().value
        val yearError = listOf("year must be between 1950 and $currentYear")
        val engineError = listOf("engineCc must be between 50 and 2500")
        val powerError = listOf("powerKw must be between 1 and 300")

        assertEquals(listOf("brand is required"), motorcycle(brand = "  ").validate())
        assertEquals(listOf("model is required"), motorcycle(model = "").validate())
        assertEquals(listOf("mileage cannot be negative"), motorcycle(mileage = -1).validate())
        assertEquals(yearError, motorcycle(year = 1949).validate())
        assertEquals(yearError, motorcycle(year = currentYear + 1).validate())
        assertEquals(engineError, motorcycle(engineCc = 49).validate())
        assertEquals(engineError, motorcycle(engineCc = 2501).validate())
        assertEquals(powerError, motorcycle(powerKw = 0).validate())
        assertEquals(powerError, motorcycle(powerKw = 301, licenseCategory = LicenseCategory.A).validate())

        assertTrue(motorcycle(mileage = 0).validate().isEmpty())
        assertTrue(motorcycle(year = 1950).validate().isEmpty())
        assertTrue(motorcycle(year = currentYear).validate().isEmpty())
        assertTrue(motorcycle(engineCc = 50).validate().isEmpty())
        assertTrue(motorcycle(engineCc = 2500).validate().isEmpty())
        assertTrue(motorcycle(powerKw = 12).validate().isEmpty())
        assertTrue(motorcycle(powerKw = 300, licenseCategory = LicenseCategory.A).validate().isEmpty())
    }
}
