package nl.vlugverkoop

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        // loads default configuration
        configure()
        // verify server root returns 200
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun `users endpoints give 400 for an invalid id and 404 for an unknown id`() = testApplication {
        configure()

        assertEquals(HttpStatusCode.BadRequest, client.get("/users/abc").status)
        assertEquals(HttpStatusCode.BadRequest, client.delete("/users/abc").status)
        assertEquals(HttpStatusCode.BadRequest, client.post("/users").status)

        assertEquals(HttpStatusCode.NotFound, client.get("/users/999999").status)
        assertEquals(HttpStatusCode.NotFound, client.delete("/users/999999").status)
        val unknownPut = client.put("/users/999999") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Test","age":30}""")
        }
        assertEquals(HttpStatusCode.NotFound, unknownPut.status)
    }
}
