package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.CreateCarRequest
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

        // POST /cars → creates a new car
        post {
            val request = call.receive<CreateCarRequest>()
            val car = repository.add(
                brand = request.brand,
                model = request.model,
                year = request.year,
                price = request.price,
                mileage = request.mileage,
                doors = request.doors
            )
            call.respond(HttpStatusCode.Created, car.toResponse())
        }

        // DELETE /cars/{id} → deletes one car
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Car not found")
            }
        }
    }
}