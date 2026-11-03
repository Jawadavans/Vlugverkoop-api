package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.CreateMotorcycleRequest
import nl.vlugverkoop.dto.ValidationErrorResponse
import nl.vlugverkoop.dto.toMotorcycle
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.MotorcycleStyle
import nl.vlugverkoop.repository.MotorcycleRepository

private const val INVALID_BODY = "request body must be valid motorcycle JSON"
private const val MOTORCYCLE_NOT_FOUND = "Motorcycle not found"

/**
 * All endpoints for motorcycles.
 *
 * Error handling:
 * - 404 Not Found: the motorcycle does not exist (body is a [ValidationErrorResponse]).
 * - 400 Bad Request: the data in the request is not correct (body is a [ValidationErrorResponse]).
 */
fun Route.motorcycleRoutes(repository: MotorcycleRepository) {
    route("/motorcycles") {

        // GET /motorcycles → all motorcycles, optionally filtered
        // Example: /motorcycles?search=yamaha&style=NAKED&license=A2&minCc=300&maxCc=700&maxPrice=8000&maxMileage=20000
        get {
            val query = call.request.queryParameters
            val errors = mutableListOf<String>()
            val style = query.enumOrError<MotorcycleStyle>("style", errors)
            val license = query.enumOrError<LicenseCategory>("license", errors)
            val minCc = query.intOrError("minCc", errors)
            val maxCc = query.intOrError("maxCc", errors)
            val maxPrice = query.doubleOrError("maxPrice", errors)
            val maxMileage = query.intOrError("maxMileage", errors)

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@get
            }

            val motorcycles = repository.find(
                search = query["search"],
                style = style,
                license = license,
                minCc = minCc,
                maxCc = maxCc,
                maxPrice = maxPrice,
                maxMileage = maxMileage
            )
            call.respond(motorcycles.map { it.toResponse() })
        }

        // GET /motorcycles/styles → the riding styles, for the dropdown in the app
        get("/styles") {
            call.respond(MotorcycleStyle.entries.toList())
        }

        // GET /motorcycles/{id} → one motorcycle
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@get
            }

            val motorcycle = repository.getById(id)
            if (motorcycle == null) {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(MOTORCYCLE_NOT_FOUND)))
                return@get
            }
            call.respond(motorcycle.toResponse())
        }

        // POST /motorcycles → places a new listing
        post {
            val request = call.receiveOrNull<CreateMotorcycleRequest>()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@post
            }

            val motorcycle = request.toMotorcycle(repository.nextId())
            val errors = motorcycle.validate()
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@post
            }

            repository.add(motorcycle)
            call.respond(HttpStatusCode.Created, motorcycle.toResponse())
        }

        // PUT /motorcycles/{id} → replaces an existing listing, for example after a price change
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@put
            }

            if (repository.getById(id) == null) {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(MOTORCYCLE_NOT_FOUND)))
                return@put
            }

            val request = call.receiveOrNull<CreateMotorcycleRequest>()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@put
            }

            val motorcycle = request.toMotorcycle(id)
            val errors = motorcycle.validate()
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@put
            }

            if (repository.update(motorcycle)) {
                call.respond(motorcycle.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(MOTORCYCLE_NOT_FOUND)))
            }
        }

        // DELETE /motorcycles/{id} → removes a listing
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(MOTORCYCLE_NOT_FOUND)))
            }
        }
    }
}
