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

        /** The spaces around the brand check that the server removes them. */
        const val VALID_BODY = """
            {"brand":" Peugeot ","model":"Kisbee","year":2022,"price":1750.0,"mileage":2500,"maxSpeedKmh":25}
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
        assertEquals(2.95, vespa.listingFee)

        // GET by id: that scooter, 404 for an id that does not exist, 400 for an id that is not a number
        val byId = Json.decodeFromString<ScooterResponse>(client.get("/scooters/1").bodyAsText())
        assertEquals("Vespa", byId.brand)
        val unknown = client.get("/scooters/99")
        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals(
            listOf("Scooter not found"),
            Json.decodeFromString<ValidationErrorResponse>(unknown.bodyAsText()).errors
        )
        assertEquals(HttpStatusCode.BadRequest, client.get("/scooters/abc").status)

        // GET with filters: only blue plates up to 1500 euro
        val filtered = Json.decodeFromString<List<ScooterResponse>>(
            client.get("/scooters?plateColor=BLUE&maxPrice=1500").bodyAsText()
        )
        assertEquals(listOf(2, 3), filtered.map { it.id })

        // GET with filters that are not valid: 400 with every reason
        val badFilter = client.get("/scooters?plateColor=RED&maxPrice=cheap")
        assertEquals(HttpStatusCode.BadRequest, badFilter.status)
        assertEquals(
            listOf("plateColor must be one of [BLUE, YELLOW]", "maxPrice must be a number"),
            Json.decodeFromString<ValidationErrorResponse>(badFilter.bodyAsText()).errors
        )

        // POST without a JSON body: 400, not a server error
        assertEquals(HttpStatusCode.BadRequest, client.post("/scooters").status)

        // POST with valid data: 201 and the plate colour derived from the speed class
        val created = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val newScooter = Json.decodeFromString<ScooterResponse>(created.bodyAsText())
        assertEquals(SEEDED + 1, newScooter.id)
        assertEquals("Peugeot", newScooter.brand)
        assertEquals(2.95, newScooter.listingFee)
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
        assertEquals(2.95, updated.listingFee)
        assertEquals(PlateColor.BLUE, updated.plateColor)

        val afterPut = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(SEEDED, afterPut.size, "an update must not add a scooter")
        assertEquals("Peugeot", afterPut.first { it.id == 1 }.brand)

        // PUT with an impossible speed class: 400 with the reason, and the scooter stays unchanged
        val rejected = client.put("/scooters/1") {
            contentType(ContentType.Application.Json)
            setBody(INVALID_SPEED_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("maxSpeedKmh must be one of [25, 45], was 80"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
        val unchanged = Json.decodeFromString<ScooterResponse>(client.get("/scooters/1").bodyAsText())
        assertEquals(25, unchanged.maxSpeedKmh, "an invalid update must not be saved")

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

        // DELETE on an id that is not a number: 400
        assertEquals(HttpStatusCode.BadRequest, client.delete("/scooters/abc").status)
    }

    @Test
    fun `every filter works on its own and combined, and invalid filters give 400`() = testApplication {
        configure()

        suspend fun ids(query: String) =
            Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters?$query").bodyAsText()).map { it.id }

        // Search: on brand, model or both, not case sensitive; empty search is ignored
        assertEquals(listOf(2), ids("search=ZIP"))
        assertEquals(listOf(2), ids("search=piaggio%20zip"))
        assertEquals(SEEDED, ids("search=%20").size)
        assertEquals(emptyList(), ids("search=honda"))

        // Choice filter, not case sensitive
        assertEquals(listOf(1, 4, 6), ids("plateColor=yellow"))

        // Limits include the value itself
        assertEquals(listOf(2, 3), ids("maxPrice=1395"))
        assertEquals(listOf(4, 5), ids("maxMileage=2500"))

        // Combined: a scooter must pass every filter
        assertEquals(listOf(4), ids("plateColor=YELLOW&maxPrice=2000&maxMileage=5000"))

        // The plate colours for the dropdown
        assertEquals("""["BLUE","YELLOW"]""", client.get("/scooters/plate-colors").bodyAsText())

        // Invalid values: 400 with every reason
        val rejected = client.get("/scooters?plateColor=RED&maxMileage=far")
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("plateColor must be one of [BLUE, YELLOW]", "maxMileage must be a whole number"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
    }
}
