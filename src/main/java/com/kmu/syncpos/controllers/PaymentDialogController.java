// src/main/java/com/kmu/syncpos/controllers/PaymentDialogController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.PaymentDTO;
import com.kmu.syncpos.dto.UserDTO;
import com.kmu.syncpos.models.Payment;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.service.PaymentService;
import com.kmu.syncpos.util.ModelMapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentDialogController {

    // --- Services ---
    private final PaymentService paymentService = new PaymentService();

    // --- FXML Components ---
    @FXML private Label totalDueLabel, amountPaidLabel, balanceLabel, changeLabel;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextField amountField, referenceField;
    @FXML private Button addPaymentButton, doneButton;
    @FXML private TableView<Payment> paymentsTableView;
    @FXML private TableColumn<Payment, Double> amountCol;
    @FXML private TableColumn<Payment, String> methodCol;
    @FXML private TableColumn<Payment, LocalDateTime> dateCol;

    // --- State ---
    private long saleId;
    private double totalDue;
    private UserDTO currentUser;

    /**
     * -- GETTER --
     *  Called by POSController after the dialog closes to see if the payment was
     *  completed successfully (i.e., paid in full).
     *
     * @return true if the balance was <= 0 when the dialog was closed.
     */
    @Getter
    private boolean paymentSuccessful = false;


    @FXML
    public void initialize() {
        paymentMethodComboBox.getItems().addAll("Cash", "Card", "Transfer", "On Account");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        changeLabel.setVisible(false);
    }

    public void initData(long saleId, double totalDue, User currentUser) {
        this.saleId = saleId;
        this.totalDue = totalDue;
        this.currentUser = ModelMapper.toDto(currentUser); // Assuming ModelMapper handles null
        loadExistingPayments();
        totalDueLabel.setText(String.format("$%.2f", totalDue));
    }

    private void loadExistingPayments() {
        List<PaymentDTO> paymentDTOs = paymentService.getPaymentsForSale(this.saleId);
        List<Payment> payments = paymentDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList());
        paymentsTableView.setItems(FXCollections.observableArrayList(payments));
        updateTotals();
    }

    private void updateTotals() {
        double paid = paymentsTableView.getItems().stream().mapToDouble(Payment::getAmount).sum();
        double balance = totalDue - paid;

        amountPaidLabel.setText(String.format("$%.2f", paid));
        balanceLabel.setText(String.format("$%.2f", balance));

        if (balance <= 0) {
            addPaymentButton.setDisable(true);
            amountField.setDisable(true);
            paymentMethodComboBox.setDisable(true);
            referenceField.setDisable(true);
        } else {
            addPaymentButton.setDisable(false);
            amountField.setDisable(false);
            paymentMethodComboBox.setDisable(false);
            referenceField.setDisable(false);
        }
    }

    @FXML
    private void handleAddPayment() {
        if (paymentMethodComboBox.getValue() == null || amountField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please select a payment method and enter an amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Payment amount must be positive.");
                return;
            }

            PaymentDTO newPayment = new PaymentDTO();
            newPayment.setSaleId(this.saleId);
            newPayment.setAmount(amount);
            newPayment.setPaymentMethod(paymentMethodComboBox.getValue());
            newPayment.setReference(referenceField.getText());
            newPayment.setUserId(this.currentUser.getId());

            boolean success = paymentService.processNewPayment(newPayment);

            if (success) {
                double totalPaidSoFar = paymentsTableView.getItems().stream().mapToDouble(Payment::getAmount).sum();
                double balanceBeforeThisPayment = totalDue - totalPaidSoFar;
                if ("Cash".equals(paymentMethodComboBox.getValue()) && amount > balanceBeforeThisPayment) {
                    double change = amount - balanceBeforeThisPayment;
                    if (change > 0) {
                        changeLabel.setText(String.format("Change: $%.2f", change));
                        changeLabel.setVisible(true);
                    }
                } else {
                    changeLabel.setVisible(false);
                }

                loadExistingPayments(); // Reload to show the new payment and update totals correctly
                amountField.clear();
                referenceField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Payment Error", "Could not process the payment. Please check logs.");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number for the amount.");
        }
    }

    @FXML
    private void handleDone() {
        // --- CHANGE 2: SET STATUS FLAG ON CLOSE ---
        // Check if the total due has been paid before closing.
        double paid = paymentsTableView.getItems().stream().mapToDouble(Payment::getAmount).sum();
        double balance = totalDue - paid;
        if (balance <= 0) {
            this.paymentSuccessful = true;
        }

        Stage stage = (Stage) doneButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}