package nl.vlugverkoop

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.vlugverkoop.dto.ScooterResponse
import nl.vlugverkoop.dto.ValidationErrorResponse
import nl.vlugverkoop.model.PlateColor
import kotlin.test.*

/**
 * Integration tests: the real routes, repository, model and DTOs together.
 *
 * The two tests are grouped by what they prove rather than by endpoint, so that
 * all four CRUD requests are covered: reading and filtering plus creating in the
 * first test, replacing and deleting in the second.
 */
class ScooterRoutesTest {

    private companion object {
        /** How many scooters the repository starts with. */
        const val SEEDED = 6

        const val VALID_BODY = """
            {"brand":"Peugeot","model":"Kisbee","year":2022,"price":1750.0,"mileage":2500,"maxSpeedKmh":25}
        """

        const val INVALID_SPEED_BODY = """
            {"brand":"Peugeot","model":"Kisbee","year":2022,"price":1750.0,"mileage":2500,"maxSpeedKmh":80}
        """
    }

    @Test
    fun `GET returns and filters scooters, and POST only stores valid ones`() = testApplication {
        configure()

        // GET: all scooters, with their details and the derived plate colour
        val all = client.get("/scooters")
        assertEquals(HttpStatusCode.OK, all.status)
        assertEquals(ContentType.Application.Json, all.contentType()?.withoutParameters())

        val scooters = Json.decodeFromString<List<ScooterResponse>>(all.bodyAsText())
        assertEquals(SEEDED, scooters.size)
        val vespa = scooters.first { it.id == 1 }
        assertEquals("Vespa", vespa.brand)
        assertEquals(2650.0, vespa.price)
        assertEquals(PlateColor.YELLOW, vespa.plateColor)

        // GET with filters: only blue plates up to 1500 euro
        val filtered = Json.decodeFromString<List<ScooterResponse>>(
            client.get("/scooters?plateColor=BLUE&maxPrice=1500").bodyAsText()
        )
        assertTrue(filtered.isNotEmpty(), "the filter should still return scooters")
        assertTrue(
            filtered.all { it.plateColor == PlateColor.BLUE && it.price <= 1500.0 },
            "wrong result: $filtered"
        )

        // POST with valid data: 201 and the plate colour derived from the speed class
        val created = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val newScooter = Json.decodeFromString<ScooterResponse>(created.bodyAsText())
        assertEquals(SEEDED + 1, newScooter.id)
        assertEquals("Peugeot", newScooter.brand)
        assertEquals(PlateColor.BLUE, newScooter.plateColor)

        // POST with an impossible speed class: 400 with the reason, and nothing is stored
        val rejected = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody(INVALID_SPEED_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("maxSpeedKmh must be one of [25, 45], was 80"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
        val afterPost = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(SEEDED + 1, afterPost.size, "an invalid scooter must not be saved")
    }

    @Test
    fun `PUT replaces and DELETE removes a scooter, both giving 404 for an unknown id`() = testApplication {
        configure()

        // PUT on an existing scooter: 200, replaced in place, no extra listing
        val replaced = client.put("/scooters/1") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.OK, replaced.status)
        val updated = Json.decodeFromString<ScooterResponse>(replaced.bodyAsText())
        assertEquals(1, updated.id)
        assertEquals("Peugeot", updated.brand)
        assertEquals(PlateColor.BLUE, updated.plateColor)

        val afterPut = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(SEEDED, afterPut.size, "an update must not add a scooter")
        assertEquals("Peugeot", afterPut.first { it.id == 1 }.brand)

        // PUT on an id that does not exist: 404
        val unknownPut = client.put("/scooters/99") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.NotFound, unknownPut.status)

        // DELETE on an existing scooter: 204 and really gone
        assertEquals(HttpStatusCode.NoContent, client.delete("/scooters/1").status)
        val afterDelete = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertTrue(afterDelete.none { it.id == 1 }, "the scooter should be gone")
        assertEquals(SEEDED - 1, afterDelete.size)

        // DELETE on an id that does not exist: 404
        assertEquals(HttpStatusCode.NotFound, client.delete("/scooters/99").status)
    }
}
