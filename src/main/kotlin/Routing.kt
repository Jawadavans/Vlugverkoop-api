package nl.vlugverkoop

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.vlugverkoop.repository.CarRepository
import nl.vlugverkoop.routes.carRoutes

fun Application.configureRouting() {
    routing {
        carRoutes(CarRepository())
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}