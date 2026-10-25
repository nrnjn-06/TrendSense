package com.socialanalytics.util;

import com.socialanalytics.exception.ValidationException;
import java.util.regex.Pattern;

/**
 * Validation routines for application inputs.
 */
public final class ValidationUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private ValidationUtil() {
    }

    public static void validateUsername(String username) throws ValidationException {
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new ValidationException("Username must be between 3 and 30 characters (letters, numbers, underscore only).");
        }
    }

    public static void validateEmail(String email) throws ValidationException {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Please provide a valid email address.");
        }
    }

    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        return input.trim();
    }
}
