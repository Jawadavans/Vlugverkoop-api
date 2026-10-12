package nl.vlugverkoop

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.vlugverkoop.dto.ScooterResponse
import nl.vlugverkoop.model.PlateColor
import kotlin.test.*

class ScooterRoutesTest {

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

        assertEquals(3, scooters.size)
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
    fun `POST scooters creates a scooter with the right plate colour`() = testApplication {
        configure()

        val response = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody("""{"brand":"Peugeot","model":"Kisbee","year":2022,"price":1750.0,"mileage":2500,"maxSpeedKmh":25}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val scooter = Json.decodeFromString<ScooterResponse>(response.bodyAsText())
        assertEquals(4, scooter.id)
        assertEquals("Peugeot", scooter.brand)
        assertEquals(PlateColor.BLUE, scooter.plateColor)
    }

    @Test
    fun `POST scooters with a speed other than 25 or 45 returns 400`() = testApplication {
        configure()

        val response = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody("""{"brand":"Peugeot","model":"Kisbee","year":2022,"price":1750.0,"mileage":2500,"maxSpeedKmh":80}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(3, scooters.size, "Invalid scooter must not be saved")
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
