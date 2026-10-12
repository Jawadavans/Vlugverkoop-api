package nl.vlugverkoop.model

/**
 * Dutch motorcycle licence categories, ordered from lowest to highest.
 * The order matters: A1 < A2 < A, so categories can be compared.
 */
enum class LicenseCategory {
    /** Light motorcycles, up to 125 cc and 11 kW. */
    A1,

    /** Medium motorcycles, up to 35 kW. */
    A2,

    /** Every motorcycle, no limit. */
    A;

    companion object {
        /** The highest engine size (in cc) an A1 licence allows. */
        private const val A1_MAX_CC = 125

        /** The highest power (in kW) an A1 licence allows. */
        private const val A1_MAX_KW = 11

        /** The highest power (in kW) an A2 licence allows. */
        private const val A2_MAX_KW = 35

        /**
         * Returns the lowest licence needed to ride a motorcycle with this engine size and power.
         * A1 allows up to 125 cc and 11 kW, A2 allows up to 35 kW, above that A is needed.
         */
        fun minimumFor(engineCc: Int, powerKw: Int): LicenseCategory = when {
            engineCc <= A1_MAX_CC && powerKw <= A1_MAX_KW -> A1
            powerKw <= A2_MAX_KW -> A2
            else -> A
        }
    }
}
