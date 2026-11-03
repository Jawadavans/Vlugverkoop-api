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
import nl.vlugverkoop.dto.CarResponse
import nl.vlugverkoop.dto.ValidationErrorResponse
import kotlin.test.*

/**
 * Integration tests: the real routes, repository, model and DTOs together.
 *
 * The two tests are grouped by what they prove rather than by endpoint, so that
 * all four CRUD requests are covered: reading and filtering plus creating in the
 * first test, replacing and deleting in the second.
 */
class CarRoutesTest {

    private companion object {
        /** How many cars the repository starts with. */
        const val SEEDED = 6

        /** The spaces around the brand check that the server removes them. */
        const val VALID_BODY = """
            {"brand":" Audi ","model":"A3","year":2019,"price":17500.0,"mileage":60000,"doors":5}
        """

        const val INVALID_BODY = """
            {"brand":"Audi","model":"A3","year":2019,"price":-1.0,"mileage":60000,"doors":9}
        """
    }

    @Test
    fun `GET returns and filters cars, and POST only stores valid ones`() = testApplication {
        configure()

        // GET: all cars, with their details
        val all = client.get("/cars")
        assertEquals(HttpStatusCode.OK, all.status)
        assertEquals(ContentType.Application.Json, all.contentType()?.withoutParameters())

        val cars = Json.decodeFromString<List<CarResponse>>(all.bodyAsText())
        assertEquals(SEEDED, cars.size)
        val golf = cars.first { it.id == 1 }
        assertEquals("Volkswagen", golf.brand)
        assertEquals(14500.0, golf.price)
        assertEquals(9.95, golf.listingFee)

        // GET by id: that car, 404 for an id that does not exist, 400 for an id that is not a number
        val byId = Json.decodeFromString<CarResponse>(client.get("/cars/1").bodyAsText())
        assertEquals("Volkswagen", byId.brand)
        val unknown = client.get("/cars/99")
        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals(
            listOf("Car not found"),
            Json.decodeFromString<ValidationErrorResponse>(unknown.bodyAsText()).errors
        )
        assertEquals(HttpStatusCode.BadRequest, client.get("/cars/abc").status)

        // GET with filters: only Volkswagens up to 15000 euro
        val filtered = Json.decodeFromString<List<CarResponse>>(
            client.get("/cars?search=volkswagen&maxPrice=15000").bodyAsText()
        )
        assertEquals(listOf(1), filtered.map { it.id })

        // GET with a filter that is not valid: 400 with the reason
        val badFilter = client.get("/cars?maxPrice=cheap")
        assertEquals(HttpStatusCode.BadRequest, badFilter.status)
        assertEquals(
            listOf("maxPrice must be a number"),
            Json.decodeFromString<ValidationErrorResponse>(badFilter.bodyAsText()).errors
        )

        // POST without a JSON body: 400, not a server error
        assertEquals(HttpStatusCode.BadRequest, client.post("/cars").status)

        // POST with valid data: 201 and a new id
        val created = client.post("/cars") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val newCar = Json.decodeFromString<CarResponse>(created.bodyAsText())
        assertEquals(SEEDED + 1, newCar.id)
        assertEquals("Audi", newCar.brand)
        assertEquals(9.95, newCar.listingFee)

        // POST with invalid data: 400 with every reason, and nothing is stored
        val rejected = client.post("/cars") {
            contentType(ContentType.Application.Json)
            setBody(INVALID_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("price must be greater than 0", "doors must be between 2 and 7"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
        val afterPost = Json.decodeFromString<List<CarResponse>>(client.get("/cars").bodyAsText())
        assertEquals(SEEDED + 1, afterPost.size, "an invalid car must not be saved")
    }

    @Test
    fun `PUT replaces and DELETE removes a car, both giving 404 for an unknown id`() = testApplication {
        configure()

        // PUT on an existing car: 200, replaced in place, no extra listing
        val replaced = client.put("/cars/1") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.OK, replaced.status)
        val updated = Json.decodeFromString<CarResponse>(replaced.bodyAsText())
        assertEquals(1, updated.id)
        assertEquals("Audi", updated.brand)
        assertEquals(9.95, updated.listingFee)

        val afterPut = Json.decodeFromString<List<CarResponse>>(client.get("/cars").bodyAsText())
        assertEquals(SEEDED, afterPut.size, "an update must not add a car")

        // PUT with invalid data: 400 with every reason, and the car stays unchanged
        val rejected = client.put("/cars/1") {
            contentType(ContentType.Application.Json)
            setBody(INVALID_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("price must be greater than 0", "doors must be between 2 and 7"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
        val unchanged = Json.decodeFromString<CarResponse>(client.get("/cars/1").bodyAsText())
        assertEquals(17500.0, unchanged.price, "an invalid update must not be saved")

        // PUT on an id that does not exist: 404
        val unknownPut = client.put("/cars/99") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.NotFound, unknownPut.status)

        // DELETE on an existing car: 204 and really gone
        assertEquals(HttpStatusCode.NoContent, client.delete("/cars/1").status)
        val afterDelete = Json.decodeFromString<List<CarResponse>>(client.get("/cars").bodyAsText())
        assertTrue(afterDelete.none { it.id == 1 }, "the car should be gone")
        assertEquals(SEEDED - 1, afterDelete.size)

        // DELETE on an id that does not exist: 404
        assertEquals(HttpStatusCode.NotFound, client.delete("/cars/99").status)

        // DELETE on an id that is not a number: 400
        assertEquals(HttpStatusCode.BadRequest, client.delete("/cars/abc").status)
    }

    @Test
    fun `every filter works on its own and combined, and invalid filters give 400`() = testApplication {
        configure()

        suspend fun ids(query: String) =
            Json.decodeFromString<List<CarResponse>>(client.get("/cars?$query").bodyAsText()).map { it.id }

        // Search: on brand, model or both, not case sensitive; empty search is ignored
        assertEquals(listOf(1), ids("search=GOLF"))
        assertEquals(listOf(1), ids("search=volkswagen%20golf"))
        assertEquals(SEEDED, ids("search=%20").size)
        assertEquals(emptyList(), ids("search=ferrari"))

        // Limits include the value itself
        assertEquals(listOf(1, 5), ids("maxPrice=14500"))
        assertEquals(listOf(2, 4), ids("maxMileage=42000"))
        assertEquals(listOf(3, 6), ids("doors=4"))

        // Combined: a car must pass every filter
        assertEquals(listOf(1, 2), ids("maxPrice=17000&doors=5"))

        // Invalid values: 400 with every reason
        val rejected = client.get("/cars?maxMileage=far&doors=many")
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        assertEquals(
            listOf("maxMileage must be a whole number", "doors must be a whole number"),
            Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        )
    }
}
