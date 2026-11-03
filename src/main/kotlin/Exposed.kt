package nl.vlugverkoop

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import nl.vlugverkoop.dto.ValidationErrorResponse
import nl.vlugverkoop.routes.INVALID_ID
import nl.vlugverkoop.routes.receiveOrNull
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

private const val INVALID_BODY = "request body must be valid user JSON"
private const val USER_NOT_FOUND = "User not found"

suspend fun Application.configureExposed() {
    val database = R2dbcDatabase.connect(
        url = "r2dbc:h2:file:///./h2",
        user = "root",
        password = "",
    )
    val userService = ExposedUserService(database).also {
        it.createSchema()
    }

    routing {
        // Create user
        post("/users") {
            val user = call.receiveOrNull<ExposedUser>()
            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@post
            }

            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }

        // Read user
        get("/users/{id}") {
            val id = call.parameters["id"]?.toUIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@get
            }

            val user = userService.read(id)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(USER_NOT_FOUND)))
                return@get
            }
            call.respond(user)
        }

        // Update user
        put("/users/{id}") {
            val id = call.parameters["id"]?.toUIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@put
            }

            val user = call.receiveOrNull<ExposedUser>()
            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@put
            }

            if (userService.update(id, user)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(USER_NOT_FOUND)))
            }
        }

        // Delete user
        delete("/users/{id}") {
            val id = call.parameters["id"]?.toUIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@delete
            }

            if (userService.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ValidationErrorResponse(listOf(USER_NOT_FOUND)))
            }
        }
    }
}
