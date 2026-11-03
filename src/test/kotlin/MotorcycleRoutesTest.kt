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
import nl.vlugverkoop.dto.MotorcycleResponse
import nl.vlugverkoop.dto.ValidationErrorResponse
import kotlin.test.*

/**
 * Integration tests: the real routes, repository, model and DTOs together.
 *
 * The two tests are grouped by what they prove rather than by endpoint, so that
 * all four CRUD requests are covered: reading and filtering plus creating in the
 * first test, replacing and deleting in the second.
 */
class MotorcycleRoutesTest {

    private companion object {
        /** How many motorcycles the repository starts with. */
        const val SEEDED = 6

        /** The spaces around the brand check that the server removes them. */
        const val VALID_BODY = """
            {"brand":" Triumph ","model":"Trident 660","year":2022,"price":7250.0,"mileage":3000,
             "engineCc":660,"powerKw":35,"style":"NAKED","licenseCategory":"A2"}
        """

        const val WRONG_LICENCE_BODY = """
            {"brand":"KTM","model":"390 Duke","year":2022,"price":4750.0,"mileage":5100,
             "engineCc":373,"powerKw":32,"style":"NAKED","licenseCategory":"A1"}
        """

        const val TOO_POWERFUL_FOR_A2_BODY = """
            {"brand":"Yamaha","model":"MT-07","year":2019,"price":6450.0,"mileage":18200,
             "engineCc":689,"powerKw":54,"style":"NAKED","licenseCategory":"A2"}
        """
    }

