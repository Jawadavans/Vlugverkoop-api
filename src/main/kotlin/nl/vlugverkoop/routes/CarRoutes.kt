package nl.vlugverkoop.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.repository.CarRepository

/**
 * All endpoints for cars.
 */
fun Route.carRoutes(repository: CarRepository) {
    route("/cars") {

        // GET /cars → returns all cars as JSON
        get {
            val cars = repository.getAll().map { it.toResponse() }
            call.respond(cars)
        }
    }
}
