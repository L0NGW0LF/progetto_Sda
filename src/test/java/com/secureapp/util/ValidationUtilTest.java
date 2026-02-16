package com.secureapp.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtil.
 * Tests input validation, email format validation, and password policy.
 */
class ValidationUtilTest {

    // ==================== sanitizeInput Tests ====================

    @Test
    void testSanitizeInput_WithNormalInput_ReturnsTrimmed() {
        String input = "  hello world  ";
        String result = ValidationUtil.sanitizeInput(input, 255);
        assertEquals("hello world", result);
    }

    @Test
    void testSanitizeInput_WithTooLongInput_Truncates() {
        // Java 8 compatible: build string manually instead of repeat()
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append("a");
        }
        String result = ValidationUtil.sanitizeInput(sb.toString(), 100);
        assertEquals(100, result.length());
    }

    @Test
    void testSanitizeInput_WithNull_ReturnsEmptyString() {
        String result = ValidationUtil.sanitizeInput(null, 255);
        assertEquals("", result);
    }

    @Test
    void testSanitizeInput_WithEmptyString_ReturnsEmpty() {
        String result = ValidationUtil.sanitizeInput("", 255);
        assertEquals("", result);
    }

    @Test
    void testSanitizeInput_WithWhitespaceOnly_ReturnsEmpty() {
        String result = ValidationUtil.sanitizeInput("   ", 255);
        assertEquals("", result);
    }

    // ==================== isValidEmail Tests ====================

    @Test
    void testIsValidEmail_WithValidEmail_ReturnsTrue() {
        assertTrue(ValidationUtil.isValidEmail("test@example.com"));
        assertTrue(ValidationUtil.isValidEmail("user.name@domain.co.uk"));
        assertTrue(ValidationUtil.isValidEmail("user+tag@example.com"));
    }

    @Test
    void testIsValidEmail_WithInvalidFormat_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidEmail("notanemail"));
        assertFalse(ValidationUtil.isValidEmail("@example.com"));
        assertFalse(ValidationUtil.isValidEmail("user@"));
        assertFalse(ValidationUtil.isValidEmail("user @example.com")); // space
    }

    @Test
    void testIsValidEmail_WithNull_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    void testIsValidEmail_WithEmpty_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidEmail(""));
        assertFalse(ValidationUtil.isValidEmail("   "));
    }

    @Test
    void testIsValidEmail_WithXSSAttempt_ReturnsFalse() {
        // XSS injection attempts should be rejected by email validation
        assertFalse(ValidationUtil.isValidEmail("<script>alert(1)</script>@test.com"));
        assertFalse(ValidationUtil.isValidEmail("test<script>@example.com"));
        assertFalse(ValidationUtil.isValidEmail("test@<script>.com"));
    }

    @Test
    void testIsValidEmail_WithSQLInjectionAttempt_ReturnsFalse() {
        // SQL injection attempts should be rejected
        assertFalse(ValidationUtil.isValidEmail("test' OR '1'='1"));
        assertFalse(ValidationUtil.isValidEmail("admin'--@test.com"));
    }

    // ==================== isValidPassword Tests ====================

    @Test
    void testIsValidPassword_WithValidPassword_ReturnsTrue() {
        assertTrue(ValidationUtil.isValidPassword("Test@1234"));
        assertTrue(ValidationUtil.isValidPassword("SecureP@ssw0rd"));
        assertTrue(ValidationUtil.isValidPassword("C0mpl3x!Pass"));
    }

    @Test
    void testIsValidPassword_WithWeakPassword_ReturnsFalse() {
        // Too short
        assertFalse(ValidationUtil.isValidPassword("Test@1"));

        // No uppercase
        assertFalse(ValidationUtil.isValidPassword("test@1234"));

        // No lowercase
        assertFalse(ValidationUtil.isValidPassword("TEST@1234"));

        // No digit
        assertFalse(ValidationUtil.isValidPassword("Test@Pass"));

        // No special character
        assertFalse(ValidationUtil.isValidPassword("Test1234"));

        // All weak
        assertFalse(ValidationUtil.isValidPassword("password"));
    }

    @Test
    void testIsValidPassword_WithNull_ReturnsFalse() {
        assertFalse(ValidationUtil.isValidPassword(null));
    }

    @Test
    void testIsValidPassword_WithWhitespace_ReturnsFalse() {
        // Password with spaces should fail (policy requires no whitespace)
        assertFalse(ValidationUtil.isValidPassword("Test @1234"));
    }

    @Test
    void testIsValidPassword_WithSpecialChars_ValidIfMeetsPolicy() {
        // Special characters allowed by policy: @$!%*?&
        // As long as they meet password policy requirements (8+ chars, uppercase,
        // digit, special)
        assertTrue(ValidationUtil.isValidPassword("Test@1234"));
        assertTrue(ValidationUtil.isValidPassword("Pass!word1"));
        assertTrue(ValidationUtil.isValidPassword("P@ssw0rd"));
    }
}
