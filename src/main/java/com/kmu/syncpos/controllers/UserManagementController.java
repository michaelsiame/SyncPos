// src/main/java/com/kmu/syncpos/controllers/UserManagementController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.UserDTO;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.service.UserService;
import com.kmu.syncpos.util.ModelMapper;
import javafx.beans.property.SimpleStringProperty;
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

public class UserManagementController {

    // --- FXML Components (unchanged) ---
    @FXML private TextField searchField, usernameField, firstNameField, lastNameField, emailField, phoneField;
    @FXML private TableView<User> userTableView;
    @FXML private TableColumn<User, String> usernameCol, nameCol, emailCol, roleCol;
    @FXML private TableColumn<User, Boolean> activeCol;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private CheckBox activeCheckBox;
    @FXML private Button newButton, saveButton, deleteButton;

    // --- Use Service, not DAO ---
    private final UserService userService = new UserService();
    private User selectedUser = null;
    private ObservableList<User> masterUserList;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadAndSetupTable();
        setupEventListeners();
        roleComboBox.getItems().addAll("admin", "inventory_manager", "cashier");
        clearForm();
    }

    private void setupTableColumns() {
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getFirstname() + " " + cellData.getValue().getLastname()
        ));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("isActive"));
    }

    private void setupEventListeners() {
        userTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> populateForm(newVal)
        );
        deleteButton.disableProperty().bind(userTableView.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadAndSetupTable() {
        List<UserDTO> userDTOs = userService.getAllActiveUsers();
        List<User> viewModels = userDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList());
        masterUserList = FXCollections.observableArrayList(viewModels);

        FilteredList<User> filteredData = new FilteredList<>(masterUserList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String filter = newValue.toLowerCase();
                if (user.getUsername().toLowerCase().contains(filter)) return true;
                if (user.getFirstname().toLowerCase().contains(filter)) return true;
                if (user.getLastname().toLowerCase().contains(filter)) return true;
                return user.getEmail() != null && user.getEmail().toLowerCase().contains(filter);
            });
        });

        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTableView.comparatorProperty());
        userTableView.setItems(sortedData);
    }

    private void refreshTableData() {
        List<UserDTO> userDTOs = userService.getAllActiveUsers();
        List<User> viewModels = userDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList());
        masterUserList.setAll(viewModels);
    }

    @FXML
    private void handleSave() {
        if (usernameField.getText().isEmpty() || firstNameField.getText().isEmpty() || roleComboBox.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Username, First Name, and Role are required.");
            return;
        }

        UserDTO dto = new UserDTO();

        // If updating, preserve the ID and UUID
        if (selectedUser != null) {
            dto.setId(selectedUser.getId());
            dto.setUuid(selectedUser.getUuid());
        } else {
            // Password is required for new users
            if (passwordField.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Password is required for new users.");
                return;
            }
        }

        // Populate DTO from form
        dto.setUsername(usernameField.getText());
        dto.setFirstname(firstNameField.getText());
        dto.setLastname(lastNameField.getText());
        dto.setEmail(emailField.getText());
        dto.setPhone(phoneField.getText());
        dto.setRole(roleComboBox.getValue());
        dto.setActive(activeCheckBox.isSelected());

        // Only set the plain password on the DTO if it has been changed
        if (!passwordField.getText().isEmpty()) {
            dto.setPlainPassword(passwordField.getText());
        }

        userService.saveUser(dto);
        refreshTableData();
        clearForm();
    }

    @FXML
    private void handleNew() {
        clearForm();
    }

    @FXML
    private void handleDelete() {
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete user: " + selectedUser.getUsername() + "?");
        confirmation.setContentText("This will mark the user as deleted and inactive.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userService.deleteUser(selectedUser.getId());
            refreshTableData();
            clearForm();
        }
    }

    private void populateForm(User user) {
        selectedUser = user;
        if (user != null) {
            usernameField.setText(user.getUsername());
            firstNameField.setText(user.getFirstname());
            lastNameField.setText(user.getLastname());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone());
            roleComboBox.setValue(user.getRole());
            activeCheckBox.setSelected(user.getIsActive());
            passwordField.clear();
            passwordField.setPromptText("Unchanged (enter new password to modify)");
        } else {
            clearForm();
        }
    }

    @FXML
    private void clearForm() {
        selectedUser = null;
        userTableView.getSelectionModel().clearSelection();
        usernameField.clear();
        firstNameField.clear();
        lastNameField.clear();
        passwordField.clear();
        passwordField.setPromptText("Password (required for new user)");
        emailField.clear();
        phoneField.clear();
        roleComboBox.getSelectionModel().clearSelection();
        activeCheckBox.setSelected(true); // Default to active
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}