// src/main/java/com/kmu/syncpos/controllers/LicenseDialogController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.App;
import com.kmu.syncpos.auth.TenantContext;
// REMOVE: import com.kmu.syncpos.dto.TenantDTO;
import com.kmu.syncpos.models.Tenant; // <-- IMPORT THE VIEWMODEL
import com.kmu.syncpos.service.AuthService;
import com.kmu.syncpos.service.SyncService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LicenseDialogController {

    @FXML private Label errorLabel;
    @FXML private TextField licenseKeyField;
    @FXML private Button validateButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleValidate() {
        String key = licenseKeyField.getText().trim();
        if (key.isEmpty()) {
            showError("License key cannot be empty.");
            return;
        }

        setLoadingState(true, "Activating...");

        // Run activation on a background thread to keep the UI responsive.
        new Thread(() -> {
            boolean success = authService.activateApplication(key);

            Platform.runLater(() -> {
                if (success) {
                    // Activation successful, now trigger the initial data download.
                    performInitialSync();
                } else {
                    showError("Invalid or inactive license key. Please try again.");
                    setLoadingState(false, "Activate");
                }
            });
        }).start();
    }

    /**
     * Kicks off the background service to perform the initial "pull" of all
     * data from the server after a successful activation.
     */
    private void performInitialSync() {
        setLoadingState(true, "Downloading data...");

        // --- THIS IS THE CORRECTED LINE ---
        // Get the newly activated tenant ViewModel from the context.
        Tenant tenant = TenantContext.getTenant();
        // --- END CORRECTION ---

        if (tenant == null) {
            showError("Critical error: Tenant context is not set after activation.");
            setLoadingState(false, "Activate");
            return;
        }

        // Create and start the SyncService in "initial sync" mode.
        // This will now work, assuming the SyncService constructor accepts a Tenant ViewModel.
        SyncService syncService = new SyncService(tenant, true);

        // Listen for when the sync completes (successfully or not).
        syncService.setOnSucceeded(event -> {
            System.out.println("Initial sync completed successfully.");
            Platform.runLater(this::proceedToLogin);
        });

        syncService.setOnFailed(event -> {
            System.err.println("Initial sync failed!");
            Throwable exception = syncService.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
            Platform.runLater(() -> {
                showError("Data download failed. Please check connection and restart.");
                setLoadingState(false, "Activate");
            });
        });

        // This starts the sync on a background thread.
        syncService.start();
    }

    private void proceedToLogin() {
        try {
            Stage currentStage = (Stage) validateButton.getScene().getWindow();
            new App().showLoginScreen(currentStage);
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("Fatal Error: Could not load the login screen."));
        }
    }

    private void setLoadingState(boolean isLoading, String statusText) {
        validateButton.setDisable(isLoading);
        validateButton.setText(isLoading ? "Please wait..." : "Activate");
        errorLabel.setText(statusText);
        errorLabel.setVisible(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}