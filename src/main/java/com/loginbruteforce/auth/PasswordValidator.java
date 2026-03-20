package com.loginbruteforce.auth;

/**
 * Validates password strength during user registration.
 *
 * <p>Rules enforced:</p>
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one digit (0–9)</li>
 *   <li>At least one special character (non-alphanumeric)</li>
 * </ul>
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;

    private PasswordValidator() {
        // Utility class — prevent instantiation
    }

    /**
     * Validates a password against the strength rules.
     *
     * @param password the password to validate
     * @return {@code null} if the password is strong enough,
     *         otherwise a human-readable error message
     */
    public static String validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters long.";
        }

        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }

        if (!hasDigit) {
            return "Password must contain at least one number (0-9).";
        }
        if (!hasSpecial) {
            return "Password must contain at least one special character (!@#$%^&* etc.).";
        }
        return null;
    }
}
