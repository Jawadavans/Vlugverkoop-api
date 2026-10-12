package nl.vlugverkoop

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.vlugverkoop.dto.MotorcycleResponse
import nl.vlugverkoop.dto.ValidationErrorResponse
import kotlin.test.Test
import kotlin.test.assertEquals

/** Integration tests: start the whole Ktor server and call the motorcycle endpoints over HTTP. */
class MotorcycleRoutesTest {

    @Test
    fun `GET motorcycles returns all seeded motorcycles`() = testApplication {
        configure()

        val response = client.get("/motorcycles")
        val motorcycles = Json.decodeFromString<List<MotorcycleResponse>>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(6, motorcycles.size)
    }

    @Test
    fun `POST motorcycle with wrong licence returns 400 with the reason`() = testApplication {
        configure()

        val response = client.post("/motorcycles") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "brand": "KTM",
                  "model": "390 Duke",
                  "year": 2022,
                  "price": 4750.0,
                  "mileage": 5100,
                  "engineCc": 373,
                  "powerKw": 32,
                  "style": "NAKED",
                  "licenseCategory": "A1"
                }
                """.trimIndent()
            )
        }
        val body = Json.decodeFromString<ValidationErrorResponse>(response.bodyAsText())

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(listOf("licenseCategory A1 is not allowed for 373 cc and 32 kW"), body.errors)
    }
}
