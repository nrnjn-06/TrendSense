package com.socialanalytics.exception;

/**
 * Base root unchecked exception for all Social Analytics domain exceptions.
 */
public class SocialAnalyticsException extends RuntimeException {
    public SocialAnalyticsException(String message) {
        super(message);
    }

    public SocialAnalyticsException(String message, Throwable cause) {
        super(message, cause);
    }
}
