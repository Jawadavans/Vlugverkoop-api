package nl.vlugverkoop

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.vlugverkoop.rdw.RdwClient
import nl.vlugverkoop.repository.CarRepository
import nl.vlugverkoop.repository.MotorcycleRepository
import nl.vlugverkoop.repository.ScooterRepository
import nl.vlugverkoop.routes.carRoutes
import nl.vlugverkoop.routes.motorcycleRoutes
import nl.vlugverkoop.routes.plateRoutes
import nl.vlugverkoop.routes.scooterRoutes

fun Application.configureRouting() {
    // One HTTP client for all RDW lookups; closed when the server stops.
    val rdwClient = RdwClient.create()
    monitor.subscribe(ApplicationStopped) { rdwClient.close() }

    routing {
        carRoutes(CarRepository())
        scooterRoutes(ScooterRepository())
        motorcycleRoutes(MotorcycleRepository())
        plateRoutes(rdwClient)
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}
