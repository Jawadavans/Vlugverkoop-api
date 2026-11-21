package nl.vlugverkoop

import nl.vlugverkoop.dto.CarForm
import nl.vlugverkoop.dto.MotorcycleForm
import nl.vlugverkoop.dto.ScooterForm
import nl.vlugverkoop.dto.toPlateCheckResponse
import nl.vlugverkoop.model.A2Restriction
import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.VehicleType
import nl.vlugverkoop.rdw.RdwVehicle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the licence rules on their limits, and for turning RDW data into a prefilled listing.
 */
class PlateCheckTest {

    /** RDW data for a motorcycle. Change one value to test one rule. */
    private fun rdwMotorcycle(engineCc: Int? = 689, powerKw: Double? = 54.0) = RdwVehicle(
        plate = "MZ12AB",
        rdwKind = "Motorfiets",
        brand = "YAMAHA",
        model = "MT-07",
        year = 2019,
        engineCc = engineCc,
        doors = null,
        maxSpeedKmh = null,
        powerKw = powerKw
    )

    @Test
    fun `licence is A1 up to 125 cc and 11 kW, A2 up to 35 kW, A above that, with decimals`() {
        assertEquals(LicenseCategory.A1, LicenseCategory.minimumFor(125, 11.0))
        assertEquals(LicenseCategory.A2, LicenseCategory.minimumFor(126, 11.0))
        assertEquals(LicenseCategory.A2, LicenseCategory.minimumFor(125, 11.1))
        assertEquals(LicenseCategory.A2, LicenseCategory.minimumFor(689, 35.0))
        assertEquals(LicenseCategory.A, LicenseCategory.minimumFor(689, 35.1))
    }

    @Test
    fun `restricting for A2 is not needed up to 35 kW and possible up to and including 70 kW`() {
        assertEquals(A2Restriction.NOT_NEEDED, A2Restriction.forPower(11.0))
        assertEquals(A2Restriction.NOT_NEEDED, A2Restriction.forPower(35.0))
        assertEquals(A2Restriction.POSSIBLE, A2Restriction.forPower(35.1))
        assertEquals(A2Restriction.POSSIBLE, A2Restriction.forPower(70.0))
        assertEquals(A2Restriction.NOT_POSSIBLE, A2Restriction.forPower(70.1))
    }

    @Test
    fun `RDW kinds are recognised as car, motorcycle or scooter, anything else is not supported`() {
        assertEquals(VehicleType.CAR, VehicleType.fromRdwKind("Personenauto"))
        assertEquals(VehicleType.MOTORCYCLE, VehicleType.fromRdwKind("motorfiets"))
        assertEquals(VehicleType.SCOOTER, VehicleType.fromRdwKind(" Bromfiets "))
        assertNull(VehicleType.fromRdwKind("Bedrijfsauto"))
        assertNull(VehicleType.fromRdwKind(null))
        assertNull(rdwMotorcycle().copy(rdwKind = "Aanhangwagen").toPlateCheckResponse())
    }

    @Test
    fun `a motorcycle gets its licence, restriction and a prefilled form with the power rounded up`() {
        val result = rdwMotorcycle(engineCc = 125, powerKw = 11.3).toPlateCheckResponse()!!

        assertEquals(VehicleType.MOTORCYCLE, result.vehicleType)
        assertEquals(LicenseCategory.A2, result.licenseCategory, "11.3 kW is more than A1 allows")
        assertEquals(A2Restriction.NOT_NEEDED, result.a2Restriction)
        assertEquals(
            MotorcycleForm("YAMAHA", "MT-07", 2019, engineCc = 125, powerKw = 12, licenseCategory = LicenseCategory.A2),
            result.motorcycleForm
        )
        assertNull(result.carForm, "only the motorcycle form is filled")
        assertNull(result.scooterForm, "only the motorcycle form is filled")
        // Rounding up keeps the licence of the form equal to the real one
        assertEquals(result.licenseCategory, LicenseCategory.minimumFor(125, 12))
    }

    @Test
    fun `a motorcycle without a known power gets no licence advice`() {
        val result = rdwMotorcycle(powerKw = null).toPlateCheckResponse()!!

        assertNull(result.licenseCategory)
        assertNull(result.a2Restriction)
        assertNull(result.motorcycleForm?.powerKw)
    }

    @Test
    fun `a car and a scooter get their own form, and only motorcycles get licence advice`() {
        val car = rdwMotorcycle().copy(rdwKind = "Personenauto", doors = 5, engineCc = 1598, powerKw = 85.0)
            .toPlateCheckResponse()!!
        assertEquals(CarForm("YAMAHA", "MT-07", 2019, doors = 5), car.carForm)
        assertNull(car.licenseCategory)
        assertNull(car.a2Restriction)

        val scooter = rdwMotorcycle().copy(rdwKind = "Bromfiets", maxSpeedKmh = 45).toPlateCheckResponse()!!
        assertEquals(ScooterForm("YAMAHA", "MT-07", 2019, maxSpeedKmh = 45), scooter.scooterForm)

        // A speed that is not a legal class (25 or 45) is shown, but not put in the form
        val oddScooter = rdwMotorcycle().copy(rdwKind = "Bromfiets", maxSpeedKmh = 40).toPlateCheckResponse()!!
        assertEquals(40, oddScooter.maxSpeedKmh)
        assertNull(oddScooter.scooterForm?.maxSpeedKmh)
    }
}
