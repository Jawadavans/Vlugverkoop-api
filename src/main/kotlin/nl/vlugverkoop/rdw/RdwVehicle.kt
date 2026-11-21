package nl.vlugverkoop.rdw

/**
 * The vehicle data VlugVerkoop uses from RDW Open Data, already converted to Kotlin types.
 *
 * Every field except [plate] can be null: RDW does not fill every field for every vehicle.
 *
 * @property plate the plate without dashes (RDW field `kenteken`).
 * @property rdwKind the kind of vehicle, for example "Personenauto" (RDW field `voertuigsoort`).
 * @property brand the make (RDW field `merk`).
 * @property model the trade name (RDW field `handelsbenaming`).
 * @property year the year of the first admission (first 4 digits of RDW field `datum_eerste_toelating`).
 * @property engineCc engine displacement in cc (RDW field `cilinderinhoud`).
 * @property doors number of doors (RDW field `aantal_deuren`).
 * @property maxSpeedKmh top speed by design in km/h (RDW field `maximale_constructiesnelheid`).
 * @property powerKw net maximum power in kW (RDW field `nettomaximumvermogen` in the fuel data set).
 */
data class RdwVehicle(
    val plate: String,
    val rdwKind: String?,
    val brand: String?,
    val model: String?,
    val year: Int?,
    val engineCc: Int?,
    val doors: Int?,
    val maxSpeedKmh: Int?,
    val powerKw: Double?
)
