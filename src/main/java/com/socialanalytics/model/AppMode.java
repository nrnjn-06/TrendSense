package com.socialanalytics.model;

/**
 * Centralized application operating mode.
 * Enforces strict separation between Live API Mode and Demo Mode (Simulated Data).
 */
public enum AppMode {
    LIVE("Live API Mode", "Displays verified real-time data from authorized social accounts."),
    DEMO("Demo Mode – Simulated Data", "Displays realistic simulated data for testing and demonstration.");

    private final String title;
    private final String description;

    AppMode(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
