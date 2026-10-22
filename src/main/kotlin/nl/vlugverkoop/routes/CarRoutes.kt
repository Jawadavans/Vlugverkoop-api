package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.CreateCarRequest
import nl.vlugverkoop.dto.ValidationErrorResponse
import nl.vlugverkoop.dto.toCar
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.repository.CarRepository

private const val INVALID_ID = "id must be a whole number"
private const val INVALID_BODY = "request body must be valid car JSON"
private const val CAR_NOT_FOUND = "Car not found"

/**
 * All endpoints for cars.
 *
 * Error handling:
 * - 404 Not Found: the car does not exist.
 * - 400 Bad Request: the data in the request is not correct (body is a [ValidationErrorResponse]).
 */
fun Route.carRoutes(repository: CarRepository) {
    route("/cars") {

        // GET /cars?search=&maxPrice=&maxMileage=&doors= → cars that match all given filters
        get {
            val params = call.request.queryParameters
            val errors = mutableListOf<String>()
            val maxPrice = params.doubleOrError("maxPrice", errors)
            val maxMileage = params.intOrError("maxMileage", errors)
            val doors = params.intOrError("doors", errors)

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@get
            }

            val cars = repository.find(
                search = params["search"]?.takeIf { it.isNotBlank() },
                maxPrice = maxPrice,
                maxMileage = maxMileage,
                doors = doors
            ).map { it.toResponse() }
            call.respond(cars)
        }

        // GET /cars/{id} → returns one car
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@get
            }

            val car = repository.getById(id)
            if (car == null) {
                call.respond(HttpStatusCode.NotFound, CAR_NOT_FOUND)
                return@get
            }
            call.respond(car.toResponse())
        }

        // POST /cars → creates a new car
        post {
            val request = call.receiveCarRequestOrNull()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@post
            }

            val car = request.toCar(repository.nextId())
            val errors = car.validate()
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@post
            }

            repository.add(car)
            call.respond(HttpStatusCode.Created, car.toResponse())
        }

        // PUT /cars/{id} → replaces an existing car
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@put
            }

            if (repository.getById(id) == null) {
                call.respond(HttpStatusCode.NotFound, CAR_NOT_FOUND)
                return@put
            }

            val request = call.receiveCarRequestOrNull()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@put
            }

            val car = request.toCar(id)
            val errors = car.validate()
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@put
            }

            repository.update(id, car)
            call.respond(car.toResponse())
        }

        // DELETE /cars/{id} → deletes one car
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, CAR_NOT_FOUND)
            }
        }
    }
}

/**
 * Reads the car JSON from the request body.
 * Returns null when the JSON is broken or a field is missing,
 * so the route can answer with 400 instead of a 500 server error.
 */
private suspend fun ApplicationCall.receiveCarRequestOrNull(): CreateCarRequest? =
    try {
        receive<CreateCarRequest>()
    } catch (e: BadRequestException) {
        null
    }

/** Reads an optional whole number from the query string. Adds an error if it is not a number. */
private fun Parameters.intOrError(name: String, errors: MutableList<String>): Int? {
    val text = this[name] ?: return null
    val number = text.toIntOrNull()
    if (number == null) {
        errors.add("$name must be a whole number")
    }
    return number
}

/** Reads an optional decimal number from the query string. Adds an error if it is not a number. */
private fun Parameters.doubleOrError(name: String, errors: MutableList<String>): Double? {
    val text = this[name] ?: return null
    val number = text.toDoubleOrNull()
    if (number == null) {
        errors.add("$name must be a number")
    }
    return number
}