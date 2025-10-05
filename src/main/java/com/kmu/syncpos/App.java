package com.kmu.syncpos;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.TenantDAO;
import com.kmu.syncpos.models.Tenant;
import com.kmu.syncpos.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Setup and migrate the database first
        DatabaseManager.getInstance().initializeDatabase();

        // Check if a tenant is already configured locally
        TenantDAO tenantDAO = new TenantDAO();
        Tenant existingTenant = tenantDAO.findActiveTenant();

        if (existingTenant != null && "ACTIVE".equals(existingTenant.getStatus())) {
            // If a valid tenant exists, set the context and go straight to login
            System.out.println("Existing tenant found: " + existingTenant.getLicenseKey());
            TenantContext.setTenant(existingTenant);
            showLoginScreen(null); // Pass null because there's no previous stage to close
        } else {
            // Otherwise, show the license activation dialog
            System.out.println("No active tenant found. Showing license dialog.");
            showLicenseDialog();
        }
    }

    public void showLicenseDialog() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("views/LicenseDialog.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setTitle("SyncPOS License Activation");
        stage.setScene(scene);
        stage.show();
    }

    public void showLoginScreen(Stage previousStage) throws IOException {
        if (previousStage != null) {
            previousStage.close();
        }
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("views/LoginView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setTitle("SyncPOS Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}