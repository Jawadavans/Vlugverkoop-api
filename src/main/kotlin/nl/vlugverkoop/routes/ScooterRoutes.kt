package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
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

private const val INVALID_BODY = "request body must be valid scooter JSON"
private const val SCOOTER_NOT_FOUND = "Scooter not found"

/**
 * All endpoints for scooters.
 *
 * Error handling:
 * - 404 Not Found: the scooter does not exist (body is a [ValidationErrorResponse]).
 * - 400 Bad Request: the data in the request is not correct (body is a [ValidationErrorResponse]).
 */
fun Route.scooterRoutes(repository: ScooterRepository) {
    route("/scooters") {

        // GET /scooters → all scooters, optionally filtered
        // Example: /scooters?search=vespa&plateColor=YELLOW&maxPrice=3000&maxMileage=5000
        get {
            val query = call.request.queryParameters
            val errors = mutableListOf<String>()
            val plateColor = query.enumOrError<PlateColor>("plateColor", errors)
            val maxPrice = query.doubleOrError("maxPrice", errors)
            val maxMileage = query.intOrError("maxMileage", errors)

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@get
            }

            val scooters = repository.find(
                search = query["search"],
                plateColor = plateColor,
                maxPrice = maxPrice,
                maxMileage = maxMileage
            )
            call.respond(scooters.map { it.toResponse() })
        }

        // GET /scooters/plate-colors → the plate colours, for the dropdown in the app
        get("/plate-colors") {
            call.respond(PlateColor.entries.toList())
        }

        // GET /scooters/{id} → one scooter
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@get
            }

            val scooter = repository.getById(id)
            if (scooter == null) {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(SCOOTER_NOT_FOUND)))
                return@get
            }
            call.respond(scooter.toResponse())
        }

        // POST /scooters → places a new listing
        post {
            val request = call.receiveOrNull<CreateScooterRequest>()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
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

        // PUT /scooters/{id} → replaces an existing listing, for example after a price change
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@put
            }

            if (repository.getById(id) == null) {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(SCOOTER_NOT_FOUND)))
                return@put
            }

            val request = call.receiveOrNull<CreateScooterRequest>()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@put
            }

            val (scooter, errors) = request.validated(id)
            if (scooter == null || errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@put
            }

            if (repository.update(scooter)) {
                call.respond(scooter.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(SCOOTER_NOT_FOUND)))
            }
        }

        // DELETE /scooters/{id} → removes a listing
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(SCOOTER_NOT_FOUND)))
            }
        }
    }
}

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
