package com.socialanalytics.exception;

/**
 * Thrown when user input or data integrity rules are violated.
 */
public class ValidationException extends SocialAnalyticsException {
    public ValidationException(String message) {
        super(message);
    }
}
