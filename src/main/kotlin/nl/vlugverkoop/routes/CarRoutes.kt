package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
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

private const val INVALID_BODY = "request body must be valid car JSON"
private const val CAR_NOT_FOUND = "Car not found"

/**
 * All endpoints for cars.
 *
 * Error handling:
 * - 404 Not Found: the car does not exist (body is a [ValidationErrorResponse]).
 * - 400 Bad Request: the data in the request is not correct (body is a [ValidationErrorResponse]).
 */
fun Route.carRoutes(repository: CarRepository) {
    route("/cars") {

        // GET /cars → all cars, optionally filtered
        // Example: /cars?search=volkswagen&maxPrice=20000&maxMileage=100000&doors=5
        get {
            val query = call.request.queryParameters
            val errors = mutableListOf<String>()
            val maxPrice = query.doubleOrError("maxPrice", errors)
            val maxMileage = query.intOrError("maxMileage", errors)
            val doors = query.intOrError("doors", errors)

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@get
            }

            val cars = repository.find(
                search = query["search"],
                maxPrice = maxPrice,
                maxMileage = maxMileage,
                doors = doors
            )
            call.respond(cars.map { it.toResponse() })
        }

        // GET /cars/{id} → one car
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@get
            }

            val car = repository.getById(id)
            if (car == null) {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(CAR_NOT_FOUND)))
                return@get
            }
            call.respond(car.toResponse())
        }

        // POST /cars → places a new listing
        post {
            val request = call.receiveOrNull<CreateCarRequest>()
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

        // PUT /cars/{id} → replaces an existing listing, for example after a price change
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@put
            }

            if (repository.getById(id) == null) {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(CAR_NOT_FOUND)))
                return@put
            }

            val request = call.receiveOrNull<CreateCarRequest>()
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

            if (repository.update(car)) {
                call.respond(car.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(CAR_NOT_FOUND)))
            }
        }

        // DELETE /cars/{id} → removes a listing
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(CAR_NOT_FOUND)))
            }
        }
    }
}
