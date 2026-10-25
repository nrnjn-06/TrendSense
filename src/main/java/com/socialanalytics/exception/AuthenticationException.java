package com.socialanalytics.exception;

/**
 * Thrown when user credentials or OAuth authorization fails or expires.
 */
public class AuthenticationException extends SocialAnalyticsException {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
