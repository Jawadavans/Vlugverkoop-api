package nl.vlugverkoop.dto

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

/**
 * Everything that can go wrong with a licence plate check.
 *
 * The app shows its own text per [code] from its strings.xml (English and Dutch);
 * [message] is an English explanation for developers and for the log.
 */
enum class PlateCheckError(val status: HttpStatusCode, val message: String) {
    PLATE_INVALID(HttpStatusCode.BadRequest, "plate must be 6 letters and digits, for example AB-123-C"),
    PLATE_NOT_FOUND(HttpStatusCode.NotFound, "no vehicle is registered with this plate"),
    UNSUPPORTED_VEHICLE(HttpStatusCode.UnprocessableEntity, "this vehicle is not a car, motorcycle or scooter"),
    RDW_UNAVAILABLE(HttpStatusCode.ServiceUnavailable, "the RDW vehicle register cannot be reached, try again later")
}

/**
 * Sent to the app when a plate check fails.
 * It has the same [errors] list as [ValidationErrorResponse], plus a fixed [code] the app can check.
 */
@Serializable
data class PlateCheckErrorResponse(
    val code: String,
    val errors: List<String>
)

/** Builds the JSON answer for this error. */
fun PlateCheckError.toResponse(): PlateCheckErrorResponse = PlateCheckErrorResponse(name, listOf(message))
