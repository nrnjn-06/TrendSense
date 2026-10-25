package com.socialanalytics.javafx.ui;

import com.socialanalytics.SocialAnalyticsApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Screen navigation helper for switching scenes smoothly within the primary JavaFX Stage.
 */
public class ViewNavigator {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void loadLoginView() {
        loadView("/fxml/login.fxml", "TrendSense – Login / Register", 950, 680);
    }

    public static void loadModeSelectionView() {
        loadView("/fxml/mode-selection.fxml", "TrendSense – Select Operating Mode", 980, 680);
    }

    public static void loadDashboardView() {
        loadDashboardView(com.socialanalytics.model.AppMode.LIVE);
    }

    public static void loadDashboardView(com.socialanalytics.model.AppMode mode) {
        com.socialanalytics.javafx.controller.DashboardController.setPreselectedMode(mode);
        loadView("/fxml/main.fxml", "TrendSense – Intelligent Dashboard", 1280, 820);
    }

    private static void loadView(String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(SocialAnalyticsApp.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            String css = SocialAnalyticsApp.class.getResource("/css/modern-dark.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load view " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
