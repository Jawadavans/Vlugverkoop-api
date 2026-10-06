package nl.vlugverkoop.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.repository.ScooterRepository

/**
 * All endpoints for scooters.
 */
fun Route.scooterRoutes(repository: ScooterRepository) {
    route("/scooters") {

        // GET /scooters → returns all scooters as JSON
        get {
            val scooters = repository.getAll().map { it.toResponse() }
            call.respond(scooters)
        }
    }
}
