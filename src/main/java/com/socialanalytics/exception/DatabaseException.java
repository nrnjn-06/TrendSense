package com.socialanalytics.exception;

/**
 * Thrown when database connectivity, transaction, or SQL execution fails.
 */
public class DatabaseException extends SocialAnalyticsException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
