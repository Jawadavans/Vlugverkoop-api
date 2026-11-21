package nl.vlugverkoop.model

/** Every Dutch licence plate has exactly this many letters and digits. */
private const val PLATE_LENGTH = 6

/**
 * Cleans a typed (or photographed) licence plate and checks whether it can be a Dutch plate.
 *
 * Cleaning removes spaces, dashes and dots and turns everything into capitals,
 * so "ab-123-c", " AB 123 C " and "AB.123.C" all become "AB123C", the form RDW uses.
 * That also makes it suitable for text that the app's camera reads from a photo.
 *
 * @return the cleaned plate, or null when it is not 6 letters and digits
 *         with at least one letter and at least one digit (every Dutch plate format has both).
 */
fun cleanLicensePlate(input: String): String? {
    val cleaned = input
        .filter { !it.isWhitespace() && it != '-' && it != '.' }
        .uppercase()

    val onlyLettersAndDigits = cleaned.all { it in 'A'..'Z' || it in '0'..'9' }
    val hasLetter = cleaned.any { it.isLetter() }
    val hasDigit = cleaned.any { it.isDigit() }

    return if (cleaned.length == PLATE_LENGTH && onlyLettersAndDigits && hasLetter && hasDigit) {
        cleaned
    } else {
        null
    }
}