    @Test
    fun `GET returns and filters motorcycles, and POST only stores valid ones`() = testApplication {
        configure()

        // GET: all motorcycles, with their details
        val all = client.get("/motorcycles")
        assertEquals(HttpStatusCode.OK, all.status)
        assertEquals(ContentType.Application.Json, all.contentType()?.withoutParameters())

        val motorcycles = Json.decodeFromString<List<MotorcycleResponse>>(all.bodyAsText())
        assertEquals(SEEDED, motorcycles.size)
        val bmw = motorcycles.first { it.id == 3 }
        assertEquals("BMW", bmw.brand)
        assertEquals(15900.0, bmw.price)
        assertEquals(4.95, bmw.listingFee)

        // GET by id: that motorcycle, 404 for an id that does not exist, 400 for an id that is not a number
        val byId = Json.decodeFromString<MotorcycleResponse>(client.get("/motorcycles/3").bodyAsText())
        assertEquals("BMW", byId.brand)
        val unknown = client.get("/motorcycles/99")
        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals(
            listOf("Motorcycle not found"),
            Json.decodeFromString<ValidationErrorResponse>(unknown.bodyAsText()).errors
        )
        assertEquals(HttpStatusCode.BadRequest, client.get("/motorcycles/abc").status)

        // GET with filters: everything an A2 rider may ride (A1 and A2) up to 6000 euro
        val filtered = Json.decodeFromString<List<MotorcycleResponse>>(
            client.get("/motorcycles?license=A2&maxPrice=6000").bodyAsText()
        )
        assertEquals(listOf(2, 4), filtered.map { it.id })

        // GET with filters that are not valid: 400 with every reason
        val badFilter = client.get("/motorcycles?license=B&maxPrice=cheap")
        assertEquals(HttpStatusCode.BadRequest, badFilter.status)
        assertEquals(
            listOf("license must be one of [A1, A2, A]", "maxPrice must be a number"),
            Json.decodeFromString<ValidationErrorResponse>(badFilter.bodyAsText()).errors
        )

        // POST without a JSON body: 400, not a server error
        assertEquals(HttpStatusCode.BadRequest, client.post("/motorcycles").status)

        // POST with valid data: 201 and a new id
        val created = client.post("/motorcycles") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val newMotorcycle = Json.decodeFromString<MotorcycleResponse>(created.bodyAsText())
        assertEquals(SEEDED + 1, newMotorcycle.id)
        assertEquals("Triumph", newMotorcycle.brand)
        assertEquals(4.95, newMotorcycle.listingFee)

        // POST with a licence that is too low: 400 with the reason, and nothing is stored
        val rejected = client.post("/motorcycles") {
            contentType(ContentType.Application.Json)
            setBody(WRONG_LICENCE_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("licenseCategory A1 is not allowed for 373 cc and 32 kW"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
        val afterPost = Json.decodeFromString<List<MotorcycleResponse>>(client.get("/motorcycles").bodyAsText())
        assertEquals(SEEDED + 1, afterPost.size, "an invalid motorcycle must not be saved")
    }

    @Test
    fun `PUT replaces and DELETE removes a motorcycle, both giving 404 for an unknown id`() = testApplication {
        configure()

        // PUT on an existing motorcycle: 200, replaced in place, no extra listing
        val replaced = client.put("/motorcycles/1") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.OK, replaced.status)
        val updated = Json.decodeFromString<MotorcycleResponse>(replaced.bodyAsText())
        assertEquals(1, updated.id)
        assertEquals("Triumph", updated.brand)
        assertEquals(4.95, updated.listingFee)

        val afterPut = Json.decodeFromString<List<MotorcycleResponse>>(client.get("/motorcycles").bodyAsText())
        assertEquals(SEEDED, afterPut.size, "an update must not add a motorcycle")

        // PUT with a licence that is too low: 400 with the reason, and the motorcycle stays unchanged
        val rejected = client.put("/motorcycles/1") {
            contentType(ContentType.Application.Json)
            setBody(TOO_POWERFUL_FOR_A2_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("licenseCategory A2 is not allowed for 689 cc and 54 kW"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
        val unchanged = Json.decodeFromString<MotorcycleResponse>(client.get("/motorcycles/1").bodyAsText())
        assertEquals("Triumph", unchanged.brand, "an invalid update must not be saved")

        // PUT on an id that does not exist: 404
        val unknownPut = client.put("/motorcycles/99") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.NotFound, unknownPut.status)

        // DELETE on an existing motorcycle: 204 and really gone
        assertEquals(HttpStatusCode.NoContent, client.delete("/motorcycles/1").status)
        val afterDelete = Json.decodeFromString<List<MotorcycleResponse>>(client.get("/motorcycles").bodyAsText())
        assertTrue(afterDelete.none { it.id == 1 }, "the motorcycle should be gone")
        assertEquals(SEEDED - 1, afterDelete.size)

        // DELETE on an id that does not exist: 404
        assertEquals(HttpStatusCode.NotFound, client.delete("/motorcycles/99").status)

        // DELETE on an id that is not a number: 400
        assertEquals(HttpStatusCode.BadRequest, client.delete("/motorcycles/abc").status)
    }

    @Test
    fun `every filter works on its own and combined, and invalid filters give 400`() = testApplication {
        configure()

        suspend fun ids(query: String) =
            Json.decodeFromString<List<MotorcycleResponse>>(client.get("/motorcycles?$query").bodyAsText()).map { it.id }

        // Search: on brand, model or both, not case sensitive; empty search is ignored
        assertEquals(listOf(4), ids("search=NINJA"))
        assertEquals(listOf(4), ids("search=kawasaki%20ninja"))
        assertEquals(SEEDED, ids("search=%20").size)
        assertEquals(emptyList(), ids("search=ducati"))

        // Choice filters, not case sensitive: a licence also allows every lower category
        assertEquals(listOf(1, 2), ids("style=naked"))
        assertEquals(listOf(2), ids("license=A1"))
        assertEquals(SEEDED, ids("license=a").size)

        // Limits include the value itself
        assertEquals(listOf(3, 6), ids("minCc=1254"))
        assertEquals(listOf(2), ids("maxCc=125"))
        assertEquals(listOf(2), ids("maxPrice=3950"))
        assertEquals(listOf(2), ids("maxMileage=6400"))

        // Combined: a motorcycle must pass every filter
        assertEquals(listOf(1), ids("style=NAKED&license=A2&minCc=300&maxCc=700&maxPrice=8000&maxMileage=20000"))

        // The riding styles for the dropdown
        assertEquals(
            """["NAKED","TOURING","SPORT","ENDURO","CHOPPER"]""",
            client.get("/motorcycles/styles").bodyAsText()
        )

        // Invalid values: 400 with every reason
        val rejected = client.get("/motorcycles?style=CRUISER&minCc=small")
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("style must be one of [NAKED, TOURING, SPORT, ENDURO, CHOPPER]", "minCc must be a whole number"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
    }
}
