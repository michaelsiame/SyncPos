// src/main/java/com/kmu/syncpos/controllers/PurchaseController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.*;
import com.kmu.syncpos.service.*;
import com.kmu.syncpos.util.ModelMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class PurchaseController implements UserAware {

    // --- Services ---
    private final ProductService productService = new ProductService();
    private final SupplierService supplierService = new SupplierService();
    private final PurchaseService purchaseService = new PurchaseService();

    // --- FXML Components ---
    @FXML private TextField productSearchField, referenceField;
    @FXML private TextArea notesArea;
    @FXML private TableView<Product> productSearchTableView;
    @FXML private TableColumn<Product, String> searchProductNameCol;
    @FXML private TableColumn<Product, Double> searchProductStockCol;
    @FXML private TableView<SaleItem> purchaseTableView;
    @FXML private TableColumn<SaleItem, String> purchaseItemNameCol;
    @FXML private TableColumn<SaleItem, Double> purchaseQtyCol;
    @FXML private TableColumn<SaleItem, Double> purchasePriceCol;
    @FXML private TableColumn<SaleItem, Double> purchaseTotalCol;
    @FXML private ComboBox<Supplier> supplierComboBox;
    @FXML private ComboBox<String> paymentStatusComboBox; // NEW: Added FXML component
    @FXML private DatePicker purchaseDatePicker;
    @FXML private Label totalItemsLabel, grandTotalLabel;
    @FXML private Button removeItemButton, completePurchaseButton;

    // --- State ---
    private User currentUser;
    private ObservableList<Product> masterProductList;
    private final ObservableList<SaleItem> purchaseItems = FXCollections.observableArrayList();

    @Override
    public void setUser(User user) {
        this.currentUser = user;
        // Data loading should happen after user context is set
        loadInitialData();
    }

    @FXML
    public void initialize() {
        setupProductSearchTable();
        setupPurchaseTable();
        setupControls(); // MODIFIED: Centralized control setup
        setupEventListeners();
        purchaseDatePicker.setValue(LocalDate.now());
    }

    private void setupProductSearchTable() {
        searchProductNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        searchProductStockCol.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
    }

    private void setupPurchaseTable() {
        purchaseItemNameCol.setCellValueFactory(cellData -> {
            Product product = findProductById(cellData.getValue().getProductId());
            return new javafx.beans.property.SimpleStringProperty(product != null ? product.getName() : "Loading...");
        });

        purchaseQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        purchasePriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        purchaseTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        purchaseTableView.setEditable(true);

        purchaseQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        purchaseQtyCol.setOnEditCommit(event -> {
            SaleItem item = event.getRowValue();
            item.setQuantity(event.getNewValue());
            updateItemTotal(item);
            updateGrandTotal();
        });

        purchasePriceCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        purchasePriceCol.setOnEditCommit(event -> {
            SaleItem item = event.getRowValue();
            item.setUnitPrice(event.getNewValue());
            updateItemTotal(item);
            updateGrandTotal();
        });
        purchaseTableView.setItems(purchaseItems);
    }

    // NEW: Central method to set up interactive controls
    private void setupControls() {
        // FIX: Tell the supplier ComboBox how to display a Supplier object
        supplierComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Supplier supplier) {
                // If the supplier is null, show nothing. Otherwise, show its name.
                return supplier == null ? "" : supplier.getName();
            }

            @Override
            public Supplier fromString(String string) {
                return null; // Not needed for a non-editable ComboBox
            }
        });

        // NEW: Populate and set a default for the payment status
        paymentStatusComboBox.getItems().addAll("On Invoice", "Paid");
        paymentStatusComboBox.setValue("On Invoice"); // Default value
    }

    private void loadInitialData() {
        if (currentUser == null) return; // Don't load if no user is set

        // Load products
        List<ProductDTO> productDTOs = productService.getAllActiveProducts();
        this.masterProductList = FXCollections.observableArrayList(
                productDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        );

        // Load suppliers
        List<SupplierDTO> supplierDTOs = supplierService.getAllActiveSuppliers();
        ObservableList<Supplier> supplierList = FXCollections.observableArrayList(
                supplierDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        );
        supplierComboBox.setItems(supplierList);

        // Setup product search filter
        FilteredList<Product> filteredProducts = new FilteredList<>(masterProductList, p -> true);
        productSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredProducts.setPredicate(product -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                return product.getName().toLowerCase().contains(filter) ||
                        (product.getSku() != null && product.getSku().toLowerCase().contains(filter));
            });
        });
        productSearchTableView.setItems(filteredProducts);
    }

    private void setupEventListeners() {
        productSearchTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Product selectedProduct = productSearchTableView.getSelectionModel().getSelectedItem();
                if (selectedProduct != null) {
                    addProductToPurchase(selectedProduct);
                }
            }
        });
    }

    private void addProductToPurchase(Product product) {
        for (SaleItem item : purchaseItems) {
            if (item.getProductId() == product.getId()) {
                item.setQuantity(item.getQuantity() + 1.0);
                updateItemTotal(item);
                updateGrandTotal();
                purchaseTableView.refresh();
                return;
            }
        }
        SaleItem newItem = new SaleItem();
        newItem.setProductId(product.getId());
        newItem.setQuantity(1.0);
        newItem.setUnitPrice(product.getPurchasePrice());
        newItem.setTaxRate(product.getTaxRate());
        updateItemTotal(newItem);
        purchaseItems.add(newItem);
        updateGrandTotal();
    }

    @FXML
    private void handleRemoveItem() {
        SaleItem selectedItem = purchaseTableView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            purchaseItems.remove(selectedItem);
            updateGrandTotal();
        }
    }

    @FXML
    private void handleCompletePurchase() {
        if (supplierComboBox.getValue() == null || purchaseItems.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please select a supplier and add items to the purchase list.");
            return;
        }

        SaleDTO purchaseDto = createPurchaseDTO();
        List<SaleItemDTO> itemDtos = createSaleItemDTOs();

        boolean success = purchaseService.processNewPurchase(purchaseDto, itemDtos);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Purchase recorded successfully. Stock has been updated.");
            resetView();
        } else {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to record purchase. Please check the logs.");
        }
    }

    private SaleDTO createPurchaseDTO() {
        SaleDTO purchaseDto = new SaleDTO();
        Supplier selectedSupplier = supplierComboBox.getValue();

        purchaseDto.setSupplierId(selectedSupplier.getId());
        purchaseDto.setSupplierUuid(selectedSupplier.getUuid());

        purchaseDto.setNotes(notesArea.getText() + " | Ref: " + referenceField.getText());
        double total = purchaseItems.stream().mapToDouble(SaleItem::getTotal).sum();
        purchaseDto.setSubtotal(total);
        purchaseDto.setTotal(total);
        purchaseDto.setTax(0.0);
        purchaseDto.setDiscount(0.0);

        // FIX: Get status from the ComboBox and map to database value
        String selectedStatus = paymentStatusComboBox.getValue();
        String dbStatus = "On Invoice".equals(selectedStatus) ? "pending" : "paid";
        purchaseDto.setPaymentStatus(dbStatus);

        return purchaseDto;
    }

    private List<SaleItemDTO> createSaleItemDTOs() {
        return purchaseItems.stream()
                .map(ModelMapper::toDto)
                .collect(Collectors.toList());
    }

    private void resetView() {
        purchaseItems.clear();
        supplierComboBox.getSelectionModel().clearSelection();
        purchaseDatePicker.setValue(LocalDate.now());
        referenceField.clear();
        notesArea.clear();

        // NEW: Reset the payment status combo box to its default
        paymentStatusComboBox.setValue("On Invoice");

        updateGrandTotal();

        // Refresh product list in case stock levels changed
        List<ProductDTO> productDTOs = productService.getAllActiveProducts();
        masterProductList.setAll(
                productDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        );
    }

    private void updateItemTotal(SaleItem item) {
        item.setTotal(item.getQuantity() * item.getUnitPrice());
    }

    private void updateGrandTotal() {
        totalItemsLabel.setText(String.valueOf(purchaseItems.size()));
        double total = purchaseItems.stream().mapToDouble(SaleItem::getTotal).sum();
        grandTotalLabel.setText(String.format("$%.2f", total));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Product findProductById(long id) {
        if (masterProductList == null) return null;
        return masterProductList.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }
}