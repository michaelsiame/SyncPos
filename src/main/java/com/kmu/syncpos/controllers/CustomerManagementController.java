// src/main/java/com/kmu/syncpos/controllers/CustomerManagementController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.CustomerDTO;
import com.kmu.syncpos.models.Customer;
import com.kmu.syncpos.service.CustomerService;
import com.kmu.syncpos.dto.PurchaseHistoryDTO;
import com.kmu.syncpos.util.ModelMapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomerManagementController {

    // FXML Components
    @FXML private Button newButton;
    @FXML private TextField loyaltyField;
    @FXML private TableView<PurchaseHistoryView> purchaseHistoryTableView;
    @FXML private TableColumn<PurchaseHistoryView, LocalDate> historyDateCol;
    @FXML private TableColumn<PurchaseHistoryView, String> historyTotalCol;
    @FXML private TableColumn<PurchaseHistoryView, Integer> historyItemsCol;
    @FXML private TableView<Customer> customerTableView;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, Integer> loyaltyCol;
    @FXML private TextField nameField, emailField, phoneField, searchField;
    @FXML private TextArea addressArea;
    @FXML private Button saveButton, clearButton, deleteButton;


    private final CustomerService customerService = new CustomerService();
    private ObservableList<Customer> masterCustomerList;
    private Customer selectedCustomer = null;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupPurchaseHistoryTableColumns();
        loadAndSetupTable();
        setupEventListeners();
        clearForm();
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        loyaltyCol.setCellValueFactory(new PropertyValueFactory<>("loyaltyPoints")); // Corrected to match property
    }

    private void setupPurchaseHistoryTableColumns() {
        historyDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        historyTotalCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getTotal())));
        historyItemsCol.setCellValueFactory(new PropertyValueFactory<>("itemCount"));
    }


    private void loadAndSetupTable() {
        List<CustomerDTO> customerDTOs = customerService.getAllActiveCustomers();
        List<Customer> viewModels = customerDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());

        masterCustomerList = FXCollections.observableArrayList(viewModels);

        FilteredList<Customer> filteredData = new FilteredList<>(masterCustomerList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(customer -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (customer.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (customer.getEmail() != null && customer.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
                return customer.getPhone() != null && customer.getPhone().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<Customer> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(customerTableView.comparatorProperty());
        customerTableView.setItems(sortedData);
    }

    private void setupEventListeners() {
        customerTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        populateForm(newSelection);
                    } else {
                        clearForm();
                    }
                });
        deleteButton.disableProperty().bind(customerTableView.getSelectionModel().selectedItemProperty().isNull());
    }

    @FXML
    private void handleSave() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Customer name is required.");
            return;
        }

        CustomerDTO dto = new CustomerDTO();

        if (selectedCustomer != null) {
            dto.setId(selectedCustomer.getId());
            dto.setUuid(selectedCustomer.getUuid());
            dto.setLoyaltyPoints(selectedCustomer.getLoyaltyPoints()); // Preserve loyalty points
        }

        dto.setName(nameField.getText());
        dto.setEmail(emailField.getText());
        dto.setPhone(phoneField.getText());
        dto.setAddress(addressArea.getText());

        customerService.saveCustomer(dto);

        refreshTableData();
        clearForm();
    }

    @FXML
    private void handleDelete() {
        if (selectedCustomer != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete Customer: " + selectedCustomer.getName());
            alert.setContentText("Are you sure you want to delete this customer?");
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                customerService.deleteCustomer(selectedCustomer.getId());
                refreshTableData();
                clearForm();
            }
        }
    }

    private void refreshTableData() {
        List<CustomerDTO> customerDTOs = customerService.getAllActiveCustomers();
        List<Customer> viewModels = customerDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());
        masterCustomerList.setAll(viewModels);
        customerTableView.sort(); // Re-apply sort after refresh
    }

    private void populateForm(Customer customer) {
        selectedCustomer = customer;
        nameField.setText(customer.getName());
        emailField.setText(customer.getEmail());
        phoneField.setText(customer.getPhone());
        addressArea.setText(customer.getAddress());
        loyaltyField.setText(String.valueOf(customer.getLoyaltyPoints()));
        saveButton.setText("Update");

        // Populate purchase history
        loadPurchaseHistory(customer.getId());
    }

    private void loadPurchaseHistory(long customerId) {
        List<PurchaseHistoryDTO> historyDTOs = customerService.getPurchaseHistoryForCustomer(customerId);
        List<PurchaseHistoryView> historyViews = historyDTOs.stream()
                .map(dto -> new PurchaseHistoryView(dto.date(), dto.total(), dto.itemCount()))
                .collect(Collectors.toList());
        purchaseHistoryTableView.setItems(FXCollections.observableArrayList(historyViews));
    }


    @FXML
    private void clearForm() {
        selectedCustomer = null;
        customerTableView.getSelectionModel().clearSelection();
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        addressArea.clear();
        loyaltyField.clear(); // Clear loyalty field
        purchaseHistoryTableView.getItems().clear(); // Clear history table
        saveButton.setText("Save");
        newButton.requestFocus();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleNew(ActionEvent actionEvent) {
        clearForm();
    }

    /**
     * Inner class to represent a row in the Purchase History TableView.
     * This separates the UI model from the data transfer object (DTO).
     */
    public static class PurchaseHistoryView {
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleObjectProperty<BigDecimal> total;
        private final SimpleIntegerProperty itemCount;

        public PurchaseHistoryView(LocalDate date, BigDecimal total, int itemCount) {
            this.date = new SimpleObjectProperty<>(date);
            this.total = new SimpleObjectProperty<>(total);
            this.itemCount = new SimpleIntegerProperty(itemCount);
        }

        public LocalDate getDate() { return date.get(); }
        public BigDecimal getTotal() { return total.get(); }
        public int getItemCount() { return itemCount.get(); }
    }
}