package com.example.chat.common.phone

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneNumbersTest {

    @Test
    fun `normalizes formatted numbers to E164`() {
        assertEquals("+14155552671", PhoneNumbers.normalizeOrSelf("+1 415-555-2671"))
        assertEquals("+14155552671", PhoneNumbers.normalizeOrSelf("+1 (415) 555-2671"))
        assertEquals("+14155552671", PhoneNumbers.normalizeOrSelf("+14155552671"))
    }

    @Test
    fun `invalid and unparseable numbers are not valid`() {
        assertFalse(PhoneNumbers.isValid("+1234"))          // too short for its country
        assertFalse(PhoneNumbers.isValid("4155552671"))     // no country code
        assertFalse(PhoneNumbers.isValid("not-a-number"))
        assertFalse(PhoneNumbers.isValid(null))
        assertTrue(PhoneNumbers.isValid("+1 415 555 2671"))
    }

    @Test
    fun `invalid input is passed through unchanged for the validator to reject`() {
        assertEquals("+1234", PhoneNumbers.normalizeOrSelf("+1234"))
        assertEquals("4155552671", PhoneNumbers.normalizeOrSelf("4155552671"))
    }
}
