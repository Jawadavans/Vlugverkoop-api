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
import nl.vlugverkoop.dto.CreateMotorcycleRequest
import nl.vlugverkoop.dto.ValidationErrorResponse
import nl.vlugverkoop.dto.toMotorcycle
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.model.LicenseCategory
import nl.vlugverkoop.model.MotorcycleStyle
import nl.vlugverkoop.repository.MotorcycleRepository

/**
 * All endpoints for motorcycles.
 */
fun Route.motorcycleRoutes(repository: MotorcycleRepository) {
    route("/motorcycles") {

        // GET /motorcycles → returns all motorcycles as JSON, optionally filtered
        // Example: /motorcycles?search=yamaha&style=NAKED&license=A2&minCc=300&maxCc=700&maxPrice=8000
        get {
            val query = call.request.queryParameters
            val motorcycles = repository.find(
                search = query["search"],
                style = MotorcycleStyle.entries.find { it.name.equals(query["style"], ignoreCase = true) },
                license = LicenseCategory.entries.find { it.name.equals(query["license"], ignoreCase = true) },
                minCc = query["minCc"]?.toIntOrNull(),
                maxCc = query["maxCc"]?.toIntOrNull(),
                maxPrice = query["maxPrice"]?.toDoubleOrNull()
            )
            call.respond(motorcycles.map { it.toResponse() })
        }

        // GET /motorcycles/styles → returns all riding styles, for the dropdown in the app
        get("/styles") {
            call.respond(MotorcycleStyle.entries.toList())
        }

        // GET /motorcycles/{id} → returns one motorcycle, or 404 when it does not exist
        get("/{id}") {
            val motorcycle = call.parameters["id"]?.toIntOrNull()?.let { repository.getById(it) }
            if (motorcycle == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(motorcycle.toResponse())
            }
        }

        // POST /motorcycles → places a new listing, or returns 400 with the reasons when the data is invalid
        post {
            val request = try {
                call.receive<CreateMotorcycleRequest>()
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf("invalid motorcycle data")))
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

        // PUT /motorcycles/{id} → replaces an existing listing, for example after a price change,
        // or returns 404 when it does not exist
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null || repository.getById(id) == null) {
                call.respond(HttpStatusCode.NotFound)
                return@put
            }

            val request = try {
                call.receive<CreateMotorcycleRequest>()
            } catch (e: BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf("invalid motorcycle data")))
                return@put
            }

            val motorcycle = request.toMotorcycle(id)
            val errors = motorcycle.validate()
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@put
            }

            if (repository.update(motorcycle)) {
                call.respond(HttpStatusCode.OK, motorcycle.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // DELETE /motorcycles/{id} → removes a listing, or returns 404 when it does not exist
        delete("/{id}") {
            val deleted = call.parameters["id"]?.toIntOrNull()?.let { repository.delete(it) } ?: false
            call.respond(if (deleted) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
        }
    }
}
