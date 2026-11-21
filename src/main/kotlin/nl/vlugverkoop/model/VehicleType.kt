package nl.vlugverkoop.model

/**
 * The kinds of vehicle that can be listed on VlugVerkoop.
 *
 * @property rdwKind the value of the field `voertuigsoort` in RDW Open Data for this kind.
 */
enum class VehicleType(val rdwKind: String) {
    CAR("Personenauto"),
    MOTORCYCLE("Motorfiets"),

    /** RDW registers both 25 km/h (snorfiets) and 45 km/h (bromfiets) scooters as "Bromfiets". */
    SCOOTER("Bromfiets");

    companion object {
        /**
         * Returns the vehicle type that belongs to an RDW `voertuigsoort`,
         * or null for every other kind (a van, a trailer, a bus, ...). Not case sensitive.
         */
        fun fromRdwKind(voertuigsoort: String?): VehicleType? =
            entries.find { it.rdwKind.equals(voertuigsoort?.trim(), ignoreCase = true) }
    }
}
