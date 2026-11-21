package nl.vlugverkoop.rdw

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.Closeable
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

/** Thrown when RDW Open Data cannot be reached or sends an answer we cannot read. */
class RdwUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Looks up vehicles in RDW Open Data (https://opendata.rdw.nl).
 *
 * Two data sets are used:
 * - `m9d7-ebf2` (Gekentekende_voertuigen): kind, make, model, year, cc, doors, top speed.
 * - `8ys7-d773` (Gekentekende_voertuigen_brandstof): power in kW.
 *
 * The [httpClient] is passed in, so tests can give a fake one that never goes to the internet.
 */
class RdwClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL
) : Closeable {

    /**
     * Finds the vehicle with this [plate] (already cleaned, for example "AB123C").
     *
     * @return the vehicle, or null when RDW does not know this plate.
     * @throws RdwUnavailableException when RDW cannot be reached or gives an error.
     */
    suspend fun findVehicle(plate: String): RdwVehicle? {
        // 1. The vehicle itself. RDW answers an unknown plate with an empty list.
        val vehicle = fetchRows(VEHICLES_DATA_SET, plate).firstOrNull() ?: return null

        // 2. The power. A hybrid has one row per fuel; the highest power is the power of the vehicle.
        val fuelRows = fetchRows(FUEL_DATA_SET, plate)
        val powerKw = fuelRows.mapNotNull { readDecimal(it, "nettomaximumvermogen") }.maxOrNull()

        // 3. Everything together, converted to Kotlin types
        return RdwVehicle(
            plate = plate,
            rdwKind = readText(vehicle, "voertuigsoort"),
            brand = readText(vehicle, "merk"),
            model = readText(vehicle, "handelsbenaming"),
            year = readText(vehicle, "datum_eerste_toelating")?.take(YEAR_DIGITS)?.toIntOrNull(),
            engineCc = readWholeNumber(vehicle, "cilinderinhoud"),
            doors = readWholeNumber(vehicle, "aantal_deuren"),
            maxSpeedKmh = readWholeNumber(vehicle, "maximale_constructiesnelheid"),
            powerKw = powerKw
        )
    }

    /** Requests the rows of one data set for one plate. */
    private suspend fun fetchRows(dataSet: String, plate: String): List<JsonObject> {
        val response: HttpResponse
        try {
            response = httpClient.get("$baseUrl/$dataSet.json") {
                parameter("kenteken", plate)
            }
        } catch (e: IOException) {
            // No internet, a lost connection or a timeout
            throw RdwUnavailableException("RDW could not be reached", e)
        } catch (e: UnresolvedAddressException) {
            // The address opendata.rdw.nl cannot be found (no internet, or a DNS problem)
            throw RdwUnavailableException("RDW could not be found", e)
        }

        if (!response.status.isSuccess()) {
            throw RdwUnavailableException("RDW answered with ${response.status}")
        }

        // RDW sends a JSON list of objects. Anything else (for example a maintenance page) is an error.
        val json = try {
            Json.parseToJsonElement(response.bodyAsText())
        } catch (e: IllegalArgumentException) {
            throw RdwUnavailableException("RDW did not send JSON", e)
        }
        if (json !is JsonArray) {
            throw RdwUnavailableException("RDW did not send a list of vehicles")
        }
        return json.filterIsInstance<JsonObject>()
    }

    override fun close() = httpClient.close()

    companion object {
        const val DEFAULT_BASE_URL = "https://opendata.rdw.nl/resource"
        private const val VEHICLES_DATA_SET = "m9d7-ebf2"
        private const val FUEL_DATA_SET = "8ys7-d773"
        private const val YEAR_DIGITS = 4
        private const val TIMEOUT_MILLIS = 5_000L

        /** Creates a client that really talks to RDW, and gives up after a few seconds. */
        fun create(): RdwClient = RdwClient(
            HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = TIMEOUT_MILLIS
                }
            }
        )
    }
}

/**
 * Reads a field of an RDW row as text, or null when it is missing or empty.
 * Works for both "1598" and 1598 in the JSON, so it does not matter whether RDW sends numbers as text.
 */
private fun readText(row: JsonObject, field: String): String? {
    val value = row[field]
    if (value !is JsonPrimitive) {
        return null
    }
    val text = value.content.trim()
    return if (text.isEmpty() || text == "null") null else text
}

/** Reads a field as a decimal number, for example "11.30" → 11.3. */
private fun readDecimal(row: JsonObject, field: String): Double? = readText(row, field)?.toDoubleOrNull()

/** Reads a field as a whole number; "1598" and "1598.00" both become 1598. */
private fun readWholeNumber(row: JsonObject, field: String): Int? = readDecimal(row, field)?.toInt()
