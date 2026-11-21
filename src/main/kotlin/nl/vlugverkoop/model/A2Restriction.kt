package nl.vlugverkoop.model

/**
 * Whether a motorcycle can be ridden with an A2 licence after restricting ("smoren") it to 35 kW.
 * By law that is only allowed when the original power is at most 70 kW.
 */
enum class A2Restriction {
    /** At most 35 kW: an A2 (or A1) licence is already enough. */
    NOT_NEEDED,

    /** More than 35 kW and at most 70 kW: can be restricted for A2. */
    POSSIBLE,

    /** More than 70 kW: too powerful to restrict, an A licence is needed. */
    NOT_POSSIBLE;

    companion object {
        /** The highest power (in kW) A2 allows without restricting. */
        private const val A2_MAX_KW = 35.0

        /** The highest original power (in kW) a motorcycle may have to be restricted for A2. */
        private const val RESTRICTABLE_MAX_KW = 70.0

        /** Returns the restriction situation for a motorcycle with this original power. */
        fun forPower(powerKw: Double): A2Restriction = when {
            powerKw <= A2_MAX_KW -> NOT_NEEDED
            powerKw <= RESTRICTABLE_MAX_KW -> POSSIBLE
            else -> NOT_POSSIBLE
        }
    }
}
