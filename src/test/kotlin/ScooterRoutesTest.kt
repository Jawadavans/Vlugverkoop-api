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

/** Integration tests: the real routes, repository, model and DTOs together. */
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

        const val NEGATIVE_PRICE_BODY = """
            {"brand":"Peugeot","model":"Kisbee","year":2022,"price":-1.0,"mileage":2500,"maxSpeedKmh":25}
        """
    }

    @Test
    fun `GET scooters returns 200 with JSON`() = testApplication {
        configure()

        val response = client.get("/scooters")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
    }

    @Test
    fun `GET scooters returns all scooters with their details`() = testApplication {
        configure()

        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())

        assertEquals(SEEDED, scooters.size)
        val vespa = scooters.first { it.id == 1 }
        assertEquals("Vespa", vespa.brand)
        assertEquals("Primavera", vespa.model)
        assertEquals(2650.0, vespa.price)
        assertEquals(45, vespa.maxSpeedKmh)
    }

    @Test
    fun `GET scooters gives blue plate for 25 kmh and yellow plate for 45 kmh`() = testApplication {
        configure()

        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())

        scooters.forEach { scooter ->
            val expected = if (scooter.maxSpeedKmh == 25) PlateColor.BLUE else PlateColor.YELLOW
            assertEquals(expected, scooter.plateColor, "Wrong plate colour for scooter ${scooter.id}")
        }
    }

    @Test
    fun `GET scooters filters on plate colour and maximum price`() = testApplication {
        configure()

        val body = client.get("/scooters?plateColor=BLUE&maxPrice=1500").bodyAsText()
        val scooters = Json.decodeFromString<List<ScooterResponse>>(body)

        assertTrue(scooters.isNotEmpty(), "filter should still return scooters")
        assertTrue(scooters.all { it.plateColor == PlateColor.BLUE && it.price <= 1500.0 }, "wrong result: $scooters")
    }

    @Test
    fun `GET scooters filters on maximum mileage`() = testApplication {
        configure()

        val body = client.get("/scooters?maxMileage=2500").bodyAsText()
        val scooters = Json.decodeFromString<List<ScooterResponse>>(body)

        // Scooter 4 has exactly 2500 km, so the limit itself must be included
        assertEquals(listOf(4, 5), scooters.map { it.id })
    }

    @Test
    fun `GET scooters by id returns that scooter`() = testApplication {
        configure()

        val response = client.get("/scooters/2")

        assertEquals(HttpStatusCode.OK, response.status)
        val scooter = Json.decodeFromString<ScooterResponse>(response.bodyAsText())
        assertEquals(2, scooter.id)
        assertEquals("Piaggio", scooter.brand)
        assertEquals("Zip", scooter.model)
        assertEquals(PlateColor.BLUE, scooter.plateColor)
    }

    @Test
    fun `GET scooters by id returns 404 for an unknown id`() = testApplication {
        configure()

        assertEquals(HttpStatusCode.NotFound, client.get("/scooters/99").status)
    }

    @Test
    fun `GET scooters plate-colors returns BLUE and YELLOW`() = testApplication {
        configure()

        val response = client.get("/scooters/plate-colors")

        assertEquals(HttpStatusCode.OK, response.status)
        val plateColors = Json.decodeFromString<List<PlateColor>>(response.bodyAsText())
        assertEquals(listOf(PlateColor.BLUE, PlateColor.YELLOW), plateColors)
    }

    @Test
    fun `POST scooters creates a scooter with the right plate colour`() = testApplication {
        configure()

        val response = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val scooter = Json.decodeFromString<ScooterResponse>(response.bodyAsText())
        assertEquals(SEEDED + 1, scooter.id)
        assertEquals("Peugeot", scooter.brand)
        assertEquals(PlateColor.BLUE, scooter.plateColor)
    }

    @Test
    fun `POST scooters with a speed other than 25 or 45 returns 400 with the reason`() = testApplication {
        configure()

        val response = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody(INVALID_SPEED_BODY)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errors = Json.decodeFromString<ValidationErrorResponse>(response.bodyAsText()).errors
        assertEquals(listOf("maxSpeedKmh must be one of [25, 45], was 80"), errors)
        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(SEEDED, scooters.size, "Invalid scooter must not be saved")
    }

    @Test
    fun `POST scooters with a negative price returns 400 with the reason`() = testApplication {
        configure()

        val response = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody(NEGATIVE_PRICE_BODY)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errors = Json.decodeFromString<ValidationErrorResponse>(response.bodyAsText()).errors
        assertEquals(listOf("price must be greater than 0"), errors)
        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(SEEDED, scooters.size, "Invalid scooter must not be saved")
    }

    @Test
    fun `PUT scooters replaces an existing scooter`() = testApplication {
        configure()

        val response = client.put("/scooters/1") {
            contentType(ContentType.Application.Json)
            setBody(VALID_BODY)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = Json.decodeFromString<ScooterResponse>(response.bodyAsText())
        assertEquals(1, updated.id)
        assertEquals("Peugeot", updated.brand)
        assertEquals(PlateColor.BLUE, updated.plateColor)

        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(SEEDED, scooters.size, "an update must not add a scooter")
        assertEquals("Peugeot", scooters.first { it.id == 1 }.brand)
    }

    @Test
    fun `PUT scooters returns 404 for an unknown id before looking at the body`() = testApplication {
        configure()

        // The body is invalid on purpose: if it were checked first, the answer would be 400
        listOf("/scooters/99", "/scooters/abc").forEach { path ->
            val response = client.put(path) {
                contentType(ContentType.Application.Json)
                setBody(NEGATIVE_PRICE_BODY)
            }

            assertEquals(HttpStatusCode.NotFound, response.status, "wrong status for $path")
            assertEquals("", response.bodyAsText(), "a 404 for $path must have an empty body")
        }
    }

    @Test
    fun `DELETE scooters removes the scooter and returns 404 for an unknown id`() = testApplication {
        configure()

        assertEquals(HttpStatusCode.NoContent, client.delete("/scooters/1").status)
        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertTrue(scooters.none { it.id == 1 })

        assertEquals(HttpStatusCode.NotFound, client.delete("/scooters/99").status)
    }
}
