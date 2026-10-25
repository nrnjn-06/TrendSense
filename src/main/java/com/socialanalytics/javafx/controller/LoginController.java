package com.socialanalytics.javafx.controller;

import com.socialanalytics.exception.AuthenticationException;
import com.socialanalytics.exception.ValidationException;
import com.socialanalytics.javafx.ui.Toast;
import com.socialanalytics.javafx.ui.ViewNavigator;
import com.socialanalytics.model.User;
import com.socialanalytics.service.AuthService;
import com.socialanalytics.service.SocialAccountService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

/**
 * Controller for Login and Registration view.
 */
public class LoginController {

    @FXML private TabPane authTabPane;
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label loginErrorLabel;

    @FXML private TextField regUsernameField;
    @FXML private TextField regEmailField;
    @FXML private PasswordField regPasswordField;
    @FXML private Label regErrorLabel;

    private final AuthService authService = AuthService.getInstance();
    private final SocialAccountService socialAccountService = SocialAccountService.getInstance();

    @FXML
    public void initialize() {
        loginErrorLabel.setText("");
        regErrorLabel.setText("");
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = loginUsernameField.getText();
        String password = loginPasswordField.getText();

        try {
            User user = authService.login(username, password);
            Toast.show(ViewNavigator.getPrimaryStage(), "Welcome back, @" + user.getUsername() + "!", false);
            ViewNavigator.loadModeSelectionView();
        } catch (AuthenticationException e) {
            loginErrorLabel.setText(e.getMessage());
        } catch (Exception e) {
            loginErrorLabel.setText("Login error: " + e.getMessage());
        }
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = regUsernameField.getText();
        String email = regEmailField.getText();
        String password = regPasswordField.getText();

        try {
            User user = authService.register(username, email, password);
            Toast.show(ViewNavigator.getPrimaryStage(), "Account created successfully!", false);
            ViewNavigator.loadModeSelectionView();
        } catch (ValidationException | AuthenticationException e) {
            regErrorLabel.setText(e.getMessage());
        } catch (Exception e) {
            regErrorLabel.setText("Registration error: " + e.getMessage());
        }
    }

    @FXML
    public void handleQuickDemoLogin(ActionEvent event) {
        try {
            // Check or register quick guest demo account
            User user;
            try {
                user = authService.login("demo_analyst", "demo123");
            } catch (Exception notFound) {
                user = authService.register("demo_analyst", "demo@socialanalytics.local", "demo123");
            }

            authService.setCurrentUser(user);
            socialAccountService.connectAllDemoAccounts(user.getId());
            Toast.show(ViewNavigator.getPrimaryStage(), "Logged in as Demo Analyst [DEMO MODE]", false);
            ViewNavigator.loadDashboardView(com.socialanalytics.model.AppMode.DEMO);
        } catch (Exception e) {
            loginErrorLabel.setText("Demo login failed: " + e.getMessage());
        }
    }
}
