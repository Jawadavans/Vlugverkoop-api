package nl.vlugverkoop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.vlugverkoop.dto.CarForm
import nl.vlugverkoop.dto.MotorcycleForm
import nl.vlugverkoop.dto.PlateCheckErrorResponse
import nl.vlugverkoop.dto.PlateCheckResponse
import nl.vlugverkoop.dto.ScooterForm
import nl.vlugverkoop.model.A2Restriction
import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.VehicleType
import nl.vlugverkoop.rdw.RdwClient
import nl.vlugverkoop.routes.plateRoutes
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for GET /plates/{plate}: the real route, RDW client and DTOs together,
 * with a fake RDW so the tests never need the internet.
 *
 * The fake answers use the RDW field names, with every value as text, the way RDW Open Data sends them.
 */
class PlateRoutesTest {

    private companion object {
        /** What the fake RDW knows in the vehicle data set (m9d7-ebf2), per plate. */
        val FAKE_VEHICLES = mapOf(
            "AB123C" to """[{"kenteken":"AB123C","voertuigsoort":"Personenauto","merk":"VOLKSWAGEN",
                "handelsbenaming":"GOLF","datum_eerste_toelating":"20190312","cilinderinhoud":"1498","aantal_deuren":"5"}]""",
            "MZ12AB" to """[{"kenteken":"MZ12AB","voertuigsoort":"Motorfiets","merk":"YAMAHA",
                "handelsbenaming":"MT-07","datum_eerste_toelating":"20190405","cilinderinhoud":"689"}]""",
            "F123BB" to """[{"kenteken":"F123BB","voertuigsoort":"Bromfiets","merk":"PIAGGIO",
                "handelsbenaming":"ZIP","datum_eerste_toelating":"20210601","cilinderinhoud":"49",
                "maximale_constructiesnelheid":"45"}]""",
            "VX123B" to """[{"kenteken":"VX123B","voertuigsoort":"Bedrijfsauto","merk":"FORD","handelsbenaming":"TRANSIT"}]"""
        )

        /** What the fake RDW knows in the fuel data set (8ys7-d773), per plate. */
        val FAKE_FUEL = mapOf(
            "AB123C" to """[{"kenteken":"AB123C","brandstof_omschrijving":"Benzine","nettomaximumvermogen":"110.00"}]""",
            "MZ12AB" to """[{"kenteken":"MZ12AB","brandstof_omschrijving":"Benzine","nettomaximumvermogen":"54.00"}]""",
            "F123BB" to """[{"kenteken":"F123BB","brandstof_omschrijving":"Benzine","nettomaximumvermogen":"2.30"}]"""
        )
    }

    /** Every plate the fake RDW was asked about, so a test can check what was sent. */
    private val askedPlates = mutableListOf<String>()

    /**
     * A fake RDW that answers from [FAKE_VEHICLES] and [FAKE_FUEL].
     * Like the real RDW, it answers an unknown plate with an empty list, not with 404.
     */
    private val fakeRdw = MockEngine { request ->
        val plate = request.url.parameters["kenteken"].orEmpty()
        askedPlates.add(plate)

        val isVehicleDataSet = request.url.encodedPath.endsWith("m9d7-ebf2.json")
        val rows = if (isVehicleDataSet) FAKE_VEHICLES[plate] else FAKE_FUEL[plate]
        respond(rows ?: "[]", HttpStatusCode.OK)
    }

    /** Starts the plate routes with an RDW that answers with [engine]. */
    private fun ApplicationTestBuilder.startWith(engine: MockEngine) {
        application {
            configureSerialization()
            routing { plateRoutes(RdwClient(HttpClient(engine))) }
        }
    }

    private suspend fun HttpResponse.errorCode(): String =
        Json.decodeFromString<PlateCheckErrorResponse>(bodyAsText()).code

