// src/main/java/com/kmu/syncpos/controllers/SupplierManagementController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.SupplierDTO;
import com.kmu.syncpos.models.Supplier;
import com.kmu.syncpos.service.SupplierService;
import com.kmu.syncpos.util.ModelMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SupplierManagementController {

    @FXML private TextField searchField, nameField, contactPersonField, emailField, phoneField, creditLimitField;
    @FXML private TableView<Supplier> supplierTableView;
    @FXML private TableColumn<Supplier, String> nameCol, contactPersonCol, phoneCol, paymentTermsCol;
    @FXML private TextArea addressArea;
    @FXML private ComboBox<String> paymentTermsComboBox;
    @FXML private Button newButton, saveButton, deleteButton;

    private final SupplierService supplierService = new SupplierService();
    private ObservableList<Supplier> masterSupplierList;
    private Supplier selectedSupplier = null;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadAndSetupTable();
        setupEventListeners();
        paymentTermsComboBox.getItems().addAll("COD", "Net 15", "Net 30", "Net 60");
        clearForm();
    }

    private void setupTableColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        contactPersonCol.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        paymentTermsCol.setCellValueFactory(new PropertyValueFactory<>("paymentTerms"));
    }

    private void loadAndSetupTable() {
        List<SupplierDTO> supplierDTOs = supplierService.getAllActiveSuppliers();
        List<Supplier> viewModels = supplierDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());
        masterSupplierList = FXCollections.observableArrayList(viewModels);

        FilteredList<Supplier> filteredData = new FilteredList<>(masterSupplierList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(supplier -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (supplier.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                return supplier.getContactPerson() != null && supplier.getContactPerson().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<Supplier> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(supplierTableView.comparatorProperty());
        supplierTableView.setItems(sortedData);
    }

    private void setupEventListeners() {
        supplierTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> populateForm(newVal)
        );
        deleteButton.disableProperty().bind(supplierTableView.getSelectionModel().selectedItemProperty().isNull());
    }

    private void refreshTableData() {
        List<SupplierDTO> supplierDTOs = supplierService.getAllActiveSuppliers();
        List<Supplier> viewModels = supplierDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());
        masterSupplierList.setAll(viewModels);
        supplierTableView.sort();
    }

    @FXML
    private void handleSave() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Supplier name cannot be empty.");
            return;
        }

        SupplierDTO dto;
        // If updating, start with a complete DTO from the model to preserve all fields.
        if (selectedSupplier != null) {
            dto = ModelMapper.toDto(selectedSupplier);
        } else {
            dto = new SupplierDTO();
        }

        // Populate DTO from the form
        dto.setName(nameField.getText().trim());
        dto.setContactPerson(contactPersonField.getText().trim());
        dto.setEmail(emailField.getText().trim());
        dto.setPhone(phoneField.getText().trim());
        dto.setAddress(addressArea.getText().trim());
        dto.setPaymentTerms(paymentTermsComboBox.getValue());

        // --- CHANGE 1: CORRECTLY HANDLE NULLABLE CREDIT LIMIT ---
        String creditLimitText = creditLimitField.getText();
        if (creditLimitText == null || creditLimitText.trim().isEmpty()) {
            dto.setCreditLimit(null); // Explicitly set to null if field is empty
        } else {
            try {
                dto.setCreditLimit(Double.parseDouble(creditLimitText));
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid number for Credit Limit.");
                return;
            }
        }

        supplierService.saveSupplier(dto);

        refreshTableData();
        clearForm();
    }

    @FXML
    private void handleNew() {
        clearForm();
    }

    @FXML
    private void handleDelete() {
        if (selectedSupplier == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete supplier: " + selectedSupplier.getName() + "?");
        confirmation.setContentText("Are you sure you want to proceed? This will mark the supplier as deleted.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            supplierService.deleteSupplier(selectedSupplier.getId());
            refreshTableData();
            clearForm();
        }
    }

    private void populateForm(Supplier supplier) {
        selectedSupplier = supplier;
        if (supplier != null) {
            nameField.setText(supplier.getName());
            contactPersonField.setText(supplier.getContactPerson());
            emailField.setText(supplier.getEmail());
            phoneField.setText(supplier.getPhone());
            addressArea.setText(supplier.getAddress());
            paymentTermsComboBox.setValue(supplier.getPaymentTerms());

            // --- CHANGE 2: CORRECTLY HANDLE NULLABLE CREDIT LIMIT ---
            if (supplier.getCreditLimit() != null) {
                creditLimitField.setText(String.format("%.2f", supplier.getCreditLimit()));
            } else {
                creditLimitField.clear(); // Clear the field if the value is null
            }
            saveButton.setText("Update");
        } else {
            clearForm();
        }
    }

    @FXML
    private void clearForm() {
        selectedSupplier = null;
        supplierTableView.getSelectionModel().clearSelection();
        nameField.clear();
        contactPersonField.clear();
        emailField.clear();
        phoneField.clear();
        addressArea.clear();
        paymentTermsComboBox.getSelectionModel().clearSelection();
        creditLimitField.clear();
        nameField.requestFocus();
        saveButton.setText("Save");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}