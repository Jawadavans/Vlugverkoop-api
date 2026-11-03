package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable

/**
 * Sent to the app when a request fails, so the app only has to read one error format.
 *
 * - Status 400: the request contains invalid data. The app shows each message under the matching input field.
 * - Status 404: the requested item does not exist.
 */
@Serializable
data class ValidationErrorResponse(
    val errors: List<String>
)