    @Test
    fun `a found plate returns the vehicle and a prefilled form for car, motorcycle and scooter`() = testApplication {
        startWith(fakeRdw)

        // Car, typed with dashes and small letters
        val carResponse = client.get("/plates/ab-123-c")
        assertEquals(HttpStatusCode.OK, carResponse.status)
        assertEquals(listOf("AB123C", "AB123C"), askedPlates, "RDW must get the cleaned plate, once per data set")
        val car = Json.decodeFromString<PlateCheckResponse>(carResponse.bodyAsText())
        assertEquals(VehicleType.CAR, car.vehicleType)
        assertEquals("VOLKSWAGEN", car.brand)
        assertEquals("GOLF", car.model)
        assertEquals(2019, car.year)
        assertEquals(1498, car.engineCc)
        assertEquals(5, car.doors)
        assertEquals(110.0, car.powerKw)
        assertEquals(CarForm("VOLKSWAGEN", "GOLF", 2019, doors = 5), car.carForm)
        assertNull(car.motorcycleForm, "only the form of the vehicle type is filled")
        assertNull(car.scooterForm, "only the form of the vehicle type is filled")

        // Motorcycle: licence and restriction advice
        val motorcycle = Json.decodeFromString<PlateCheckResponse>(client.get("/plates/MZ-12-AB").bodyAsText())
        assertEquals(VehicleType.MOTORCYCLE, motorcycle.vehicleType)
        assertEquals(LicenseCategory.A, motorcycle.licenseCategory)
        assertEquals(A2Restriction.POSSIBLE, motorcycle.a2Restriction)
        assertEquals(
            MotorcycleForm("YAMAHA", "MT-07", 2019, engineCc = 689, powerKw = 54, licenseCategory = LicenseCategory.A),
            motorcycle.motorcycleForm
        )

        // Scooter
        val scooter = Json.decodeFromString<PlateCheckResponse>(client.get("/plates/F-123-BB").bodyAsText())
        assertEquals(VehicleType.SCOOTER, scooter.vehicleType)
        assertEquals(ScooterForm("PIAGGIO", "ZIP", 2021, maxSpeedKmh = 45), scooter.scooterForm)
    }

    @Test
    fun `an unknown, invalid or unsupported plate gives 404, 400 or 422 with a code`() = testApplication {
        startWith(fakeRdw)

        // Not found
        val notFound = client.get("/plates/ZZ-999-Z")
        assertEquals(HttpStatusCode.NotFound, notFound.status)
        assertEquals("PLATE_NOT_FOUND", notFound.errorCode())

        // Invalid: RDW is not even asked
        askedPlates.clear()
        for (text in listOf("AB-12-C", "ABCDEF", "AB-12!-C", "%20")) {
            val invalid = client.get("/plates/$text")
            assertEquals(HttpStatusCode.BadRequest, invalid.status, text)
            assertEquals("PLATE_INVALID", invalid.errorCode(), text)
        }
        assertTrue(askedPlates.isEmpty(), "an invalid plate must not be sent to RDW")

        // A van is not a car, motorcycle or scooter
        val van = client.get("/plates/VX-123-B")
        assertEquals(HttpStatusCode.UnprocessableEntity, van.status)
        assertEquals(
            PlateCheckErrorResponse("UNSUPPORTED_VEHICLE", listOf("this vehicle is not a car, motorcycle or scooter")),
            Json.decodeFromString<PlateCheckErrorResponse>(van.bodyAsText())
        )
    }

    @Test
    fun `no connection, an RDW error or unreadable data gives 503`() = testApplication {
        val brokenRdw = MockEngine { request ->
            when (request.url.parameters["kenteken"]) {
                "AB123C" -> throw IOException("no internet")
                "MZ12AB" -> respond("server error", HttpStatusCode.InternalServerError)
                "F123BB" -> respond("<html>maintenance</html>", HttpStatusCode.OK)
                else -> respond("""{"error":"not a list"}""", HttpStatusCode.OK)
            }
        }
        startWith(brokenRdw)

        for (plate in listOf("AB-123-C", "MZ-12-AB", "F-123-BB", "VX-123-B")) {
            val response = client.get("/plates/$plate")
            assertEquals(HttpStatusCode.ServiceUnavailable, response.status, plate)
            assertEquals("RDW_UNAVAILABLE", response.errorCode(), plate)
            assertTrue("maintenance" !in response.bodyAsText(), "RDW's own answer must not reach the app")
        }
    }
}
