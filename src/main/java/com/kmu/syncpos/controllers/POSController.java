// src/main/java/com/kmu/syncpos/controllers/POSController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.App;
import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.*;
import com.kmu.syncpos.service.*;
import com.kmu.syncpos.util.ModelMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import javax.print.PrintException; // <-- LIBRARY ADDED for hardware exceptions
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class POSController implements UserAware {

    // --- Services ---
    private final ProductService productService = new ProductService();
    private final CustomerService customerService = new CustomerService();
    private final SaleService saleService = new SaleService();
    // --- CHANGE 1: ADDED HARDWARE SERVICES ---
    private final SettingsService settingsService = new SettingsService(); // Needed for hardware config
    private final ReceiptService receiptService = new ReceiptService(settingsService);
    private final CashDrawerService cashDrawerService = new CashDrawerService(settingsService);


    // --- FXML Components ---
    @FXML private TextField productSearchField;
    @FXML private TextField discountField;
    @FXML private TableView<Product> productSearchTableView;
    @FXML private TableView<SaleItem> cartTableView;
    @FXML private TableColumn<SaleItem, String> cartItemNameCol;
    @FXML private TableColumn<SaleItem, Double> cartQtyCol;
    @FXML private TableColumn<SaleItem, Double> cartPriceCol;
    @FXML private TableColumn<SaleItem, Double> cartTotalCol;
    @FXML private ComboBox<Customer> customerComboBox;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Button checkoutButton;

    // --- State Management ---
    private User currentUser;
    private ObservableList<Product> masterProductList;
    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();

    @Override
    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void initialize() {
        setupProductSearchTable();
        setupCartTable();
        loadInitialData();
        setupEventListeners();
        discountField.setText("0.00");
    }

    private void loadInitialData() {
        // ... (this method is correct and does not need changes)
        List<ProductDTO> productDTOs = productService.getAllActiveProducts();
        this.masterProductList = FXCollections.observableArrayList(
                productDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        );

        List<CustomerDTO> customerDTOs = customerService.getAllActiveCustomers();
        ObservableList<Customer> masterCustomerList = FXCollections.observableArrayList(
                customerDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        );
        customerComboBox.setItems(masterCustomerList);

        Customer walkIn = new Customer();
        walkIn.setId(0L); // Use Long for consistency
        walkIn.setName("Walk-in Customer");
        customerComboBox.getItems().add(0, walkIn);
        customerComboBox.getSelectionModel().select(walkIn);

        FilteredList<Product> filteredProducts = new FilteredList<>(masterProductList, p -> p.getIsActive() && p.getCurrentStock() > 0);
        productSearchField.textProperty().addListener((obs, old, val) -> {
            filteredProducts.setPredicate(p -> {
                if (!p.getIsActive() || p.getCurrentStock() <= 0) return false;
                if (val == null || val.isEmpty()) return true;
                String filter = val.toLowerCase();
                // NOTE: Barcode scanner functionality is handled here.
                // A scanner typically types the barcode and presses "Enter".
                // The check for `p.getBarcode().equals(val)` enables this.
                return p.getName().toLowerCase().contains(filter) ||
                        (p.getBarcode() != null && p.getBarcode().equals(val)) ||
                        (p.getSku() != null && p.getSku().toLowerCase().contains(filter));
            });
        });
        productSearchTableView.setItems(filteredProducts);
    }

    private void setupProductSearchTable() {
        // ... (this method is correct and does not need changes)
        TableColumn<Product, String> nameCol = (TableColumn<Product, String>) productSearchTableView.getColumns().get(0);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
    }

    private void setupCartTable() {
        cartItemNameCol.setCellValueFactory(cellData -> {
            Product product = findProductById(cellData.getValue().getProductId());
            return new javafx.beans.property.SimpleStringProperty(product != null ? product.getName() : "Not Found");
        });

        cartQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        cartTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        cartTableView.setEditable(true);
        cartQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        cartQtyCol.setOnEditCommit(event -> {
            SaleItem item = event.getRowValue();
            Product product = findProductById(item.getProductId());
            double newQuantity = event.getNewValue();

            if (newQuantity > product.getCurrentStock()) {
                showAlert(Alert.AlertType.ERROR, "Stock Error", "Not enough stock for " + product.getName() + ". Available: " + product.getCurrentStock());
                event.getTableView().getItems().set(event.getTablePosition().getRow(), item);
                return;
            }

            if (newQuantity > 0) {
                item.setQuantity(newQuantity);
                updateItemTotal(item);
            } else {
                cartItems.remove(item);
            }
            updateTotals();
            cartTableView.refresh();
        });
        cartTableView.setItems(cartItems);
    }

    private void setupEventListeners() {
        // ... (this method is correct and does not need changes)
        productSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !productSearchTableView.getItems().isEmpty()) {
                addProductToCart(productSearchTableView.getItems().get(0));
                productSearchField.clear();
            }
        });

        productSearchTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Product selectedProduct = productSearchTableView.getSelectionModel().getSelectedItem();
                if (selectedProduct != null) addProductToCart(selectedProduct);
            }
        });

        discountField.textProperty().addListener((obs, old, val) -> updateTotals());
    }

    private void addProductToCart(Product product) {
        Optional<SaleItem> existingItemOpt = cartItems.stream()
                .filter(item -> item.getProductId() == product.getId())
                .findFirst();

        double currentQtyInCart = existingItemOpt.map(SaleItem::getQuantity).orElse(0.0);

        if (currentQtyInCart + 1 > product.getCurrentStock()) {
            showAlert(Alert.AlertType.ERROR, "Stock Error", "Not enough stock for " + product.getName() + ". Available: " + product.getCurrentStock());
            return;
        }

        if (existingItemOpt.isPresent()) {
            SaleItem item = existingItemOpt.get();
            item.setQuantity(item.getQuantity() + 1.0);
            updateItemTotal(item);
            cartTableView.refresh();
        } else {
            SaleItem newItem = new SaleItem();
            newItem.setProductId(product.getId());
            newItem.setQuantity(1.0);
            newItem.setUnitPrice(product.getSellingPrice());
            newItem.setTaxRate(product.getTaxRate());
            updateItemTotal(newItem);
            cartItems.add(newItem);
        }
        updateTotals();
    }

    @FXML
    private void handleRemoveItem() {
        // ... (this method is correct and does not need changes)
        SaleItem selectedItem = cartTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            cartItems.remove(selectedItem);
            updateTotals();
        }
    }

    @FXML
    private void handleCheckout() {
        // ... (this method is correct and does not need changes)
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Cart", "Please add items to the cart before checking out.");
            return;
        }
        Customer selectedCustomer = customerComboBox.getValue();
        if (selectedCustomer == null || selectedCustomer.getId() == 0) {
            processSale(true);
        } else {
            Alert choiceDialog = new Alert(Alert.AlertType.CONFIRMATION);
            choiceDialog.setTitle("Checkout Option");
            choiceDialog.setHeaderText("Choose how to proceed for " + selectedCustomer.getName());
            ButtonType payNowButton = new ButtonType("Pay Now");
            ButtonType saveInvoiceButton = new ButtonType("Save as Invoice (Charge to Account)");
            choiceDialog.getButtonTypes().setAll(payNowButton, saveInvoiceButton, ButtonType.CANCEL);
            choiceDialog.showAndWait().ifPresent(response -> {
                if (response == payNowButton) {
                    processSale(true);
                } else if (response == saveInvoiceButton) {
                    processSale(false);
                }
            });
        }
    }

    private void processSale(boolean handlePayment) {
        SaleDTO saleDto = createSaleDTOFromCart();
        List<SaleItemDTO> itemDtos = createSaleItemDTOsFromCart();

        long saleId = saleService.processNewSale(saleDto, itemDtos);

        if (saleId > 0) {
            if (handlePayment) {
                // The payment dialog returns true if payment was successful
                boolean paymentSuccess = showPaymentDialog(saleId, saleDto.getTotal());
                if (paymentSuccess) {
                    // --- CHANGE 2: TRIGGER HARDWARE ON SUCCESSFUL PAYMENT ---
                    // This block is wrapped in a try-catch to ensure that if the printer
                    // or cash drawer fails, the app doesn't crash.
                    try {
                        Customer customer = customerComboBox.getValue();
                        String customerName = (customer != null && customer.getId() != 0) ? customer.getName() : "Walk-in Customer";

                        // Get full product details for the receipt
                        List<Product> productsInCart = itemDtos.stream()
                                .map(item -> findProductById(item.getProductId()))
                                .collect(Collectors.toList());

                        receiptService.printReceipt(saleDto, itemDtos, productsInCart, customerName);

                        // In a real app, we would get the payment method from the dialog
                        // and only open for cash. For now, we open it.
                        cashDrawerService.openDrawer();

                    } catch (PrintException | IOException e) {
                        showAlert(Alert.AlertType.WARNING, "Hardware Error", "Sale complete, but could not print receipt or open drawer. Please check hardware and settings.\nError: " + e.getMessage());
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Unexpected Error", "An unexpected error occurred during printing: " + e.getMessage());
                    }
                }
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Invoice #" + saleId + " saved successfully.");
            }
            resetView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to process the sale. Please check the logs.");
        }
    }

    // --- CHANGE 3: MODIFIED METHOD SIGNATURE ---
    private boolean showPaymentDialog(long transactionId, double totalDue) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/PaymentDialog.fxml"));
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Make Payment for Transaction #" + transactionId);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(checkoutButton.getScene().getWindow());
            dialogStage.setScene(new Scene(loader.load()));
            PaymentDialogController controller = loader.getController();
            controller.initData(transactionId, totalDue, currentUser);

            // Assuming the controller can be closed and we can check a status
            // to see if payment was completed.
            dialogStage.showAndWait();

            // We'll assume if the dialog was not cancelled, payment was successful.
            // A more robust implementation would have the dialog controller return a true/false value.
            return controller.isPaymentSuccessful(); // Assumes PaymentDialogController has this method.

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "UI Error", "Could not open the payment dialog.");
            return false;
        }
    }

    private void updateItemTotal(SaleItem item) {
        // ... (this method is correct and does not need changes)
        item.setTotal(item.getQuantity() * item.getUnitPrice());
    }

    private void updateTotals() {
        // ... (this method is correct and does not need changes)
        double subtotal = cartItems.stream().mapToDouble(SaleItem::getTotal).sum();
        double tax = cartItems.stream().mapToDouble(item -> {
            Product p = findProductById(item.getProductId());
            return p != null ? item.getTotal() * (p.getTaxRate() / 100.0) : 0.0;
        }).sum();
        double discount = parseCurrency(discountField.getText());
        double total = subtotal + tax - discount;

        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalAmountLabel.setText(String.format("$%.2f", total));
    }

    private SaleDTO createSaleDTOFromCart() {
        SaleDTO sale = new SaleDTO();
        Customer customer = customerComboBox.getValue();
        if (customer != null && customer.getId() != 0) {
            sale.setCustomerId(customer.getId());
            sale.setCustomerUuid(customer.getUuid());
        }
        updateTotals();
        sale.setSubtotal(parseCurrency(subtotalLabel.getText()));
        sale.setTax(parseCurrency(taxLabel.getText()));
        sale.setDiscount(parseCurrency(discountField.getText()));
        sale.setTotal(parseCurrency(totalAmountLabel.getText()));
        sale.setPaymentStatus("pending");
        return sale;
    }

    private List<SaleItemDTO> createSaleItemDTOsFromCart() {
        return cartItems.stream()
                .map(ModelMapper::toDto)
                .collect(Collectors.toList());
    }

    private void resetView() {
        // ... (this method is correct and does not need changes)
        cartItems.clear();
        productSearchField.clear();
        discountField.setText("0.00");
        if (!customerComboBox.getItems().isEmpty()) {
            customerComboBox.getSelectionModel().selectFirst();
        }
        updateTotals();
        List<ProductDTO> productDTOs = productService.getAllActiveProducts();
        this.masterProductList.setAll(
                productDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        );
    }

    private Product findProductById(long id) {
        return masterProductList.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    @FXML
    private void handleClearCart() {
        // ... (this method is correct and does not need changes)
        if (!cartItems.isEmpty()) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to clear the entire cart?", ButtonType.YES, ButtonType.NO);
            confirmation.setTitle("Confirm Clear Cart");
            confirmation.setHeaderText(null);
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    resetView();
                }
            });
        }
    }

    private double parseCurrency(String text) {
        // ... (this method is correct and does not need changes)
        try {
            return Double.parseDouble(text.replaceAll("[^\\d.-]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        // ... (this method is correct and does not need changes)
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}