package com.afrouzi.fakecall

import com.afrouzi.fakecall.data.model.ProfileValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidationTest {

    @Test
    fun isValid_withValidNameAndNumber_returnsTrue() {
        assertTrue(ProfileValidator.isValid("John Doe", "09121234567"))
        assertTrue(ProfileValidator.isValid("مامان", "+989120000000"))
        assertTrue(ProfileValidator.isValid("Boss", "110"))
    }

    @Test
    fun isValidName_withBlankOrNull_returnsFalse() {
        assertFalse(ProfileValidator.isValidName(null))
        assertFalse(ProfileValidator.isValidName(""))
        assertFalse(ProfileValidator.isValidName("   "))
        assertFalse(ProfileValidator.isValidName("\t\n"))
    }

    @Test
    fun isValidNumber_withBlankOrNull_returnsFalse() {
        assertFalse(ProfileValidator.isValidNumber(null))
        assertFalse(ProfileValidator.isValidNumber(""))
        assertFalse(ProfileValidator.isValidNumber("   "))
        assertFalse(ProfileValidator.isValidNumber("\t\n"))
    }

    @Test
    fun isValid_withBlankNameAndValidNumber_returnsFalse() {
        assertFalse(ProfileValidator.isValid("", "09121234567"))
        assertFalse(ProfileValidator.isValid("   ", "09121234567"))
    }

    @Test
    fun isValid_withValidNameAndBlankNumber_returnsFalse() {
        assertFalse(ProfileValidator.isValid("Boss", ""))
        assertFalse(ProfileValidator.isValid("Boss", "   "))
    }

    @Test
    fun isValid_withBothBlank_returnsFalse() {
        assertFalse(ProfileValidator.isValid("", ""))
        assertFalse(ProfileValidator.isValid("   ", "   "))
    }
}
