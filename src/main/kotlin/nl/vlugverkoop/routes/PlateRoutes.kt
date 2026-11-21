package nl.vlugverkoop.routes

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import nl.vlugverkoop.dto.PlateCheckError
import nl.vlugverkoop.dto.toPlateCheckResponse
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.model.cleanLicensePlate
import nl.vlugverkoop.rdw.RdwClient
import nl.vlugverkoop.rdw.RdwUnavailableException

/**
 * The licence plate check: looks a plate up at RDW, so the app can show the vehicle
 * and fill in the "Place listing" form.
 *
 * Error handling (body is a [nl.vlugverkoop.dto.PlateCheckErrorResponse]):
 * - 400 PLATE_INVALID: the text is not a Dutch licence plate.
 * - 404 PLATE_NOT_FOUND: RDW does not know the plate.
 * - 422 UNSUPPORTED_VEHICLE: the vehicle is not a car, motorcycle or scooter.
 * - 503 RDW_UNAVAILABLE: RDW cannot be reached.
 */
fun Route.plateRoutes(rdw: RdwClient) {

    // GET /plates/{plate} → the vehicle with this plate, for example /plates/AB-123-C
    get("/plates/{plate}") {
        val plate = cleanLicensePlate(call.parameters["plate"].orEmpty())
        if (plate == null) {
            call.respondPlateError(PlateCheckError.PLATE_INVALID)
            return@get
        }

        val vehicle = try {
            rdw.findVehicle(plate)
        } catch (e: RdwUnavailableException) {
            call.application.environment.log.warn("RDW lookup failed", e)
            call.respondPlateError(PlateCheckError.RDW_UNAVAILABLE)
            return@get
        }
        if (vehicle == null) {
            call.respondPlateError(PlateCheckError.PLATE_NOT_FOUND)
            return@get
        }

        val result = vehicle.toPlateCheckResponse()
        if (result == null) {
            call.respondPlateError(PlateCheckError.UNSUPPORTED_VEHICLE)
            return@get
        }
        call.respond(result)
    }
}

/** Answers with the status code and JSON body that belong to [error]. */
private suspend fun ApplicationCall.respondPlateError(error: PlateCheckError) =
    respond(error.status, error.toResponse())
