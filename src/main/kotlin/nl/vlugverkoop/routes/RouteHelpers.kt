package nl.vlugverkoop.routes

import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive

/** Error message for an id in the URL that is not a whole number. */
internal const val INVALID_ID = "id must be a whole number"

/**
 * Reads the JSON from the request body.
 * Returns null when the JSON is broken, a field is missing or the body is not JSON at all,
 * so the route can answer with 400 instead of a 500 server error.
 */
internal suspend inline fun <reified T : Any> ApplicationCall.receiveOrNull(): T? =
    try {
        receive<T>()
    } catch (e: BadRequestException) {
        null
    } catch (e: ContentTransformationException) {
        null
    }

/** Reads an optional whole number from the query string. Adds an error if it is not a number. */
internal fun Parameters.intOrError(name: String, errors: MutableList<String>): Int? {
    val text = this[name] ?: return null
    val number = text.toIntOrNull()
    if (number == null) {
        errors.add("$name must be a whole number")
    }
    return number
}

/** Reads an optional decimal number from the query string. Adds an error if it is not a number. */
internal fun Parameters.doubleOrError(name: String, errors: MutableList<String>): Double? {
    val text = this[name] ?: return null
    val number = text.toDoubleOrNull()
    if (number == null) {
        errors.add("$name must be a number")
    }
    return number
}

/** Reads an optional enum value from the query string (not case sensitive). Adds an error if it is unknown. */
internal inline fun <reified T : Enum<T>> Parameters.enumOrError(name: String, errors: MutableList<String>): T? {
    val text = this[name] ?: return null
    val value = enumValues<T>().find { it.name.equals(text, ignoreCase = true) }
    if (value == null) {
        errors.add("$name must be one of ${enumValues<T>().toList()}")
    }
    return value
}
