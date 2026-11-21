package nl.vlugverkoop

import nl.vlugverkoop.model.cleanLicensePlate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for cleaning and checking a typed (or photographed) licence plate.
 */
class LicensePlateTest {

    @Test
    fun `dashes, spaces, dots and small letters are cleaned up`() {
        assertEquals("AB123C", cleanLicensePlate("AB123C"))
        assertEquals("AB123C", cleanLicensePlate("ab-123-c"))
        assertEquals("AB123C", cleanLicensePlate("  AB 123 C "))
        assertEquals("AB123C", cleanLicensePlate("AB.123.C"))
        assertEquals("1KBB00", cleanLicensePlate("1-kbb-00"))
        // Text recognition can put a line break between the groups
        assertEquals("AB123C", cleanLicensePlate("AB-123\nC"))
    }

    @Test
    fun `text that cannot be a Dutch plate is rejected`() {
        assertNull(cleanLicensePlate(""), "empty")
        assertNull(cleanLicensePlate("   "), "only spaces")
        assertNull(cleanLicensePlate("AB-12-C"), "5 characters")
        assertNull(cleanLicensePlate("AB-123-CD"), "7 characters")
        assertNull(cleanLicensePlate("AB-12!-C"), "a character that is not a letter or digit")
        assertNull(cleanLicensePlate("ÄB-123-C"), "a letter that is not A to Z")
        assertNull(cleanLicensePlate("ABCDEF"), "no digit")
        assertNull(cleanLicensePlate("123456"), "no letter")
    }
}
