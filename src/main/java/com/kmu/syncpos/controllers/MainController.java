// src/main/java/com/kmu/syncpos/controllers/MainController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.App;
import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dto.TenantDTO;
import com.kmu.syncpos.models.Tenant;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.service.SyncService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    // --- FXML Injected Components ---
    @FXML private StackPane contentPane;
    @FXML private Label currentUserLabel;
    @FXML private Label currentUserRoleLabel;
    @FXML private Label syncStatusLabel; // This label MUST exist in your FXML

    // --- Menu items for role-based access control ---
    @FXML private MenuItem dashboardMenuItem, posMenuItem, productManagementMenuItem,
            categoryManagementMenuItem, supplierManagementMenuItem,
            userManagementMenuItem, customerManagementMenuItem,
            purchaseMenuItem, reportsMenuItem, settingsMenuItem,
            logoutMenuItem;

    // --- State and Services ---
    private User loggedInUser;
    private SyncService syncService;
    private ScheduledExecutorService syncScheduler;

    @FXML
    public void initialize() {
        showDashboard();
    }

    public void initData(User user) {
        this.loggedInUser = user;
        updateStatusBar();
        applyRolePermissions();
        setupAndStartPeriodicSync(); // <-- Renamed for clarity
    }


    private void updateStatusBar() {
        if (loggedInUser != null) {
            currentUserLabel.setText(loggedInUser.getUsername());
            currentUserRoleLabel.setText(loggedInUser.getRole());
        } else {
            currentUserLabel.setText("N/A");
            currentUserRoleLabel.setText("N/A");
        }
    }

    private void setupAndStartPeriodicSync() {
        if (loggedInUser == null) {
            syncStatusLabel.setText("Sync Disabled: No user logged in.");
            return;
        }

        // --- THIS IS THE CORRECTED LOGIC ---
        // 1. Get the Tenant ViewModel directly from the context.
        Tenant currentTenant = TenantContext.getTenant();

        // 2. Pass the ViewModel to the SyncService constructor.
        //    (Assuming SyncService constructor accepts a Tenant ViewModel).
        syncService = new SyncService(currentTenant, false); // isInitialSync is false for periodic push
        // --- END CORRECTION ---

        // Bind the UI label's text directly to the service's message property.
        syncStatusLabel.textProperty().bind(syncService.messageProperty());

        syncService.setOnFailed(event -> {
            System.err.println("A periodic sync task has failed!");
            if(syncService.getException() != null){
                syncService.getException().printStackTrace();
            }
        });

        syncService.setOnSucceeded(event -> {
            System.out.println("Periodic sync push completed.");
        });

        // Set up a scheduler to run the sync service periodically.
        syncScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "SyncScheduler");
            t.setDaemon(true); // Ensures this thread doesn't prevent app from closing
            return t;
        });

        // Start after 10 seconds, then run every 5 minutes.
        syncScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (syncService != null && !syncService.isRunning()) {
                    syncService.restart();
                }
            });
        }, 10, 300, TimeUnit.SECONDS);
    }
    private void applyRolePermissions() {
        if (loggedInUser == null) return;

        String role = loggedInUser.getRole().toLowerCase();

        // Deny all by default for clarity
        posMenuItem.setDisable(true);
        productManagementMenuItem.setDisable(true);
        categoryManagementMenuItem.setDisable(true);
        supplierManagementMenuItem.setDisable(true);
        userManagementMenuItem.setDisable(true);
        reportsMenuItem.setDisable(true);
        settingsMenuItem.setDisable(true);
        purchaseMenuItem.setDisable(true);
        customerManagementMenuItem.setDisable(true);

        switch (role) {
            case "admin":
                posMenuItem.setDisable(false);
                productManagementMenuItem.setDisable(false);
                categoryManagementMenuItem.setDisable(false);
                supplierManagementMenuItem.setDisable(false);
                userManagementMenuItem.setDisable(false);
                reportsMenuItem.setDisable(false);
                settingsMenuItem.setDisable(false);
                purchaseMenuItem.setDisable(false);
                customerManagementMenuItem.setDisable(false);
                break;
            case "inventory_manager":
                productManagementMenuItem.setDisable(false);
                categoryManagementMenuItem.setDisable(false);
                supplierManagementMenuItem.setDisable(false);
                reportsMenuItem.setDisable(false);
                purchaseMenuItem.setDisable(false);
                break;
            case "cashier":
                posMenuItem.setDisable(false);
                reportsMenuItem.setDisable(false);
                customerManagementMenuItem.setDisable(false);
                break;
        }
    }

    // --- Navigation Handlers ---
    @FXML private void showDashboard() { loadView("DashboardView.fxml"); }
    @FXML private void showPOS() { loadView("POSView.fxml"); }
    @FXML private void showProductManagement() { loadView("ProductManagementView.fxml"); }
    @FXML private void showCategoryManagement() { loadView("CategoryManagementView.fxml"); }
    @FXML private void showCustomerManagement() { loadView("CustomerManagementView.fxml"); }
    @FXML private void showSupplierManagement() { loadView("SupplierManagementView.fxml"); }
    @FXML private void showUserManagement() { loadView("UserManagementView.fxml"); }
    @FXML private void showReports() { loadView("ReportsView.fxml"); }
    @FXML private void showSettings() { loadView("SettingsView.fxml"); }
    @FXML private void showPurchaseView() { loadView("PurchaseView.fxml"); }

    @FXML
    private void handleLogout() {
        shutdown(); // Gracefully stop background tasks
        Stage stage = (Stage) contentPane.getScene().getWindow();
        stage.close();
        Platform.runLater(() -> {
            try {
                new App().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleExit() {
        shutdown(); // Gracefully stop background tasks
        Platform.exit();
    }

    /**
     * Loads the specified FXML view into the main content pane and injects
     * the logged-in user if the controller implements the UserAware interface.
     * @param fxmlFileName The name of the FXML file to load.
     */
    private void loadView(String fxmlFileName) {
        try {
            String fxmlPath = "/com/kmu/syncpos/views/" + fxmlFileName;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node newView = loader.load();

            Object controller = loader.getController();
            if (controller instanceof UserAware && loggedInUser != null) {
                ((UserAware) controller).setUser(loggedInUser);
            }

            contentPane.getChildren().setAll(newView);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load view: " + fxmlFileName);
            // In a real application, you would show a user-friendly error dialog here.
        }
    }

    /**
     * Gracefully shuts down the background sync scheduler.
     * This should be called when the main window is closing.
     */
    public void shutdown() {
        System.out.println("MainController: Shutting down sync service scheduler.");
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            syncScheduler.shutdownNow();
        }
    }
}