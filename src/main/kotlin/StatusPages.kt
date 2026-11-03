package nl.vlugverkoop

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            // The details go to the log only: they must not reach the client.
            call.application.environment.log.error("Unhandled exception", cause)
            call.respondText(text = "500: Internal Server Error", status = HttpStatusCode.InternalServerError)
        }
    }
}
