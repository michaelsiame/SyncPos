// src/main/java/com/kmu/syncpos/controllers/LoginController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.App;
import com.kmu.syncpos.dto.auth.LoginResult;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    // The controller now depends on the service, not the DAO.
    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and password cannot be empty.");
            return;
        }

        // The controller makes a single call to the service.
        LoginResult result = authService.login(username, password);

        // The controller's only job is to interpret the result and update the UI.
        switch (result.getStatus()) {
            case SUCCESS:
                errorLabel.setText(""); // Clear any previous errors
                openMainApplication(result.getUser());
                break;

            case INVALID_CREDENTIALS:
                errorLabel.setText(result.getMessage());
                passwordField.clear();
                break;

            case TENANT_NOT_SET:
                errorLabel.setText(result.getMessage());
                break;
        }
    }

    private void openMainApplication(User user) {
        try {
            // Close the login stage
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();

            // Load the main view
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/MainView.fxml"));
            Parent root = loader.load();

            // Get the MainController and pass the logged-in user
            MainController mainController = loader.getController();
            mainController.initData(user); // Pass the logged-in user to the main view

            // Show the main application window
            Stage mainStage = new Stage();

            // The user object itself now provides the tenantId, no need for TenantContext here.
            mainStage.setTitle("SyncPOS - Dashboard (Tenant: " + user.getTenantId() + ")");

            mainStage.setScene(new Scene(root));
            mainStage.setMaximized(true);
            mainStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Error: Could not load the main application.");
        }
    }
}