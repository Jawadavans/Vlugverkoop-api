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

        const val VALID_BODY = """
            {"brand":"Triumph","model":"Trident 660","year":2022,"price":7250.0,"mileage":3000,
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

        // GET by id: that motorcycle, or 404 for an id that does not exist
        val byId = Json.decodeFromString<MotorcycleResponse>(client.get("/motorcycles/3").bodyAsText())
        assertEquals("BMW", byId.brand)
        assertEquals(HttpStatusCode.NotFound, client.get("/motorcycles/99").status)

        // GET with filters: everything an A2 rider may ride (A1 and A2) up to 6000 euro
        val filtered = Json.decodeFromString<List<MotorcycleResponse>>(
            client.get("/motorcycles?license=A2&maxPrice=6000").bodyAsText()
        )
        assertEquals(listOf(2, 4), filtered.map { it.id })

        // POST with valid data: 201 and a new id
        val created = client.post("/motorcycles") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val newMotorcycle = Json.decodeFromString<MotorcycleResponse>(created.bodyAsText())
        assertEquals(SEEDED + 1, newMotorcycle.id)
        assertEquals("Triumph", newMotorcycle.brand)

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
    }
}
