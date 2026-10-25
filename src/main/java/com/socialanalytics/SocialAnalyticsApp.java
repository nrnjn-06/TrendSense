package com.socialanalytics;

import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.javafx.ui.ViewNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main JavaFX Application Launcher for Social Analytics desktop dashboard.
 */
public class SocialAnalyticsApp extends Application {

    @Override
    public void init() {
        // Initialize database and verify schema
        DatabaseManager.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        ViewNavigator.setPrimaryStage(primaryStage);
        ViewNavigator.loadLoginView();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
