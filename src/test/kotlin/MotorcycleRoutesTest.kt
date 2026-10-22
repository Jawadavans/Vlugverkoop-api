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

/** Integration tests: start the whole Ktor server and call the motorcycle endpoints over HTTP. */
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
    fun `GET motorcycles returns all seeded motorcycles`() = testApplication {
        configure()

        val response = client.get("/motorcycles")
        val motorcycles = Json.decodeFromString<List<MotorcycleResponse>>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(SEEDED, motorcycles.size)
    }

    @Test
    fun `GET motorcycles by id returns that motorcycle and 404 for an unknown id`() = testApplication {
        configure()

        val response = client.get("/motorcycles/3")

        assertEquals(HttpStatusCode.OK, response.status)
        val motorcycle = Json.decodeFromString<MotorcycleResponse>(response.bodyAsText())
        assertEquals(3, motorcycle.id)
        assertEquals("BMW", motorcycle.brand)

        assertEquals(HttpStatusCode.NotFound, client.get("/motorcycles/99").status)
    }

    @Test
    fun `POST motorcycles creates a motorcycle and rejects a wrong licence with 400`() = testApplication {
        configure()

        val created = client.post("/motorcycles") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.Created, created.status)
        assertEquals(SEEDED + 1, Json.decodeFromString<MotorcycleResponse>(created.bodyAsText()).id)

        val rejected = client.post("/motorcycles") {
            contentType(ContentType.Application.Json)
            setBody(WRONG_LICENCE_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        val errors = Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        assertEquals(listOf("licenseCategory A1 is not allowed for 373 cc and 32 kW"), errors)

        val motorcycles = Json.decodeFromString<List<MotorcycleResponse>>(client.get("/motorcycles").bodyAsText())
        assertEquals(SEEDED + 1, motorcycles.size, "only the valid motorcycle must be saved")
    }

    @Test
    fun `PUT motorcycles replaces a motorcycle, rejects a wrong licence and 404s an unknown id`() = testApplication {
        configure()

        val response = client.put("/motorcycles/1") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val updated = Json.decodeFromString<MotorcycleResponse>(client.get("/motorcycles/1").bodyAsText())
        assertEquals("Triumph", updated.brand)

        val rejected = client.put("/motorcycles/1") {
            contentType(ContentType.Application.Json)
            setBody(TOO_POWERFUL_FOR_A2_BODY)
        }
        assertEquals(HttpStatusCode.BadRequest, rejected.status)
        val errors = Json.decodeFromString<ValidationErrorResponse>(rejected.bodyAsText()).errors
        assertEquals(listOf("licenseCategory A2 is not allowed for 689 cc and 54 kW"), errors)
        val unchanged = Json.decodeFromString<MotorcycleResponse>(client.get("/motorcycles/1").bodyAsText())
        assertEquals("Triumph", unchanged.brand, "an invalid update must not be saved")

        val unknown = client.put("/motorcycles/99") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }
        assertEquals(HttpStatusCode.NotFound, unknown.status)
    }

    @Test
    fun `DELETE motorcycles removes the motorcycle and returns 404 for an unknown id`() = testApplication {
        configure()

        assertEquals(HttpStatusCode.NoContent, client.delete("/motorcycles/1").status)
        val motorcycles = Json.decodeFromString<List<MotorcycleResponse>>(client.get("/motorcycles").bodyAsText())
        assertTrue(motorcycles.none { it.id == 1 })

        assertEquals(HttpStatusCode.NotFound, client.delete("/motorcycles/99").status)
    }
}
