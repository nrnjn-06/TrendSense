package com.socialanalytics.exception;

/**
 * Thrown when a platform API does not permit or support a requested metric
 * (e.g. impressions on personal accounts or restricted permissions).
 */
public class UnsupportedMetricException extends SocialAnalyticsException {
    private final String platform;
    private final String metricName;

    public UnsupportedMetricException(String platform, String metricName, String message) {
        super(String.format("[%s] Metric '%s' is not supported: %s", platform, metricName, message));
        this.platform = platform;
        this.metricName = metricName;
    }

    public String getPlatform() {
        return platform;
    }

    public String getMetricName() {
        return metricName;
    }
}
