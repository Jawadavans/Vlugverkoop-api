package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable

/**
 * Sent to the app with status 400 when a request contains invalid data.
 * The app shows each message under the matching input field.
 */
@Serializable
data class ValidationErrorResponse(
    val errors: List<String>
)
