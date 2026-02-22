package com.secureapp.util;

import java.util.regex.Pattern;

/**
 * Input validation utility class.
 * Provides methods for validating user inputs according to security
 * requirements.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private static final Pattern FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]{1,255}$");

    private ValidationUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Validate email format.
     * 
     * @param email Email to validate
     * @return true if valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate password strength.
     * Password must be at least 8 characters with uppercase, lowercase, digit, and
     * special char.
     * 
     * @param password Password to validate
     * @return true if valid
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validate filename format.
     * 
     * @param filename Filename to validate
     * @return true if valid
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        return FILENAME_PATTERN.matcher(filename).matches();
    }

    /**
     * Sanitize string input by trimming and limiting length.
     * 
     * @param input     Input string
     * @param maxLength Maximum allowed length
     * @return Sanitized string
     */
    public static String sanitizeInput(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }
}
