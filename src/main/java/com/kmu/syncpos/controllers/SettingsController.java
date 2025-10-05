// src/main/java/com/kmu/syncpos/controllers/SettingsController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.UnitDTO;
import com.kmu.syncpos.models.Unit;
import com.kmu.syncpos.service.*; // <-- IMPORT MODIFIED
import com.kmu.syncpos.util.DatabaseManager;
import com.kmu.syncpos.util.ModelMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import javax.print.PrintService; // <-- LIBRARY ADDED
import javax.print.PrintServiceLookup; // <-- LIBRARY ADDED
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SettingsController {
    // --- Services ---
    private final SettingsService settingsService = new SettingsService();
    private final UnitService unitService = new UnitService();
    // --- CHANGE 1: ADDED HARDWARE SERVICES ---
    private final ReceiptService receiptService = new ReceiptService(settingsService);
    private final CashDrawerService cashDrawerService = new CashDrawerService(settingsService);

    // --- FXML Components for Company Tab ---
    @FXML private TextField companyNameField, companyAddressField, companyPhoneField, currencySymbolField, defaultTaxRateField;
    @FXML private TextArea receiptFooterArea;

    // --- CHANGE 2: FXML COMPONENTS FOR HARDWARE TAB ---
    @FXML private ComboBox<String> printerComboBox;
    @FXML private CheckBox enableCashDrawerCheckBox;
    @FXML private Button testPrintButton;
    @FXML private Button testCashDrawerButton;

    // --- FXML Components for Units Tab ---
    @FXML private TableView<Unit> unitsTableView;
    @FXML private TableColumn<Unit, String> unitNameCol;
    @FXML private TableColumn<Unit, String> unitAbbrCol;
    @FXML private Button addUnitButton, editUnitButton, deleteUnitButton;

    // --- FXML Components for Database Tab ---
    @FXML private Label dbLocationLabel;

    private ObservableList<Unit> unitList;

    @FXML
    public void initialize() {
        // Setup for Company Tab
        loadCompanySettings();

        // Setup for Hardware Tab
        setupHardwareTab();

        // Setup for Units Tab
        setupUnitTableColumns();
        loadUnitData();
        editUnitButton.disableProperty().bind(unitsTableView.getSelectionModel().selectedItemProperty().isNull());
        deleteUnitButton.disableProperty().bind(unitsTableView.getSelectionModel().selectedItemProperty().isNull());

        // Setup for Database Tab
        dbLocationLabel.setText(DatabaseManager.getInstance().getDatabasePath());
    }

    // =================================================================
    //                COMPANY SETTINGS LOGIC (UNCHANGED)
    // =================================================================

    private void loadCompanySettings() {
        Map<String, String> settings = settingsService.getAllSettingsAsMap();
        companyNameField.setText(settings.getOrDefault("companyName", ""));
        companyAddressField.setText(settings.getOrDefault("companyAddress", ""));
        companyPhoneField.setText(settings.getOrDefault("companyPhone", ""));
        currencySymbolField.setText(settings.getOrDefault("currencySymbol", "$"));
        defaultTaxRateField.setText(settings.getOrDefault("defaultTaxRate", "0.0"));
        receiptFooterArea.setText(settings.getOrDefault("receiptFooter", "Thank you for your business!"));
    }

    // =================================================================
    //                HARDWARE SETTINGS LOGIC (NEW)
    // =================================================================

    private void setupHardwareTab() {
        handleRefreshPrinters(); // Initial load
        loadHardwareSettings();
        testCashDrawerButton.disableProperty().bind(enableCashDrawerCheckBox.selectedProperty().not());
    }

    @FXML
    private void handleRefreshPrinters() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        ObservableList<String> printers = FXCollections.observableArrayList(
                Arrays.stream(printServices).map(PrintService::getName).collect(Collectors.toList())
        );
        printerComboBox.setItems(printers);
        // Reselect the saved printer if it's still in the list
        String savedPrinter = settingsService.getSetting("printerName",null);
        if (savedPrinter != null && printers.contains(savedPrinter)) {
            printerComboBox.setValue(savedPrinter);
        }
    }

    private void loadHardwareSettings() {
        Map<String, String> settings = settingsService.getAllSettingsAsMap();
        String savedPrinter = settings.get("printerName");
        if (savedPrinter != null && printerComboBox.getItems().contains(savedPrinter)) {
            printerComboBox.setValue(savedPrinter);
        }
        boolean cashDrawerEnabled = Boolean.parseBoolean(settings.getOrDefault("cashDrawerEnabled", "false"));
        enableCashDrawerCheckBox.setSelected(cashDrawerEnabled);
    }

    @FXML
    private void handleTestPrint() {
        try {
            receiptService.printTestPage();
            showAlert(Alert.AlertType.INFORMATION, "Success", "A test page has been sent to the selected printer.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Print Error", "Could not print test page. Please check printer connection and selection.\nError: " + e.getMessage());
        }
    }

    @FXML
    private void handleTestCashDrawer() {
        try {
            cashDrawerService.openDrawer();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Cash drawer open command sent.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Drawer Error", "Could not open cash drawer. Ensure it's enabled and connected via the selected printer.\nError: " + e.getMessage());
        }
    }

    // =================================================================
    //                      SAVE ALL SETTINGS
    // =================================================================

    @FXML
    private void handleSaveAllSettings() {
        Map<String, String> settingsToSave = new HashMap<>();
        // Company settings
        settingsToSave.put("companyName", companyNameField.getText());
        settingsToSave.put("companyAddress", companyAddressField.getText());
        settingsToSave.put("companyPhone", companyPhoneField.getText());
        settingsToSave.put("currencySymbol", currencySymbolField.getText());
        settingsToSave.put("defaultTaxRate", defaultTaxRateField.getText());
        settingsToSave.put("receiptFooter", receiptFooterArea.getText());

        // --- CHANGE 3: ADDED HARDWARE SETTINGS TO SAVE LOGIC ---
        if (printerComboBox.getValue() != null) {
            settingsToSave.put("printerName", printerComboBox.getValue());
        }
        settingsToSave.put("cashDrawerEnabled", String.valueOf(enableCashDrawerCheckBox.isSelected()));


        boolean success = settingsService.saveAllSettings(settingsToSave);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "All settings have been saved.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save settings. Please check the logs.");
        }
    }


    // =================================================================
    //                UNITS OF MEASUREMENT LOGIC (UNCHANGED)
    // =================================================================

    private void setupUnitTableColumns() {
        unitNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        unitAbbrCol.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));
    }

    private void loadUnitData() {
        List<UnitDTO> unitDTOs = unitService.getAllActiveUnits();
        List<Unit> viewModels = unitDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());
        unitList = FXCollections.observableArrayList(viewModels);
        unitsTableView.setItems(unitList);
    }

    @FXML
    private void handleAddUnit() {
        showUnitDialog(null);
    }

    @FXML
    private void handleEditUnit() {
        Unit selectedUnit = unitsTableView.getSelectionModel().getSelectedItem();
        if (selectedUnit != null) {
            showUnitDialog(selectedUnit);
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a unit to edit.");
        }
    }

    @FXML
    private void handleDeleteUnit() {
        Unit selectedUnit = unitsTableView.getSelectionModel().getSelectedItem();
        if (selectedUnit == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a unit to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete '" + selectedUnit.getName() + "'?");
        confirmation.setContentText("Are you sure? This cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            unitService.deleteUnit(selectedUnit.getId());
            loadUnitData();
        }
    }

    private void showUnitDialog(Unit unit) {
        Dialog<Unit> dialog = new Dialog<>();
        dialog.setTitle(unit == null ? "Add New Unit" : "Edit Unit");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        TextField nameField = new TextField(); nameField.setPromptText("e.g., Kilogram");
        TextField abbrField = new TextField(); abbrField.setPromptText("e.g., kg");
        if (unit != null) {
            nameField.setText(unit.getName());
            abbrField.setText(unit.getAbbreviation());
        }
        VBox content = new VBox(10, new Label("Unit Name:"), nameField, new Label("Abbreviation:"), abbrField);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Unit resultUnit = (unit == null) ? new Unit() : unit;
                resultUnit.setName(nameField.getText().trim());
                resultUnit.setAbbreviation(abbrField.getText().trim());
                return resultUnit;
            }
            return null;
        });

        Optional<Unit> result = dialog.showAndWait();
        result.ifPresent(editedUnit -> {
            UnitDTO dto = ModelMapper.toDto(editedUnit);
            unitService.saveUnit(dto);
            loadUnitData();
        });
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}