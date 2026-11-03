package nl.vlugverkoop.model

/**
 * Colour of a Dutch scooter licence plate. The colour is tied to the legal top speed.
 *
 * @property maxSpeedKmh legal top speed in km/h that belongs to this plate colour.
 */
enum class PlateColor(val maxSpeedKmh: Int) {
    /** Snorfiets, max 25 km/h. */
    BLUE(25),

    /** Bromfiets, max 45 km/h. */
    YELLOW(45);

    companion object {
        /** Every speed class a Dutch scooter is allowed to have. */
        val allowedSpeeds: Set<Int> get() = entries.map { it.maxSpeedKmh }.toSet()

        /** Returns the plate colour that belongs to [maxSpeedKmh], or null when no colour matches. */
        fun forSpeed(maxSpeedKmh: Int): PlateColor? = entries.find { it.maxSpeedKmh == maxSpeedKmh }
    }
}
