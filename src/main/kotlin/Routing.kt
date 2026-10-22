package nl.vlugverkoop

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.vlugverkoop.repository.CarRepository
import nl.vlugverkoop.repository.MotorcycleRepository
import nl.vlugverkoop.repository.ScooterRepository
import nl.vlugverkoop.routes.carRoutes
import nl.vlugverkoop.routes.motorcycleRoutes
import nl.vlugverkoop.routes.scooterRoutes

fun Application.configureRouting() {
    routing {
        carRoutes(CarRepository())
        scooterRoutes(ScooterRepository())
        motorcycleRoutes(MotorcycleRepository())
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}
