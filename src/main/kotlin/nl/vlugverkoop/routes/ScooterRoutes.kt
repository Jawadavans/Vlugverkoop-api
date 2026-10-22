package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.CreateScooterRequest
import nl.vlugverkoop.dto.ValidationErrorResponse
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.dto.toScooter
import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.Scooter
import nl.vlugverkoop.repository.ScooterRepository

/**
 * Builds a scooter from this request and collects every problem with it.
 *
 * The scooter is null when the speed class makes a scooter impossible to build at all;
 * in that case the message from the constructor is the only error.
 */
private fun CreateScooterRequest.validated(id: Int): Pair<Scooter?, List<String>> {
    val scooter = try {
        toScooter(id)
    } catch (e: IllegalArgumentException) {
        return null to listOf(e.message ?: "invalid scooter data")
    }
    return scooter to scooter.validate()
}

/**
 * All endpoints for scooters.
 */
fun Route.scooterRoutes(repository: ScooterRepository) {
    route("/scooters") {

        // GET /scooters → all scooters as JSON, optionally filtered
        // Example: /scooters?search=vespa&plateColor=YELLOW&maxPrice=3000&maxMileage=5000
        get {
            val query = call.request.queryParameters
            val scooters = repository.find(
                search = query["search"],
                plateColor = PlateColor.entries.find { it.name.equals(query["plateColor"], ignoreCase = true) },
                maxPrice = query["maxPrice"]?.toDoubleOrNull(),
                maxMileage = query["maxMileage"]?.toIntOrNull()
            )
            call.respond(scooters.map { it.toResponse() })
        }

        // GET /scooters/plate-colors → the plate colours, for the dropdown in the app
        get("/plate-colors") {
            call.respond(PlateColor.entries.toList())
        }

        // GET /scooters/{id} → one scooter, or 404 when it does not exist
        get("/{id}") {
            val scooter = call.parameters["id"]?.toIntOrNull()?.let { repository.getById(it) }
            if (scooter == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(scooter.toResponse())
            }
        }

        // POST /scooters → places a new listing, or 400 with the reasons when the data is invalid
        post {
            val request = try {
                call.receive<CreateScooterRequest>()
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf("invalid scooter data")))
                return@post
            }

            val (scooter, errors) = request.validated(repository.nextId())
            if (scooter == null || errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@post
            }

            repository.add(scooter)
            call.respond(HttpStatusCode.Created, scooter.toResponse())
        }

        // PUT /scooters/{id} → replaces an existing listing, for example after a price change,
        // or 404 when it does not exist
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null || repository.getById(id) == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            val request = try {
                call.receive<CreateScooterRequest>()
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf("invalid scooter data")))
                return@put
            }

            val (scooter, errors) = request.validated(id)
            if (scooter == null || errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@put
            }

            if (repository.update(scooter)) {
                call.respond(HttpStatusCode.OK, scooter.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // DELETE /scooters/{id} → removes a listing, or 404 when it does not exist
        delete("/{id}") {
            val deleted = call.parameters["id"]?.toIntOrNull()?.let { repository.delete(it) } ?: false
            call.respond(if (deleted) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
        }
    }
}
