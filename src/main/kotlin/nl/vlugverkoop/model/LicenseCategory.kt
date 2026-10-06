package nl.vlugverkoop.model

/**
 * Dutch motorcycle licence categories, ordered from lowest to highest.
 * The order matters: A1 < A2 < A, so categories can be compared.
 */
enum class LicenseCategory {
    A1,
    A2,
    A;

    companion object {
        /**the highest enigine size (in CC) an A1 License allows.*/
        private const val A1_MAX_CC= 125

        /**
         * Returns the lowest Licence needed to ride a motorcycle with this engine size.
         * Above 125CC at Least A2 needed.
         */
        fun minimumFor(engineCc: Int): LicenseCategory = if (engineCc <= A1_MAX_CC) A1 else A2
    }
}