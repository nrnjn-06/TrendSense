package com.socialanalytics.exception;

/**
 * Thrown when an external social media platform API returns an error or is unreachable.
 */
public class ApiException extends SocialAnalyticsException {
    private final String platform;
    private final int statusCode;

    public ApiException(String platform, String message) {
        super("[" + platform + "] " + message);
        this.platform = platform;
        this.statusCode = 0;
    }

    public ApiException(String platform, int statusCode, String message) {
        super(String.format("[%s] HTTP %d: %s", platform, statusCode, message));
        this.platform = platform;
        this.statusCode = statusCode;
    }

    public ApiException(String platform, String message, Throwable cause) {
        super("[" + platform + "] " + message, cause);
        this.platform = platform;
        this.statusCode = 0;
    }

    public String getPlatform() {
        return platform;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
