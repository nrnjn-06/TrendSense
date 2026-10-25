package com.socialanalytics.javafx.controller;

import com.socialanalytics.javafx.ui.Toast;
import com.socialanalytics.javafx.ui.ViewNavigator;
import com.socialanalytics.model.AppMode;
import com.socialanalytics.model.User;
import com.socialanalytics.service.AuthService;
import com.socialanalytics.service.SocialAccountService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller for the post-login Mode Selection screen.
 * Allows users to choose between Live Mode (authentic API integrations) and
 * Demo Mode (multi-platform interactive sandbox) before entering the dashboard.
 */
public class ModeSelectionController {

    @FXML private Label welcomeUserLabel;
    @FXML private Button btnLaunchLive;
    @FXML private Button btnLaunchDemo;

    private final AuthService authService = AuthService.getInstance();
    private final SocialAccountService socialAccountService = SocialAccountService.getInstance();

    @FXML
    public void initialize() {
        User user = authService.getCurrentUser();
        if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
            welcomeUserLabel.setText("Welcome back, @" + user.getUsername() + "!");
        } else {
            welcomeUserLabel.setText("Welcome to TrendSense");
        }
    }

    @FXML
    public void handleLaunchLiveMode(ActionEvent event) {
        User user = authService.getCurrentUser();
        Toast.show(ViewNavigator.getPrimaryStage(), "Starting TrendSense in Live Mode", false);
        ViewNavigator.loadDashboardView(AppMode.LIVE);
    }

    @FXML
    public void handleLaunchDemoMode(ActionEvent event) {
        User user = authService.getCurrentUser();
        Long userId = (user != null && user.getId() != null) ? user.getId() : 1L;
        // Ensure all 4 platforms have representative demo accounts
        socialAccountService.connectAllDemoAccounts(userId);
        Toast.show(ViewNavigator.getPrimaryStage(), "Starting TrendSense in Demo Mode", false);
        ViewNavigator.loadDashboardView(AppMode.DEMO);
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        authService.logout();
        Toast.show(ViewNavigator.getPrimaryStage(), "Logged out successfully", false);
        ViewNavigator.loadLoginView();
    }
}
