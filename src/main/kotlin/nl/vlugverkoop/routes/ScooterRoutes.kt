package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.CreateScooterRequest
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

        // POST /scooters → creates a new scooter (speed must be 25 or 45 km/h)
        post {
            val request = call.receive<CreateScooterRequest>()
            val scooter = try {
                repository.add(
                    brand = request.brand,
                    model = request.model,
                    year = request.year,
                    price = request.price,
                    mileage = request.mileage,
                    maxSpeedKmh = request.maxSpeedKmh
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid scooter")
                return@post
            }
            call.respond(HttpStatusCode.Created, scooter.toResponse())
        }

        // DELETE /scooters/{id} → deletes one scooter
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Scooter not found")
            }
        }
    }
}
