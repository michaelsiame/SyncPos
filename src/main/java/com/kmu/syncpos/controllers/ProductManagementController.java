// src/main/java/com/kmu/syncpos/controllers/ProductManagementController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.*;
import com.kmu.syncpos.service.*;
import com.kmu.syncpos.service.form.ProductFormDependencies;
import com.kmu.syncpos.util.ModelMapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProductManagementController {

    // --- Services ---
    private final ProductService productService = new ProductService();
    private final StockLedgerService stockLedgerService = new StockLedgerService();
    private final SupplierService supplierService = new SupplierService();
    private final ProductSupplierService productSupplierService = new ProductSupplierService();

    // --- FXML Components: Main ---
    @FXML private TextField searchField;
    @FXML private TableView<Product> productTableView;
    @FXML private TableColumn<Product, String> skuCol, nameCol;
    @FXML private TableColumn<Product, Long> categoryCol;
    @FXML private TableColumn<Product, Double> sellingPriceCol, stockCol;
    @FXML private TableColumn<Product, Double> purchasePriceCol; // Corrected type
    @FXML private TableColumn<Product, Boolean> activeCol;
    @FXML private Button newButton, saveButton, deleteButton;

    // --- FXML Components: Details Tabs ---
    @FXML private TextField nameField, skuField, barcodeField, initialStockField;
    @FXML private TextField sellingPriceField, purchasePriceField, taxRateField, minStockField, reorderQtyField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private ComboBox<Unit> unitComboBox;
    @FXML private ComboBox<String> productTypeComboBox;
    @FXML private ComboBox<Supplier> supplierComboBox;
    @FXML private CheckBox activeCheckBox;

    // --- FXML Components: History Tab ---
    @FXML private TableView<StockLedgerView> inventoryHistoryTableView;
    @FXML private TableColumn<StockLedgerView, LocalDateTime> invDateCol;
    @FXML private TableColumn<StockLedgerView, Double> invQtyCol;
    @FXML private TableColumn<StockLedgerView, String> invReasonCol, invUserCol;
    @FXML private TableView<?> priceHistoryTableView; // Placeholder
    @FXML private TableColumn<?, ?> priceDateCol, priceOldCol, priceNewCol, priceUserCol; // Placeholders

    // --- FXML Components: Suppliers Tab ---
    @FXML private TableView<ProductSupplier> productSuppliersTableView;
    @FXML private TableColumn<ProductSupplier, String> supplierNameCol, supplierCodeCol;
    @FXML private Button addSupplierLinkButton, editSupplierLinkButton, removeSupplierLinkButton;

    // --- State ---
    private ObservableList<Product> masterProductList;
    private Product selectedProduct;
    private ObservableList<StockLedgerView> inventoryHistoryList = FXCollections.observableArrayList();
    private ObservableList<ProductSupplier> productSuppliersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupInventoryHistoryTable();
        setupProductSuppliersTable();
        populateComboBoxes();
        loadAndSetupTable();
        setupEventListeners();
        clearForm();
    }

    private void setupTableColumns() {
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        sellingPriceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        purchasePriceCol.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        stockCol.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("isActive"));
    }

    private void setupInventoryHistoryTable() {
        invDateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        invQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantityDelta"));
        invReasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        invUserCol.setCellValueFactory(new PropertyValueFactory<>("userId")); // Should ideally be a username
        inventoryHistoryTableView.setItems(inventoryHistoryList);
    }

    private void setupProductSuppliersTable() {
        supplierNameCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        supplierCodeCol.setCellValueFactory(new PropertyValueFactory<>("supplierProductCode"));
        productSuppliersTableView.setItems(productSuppliersList);
    }

    private void populateComboBoxes() {
        ProductFormDependencies deps = productService.getFormDependencies();
        categoryComboBox.setItems(FXCollections.observableArrayList(
                deps.getCategories().stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        ));
        unitComboBox.setItems(FXCollections.observableArrayList(
                deps.getUnits().stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        ));
        supplierComboBox.setItems(FXCollections.observableArrayList(
                deps.getSuppliers().stream().map(ModelMapper::fromDto).collect(Collectors.toList())
        ));
        productTypeComboBox.getItems().addAll("PHYSICAL", "SERVICE");
    }

    private void loadAndSetupTable() {
        List<ProductDTO> productDTOs = productService.getAllActiveProducts();
        List<Product> viewModels = productDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList());
        masterProductList = FXCollections.observableArrayList(viewModels);

        FilteredList<Product> filteredData = new FilteredList<>(masterProductList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(product -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                if (product.getName().toLowerCase().contains(filter)) return true;
                if (product.getSku() != null && product.getSku().toLowerCase().contains(filter)) return true;
                return product.getBarcode() != null && product.getBarcode().toLowerCase().contains(filter);
            });
        });

        SortedList<Product> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(productTableView.comparatorProperty());
        productTableView.setItems(sortedData);
    }

    private void setupEventListeners() {
        productTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> populateForm(newVal)
        );
        deleteButton.disableProperty().bind(productTableView.getSelectionModel().selectedItemProperty().isNull());
        addSupplierLinkButton.disableProperty().bind(productTableView.getSelectionModel().selectedItemProperty().isNull());
        editSupplierLinkButton.disableProperty().bind(productSuppliersTableView.getSelectionModel().selectedItemProperty().isNull());
        removeSupplierLinkButton.disableProperty().bind(productSuppliersTableView.getSelectionModel().selectedItemProperty().isNull());
    }

    private void refreshTableData() {
        List<ProductDTO> productDTOs = productService.getAllActiveProducts();
        List<Product> viewModels = productDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList());
        masterProductList.setAll(viewModels);
        clearForm();
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        ProductDTO dto;
        if (selectedProduct != null) {
            dto = ModelMapper.toDto(selectedProduct);
        } else {
            dto = new ProductDTO();
        }

        try {
            java.util.function.Function<String, Double> parseDoubleOrNull = (text) ->
                    text.trim().isEmpty() ? null : Double.parseDouble(text.trim());

            // For string fields, trim and set.
            dto.setName(nameField.getText().trim());
            dto.setSku(skuField.getText().trim());
            dto.setBarcode(barcodeField.getText().trim());
            dto.setDescription(descriptionArea.getText().trim());
            dto.setActive(activeCheckBox.isSelected());
            dto.setProductType(productTypeComboBox.getValue());

            // For floating-point fields, parse safely.
            dto.setSellingPrice(parseDoubleOrNull.apply(sellingPriceField.getText()));
            dto.setPurchasePrice(parseDoubleOrNull.apply(purchasePriceField.getText()));
            dto.setTaxRate(parseDoubleOrNull.apply(taxRateField.getText()));

            // For integer fields, parse as a double first to handle inputs like "10.0",
            // then cast to int. Also handle empty fields.
            String minStockText = minStockField.getText().trim();
            dto.setMinStockLevel(minStockText.isEmpty() ? null : Double.parseDouble(minStockText));

            String reorderQtyText = reorderQtyField.getText().trim();
            dto.setReorderQuantity(reorderQtyText.isEmpty() ? null : Double.parseDouble(reorderQtyText));

            // For new products, set the initial stock.
            if (dto.getId() == 0) {
                dto.setCurrentStock(parseDoubleOrNull.apply(initialStockField.getText()));
            }

            // --- FIX END ---

            // Relational fields
            Category category = categoryComboBox.getValue();
            if (category != null) {
                dto.setCategoryId(category.getId());
                dto.setCategoryUuid(category.getUuid());
            }
            Unit unit = unitComboBox.getValue();
            if (unit != null) {
                dto.setUnitId(unit.getId());
                dto.setUnitUuid(unit.getUuid());
            }
            Supplier supplier = supplierComboBox.getValue();
            if (supplier != null) {
                dto.setSupplierId(supplier.getId());
                dto.setSupplierUuid(supplier.getUuid());
            } else {
                dto.setSupplierId(null);
                dto.setSupplierUuid(null);
            }

        } catch(NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Number", "Please enter valid numbers for pricing and stock fields. For example: 10.50 or 5.");
            return;
        } catch(Exception e) {
            showAlert(Alert.AlertType.ERROR, "Form Error", "There was an error reading form data. Please check all fields.");
            return;
        }

        boolean success = productService.saveProduct(dto);
        if (success) {
            refreshTableData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not save the product. Please check the logs for details.");
        }
    }
    @FXML
    private void handleNew() {
        clearForm();
    }

    @FXML
    private void handleDelete() {
        if (selectedProduct == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deactivation");
        alert.setHeaderText("Deactivate Product: " + selectedProduct.getName());
        alert.setContentText("This will set the product as inactive. Are you sure?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            productService.deactivateProduct(selectedProduct.getId());
            refreshTableData();
        }
    }

    private void populateForm(Product product) {
        selectedProduct = product;
        if (product != null) {
            nameField.setText(product.getName());
            skuField.setText(product.getSku());
            barcodeField.setText(product.getBarcode());
            descriptionArea.setText(product.getDescription());
            sellingPriceField.setText(String.format("%.2f", product.getSellingPrice()));
            purchasePriceField.setText(String.format("%.2f", product.getPurchasePrice()));
            taxRateField.setText(String.valueOf(product.getTaxRate()));
            minStockField.setText(String.valueOf(product.getMinStockLevel()));
            reorderQtyField.setText(String.valueOf(product.getReorderQuantity()));
            activeCheckBox.setSelected(product.getIsActive());
            productTypeComboBox.setValue(product.getProductType());
            initialStockField.setText(String.valueOf(product.getCurrentStock()));
            initialStockField.setDisable(true);

            categoryComboBox.getSelectionModel().select(
                    categoryComboBox.getItems().stream().filter(c -> c.getId() == product.getCategoryId()).findFirst().orElse(null)
            );
            unitComboBox.getSelectionModel().select(
                    unitComboBox.getItems().stream().filter(u -> u.getId() == product.getUnitId()).findFirst().orElse(null)
            );
            supplierComboBox.getSelectionModel().select(
                    supplierComboBox.getItems().stream().filter(s -> product.getSupplierId() != null && s.getId() == product.getSupplierId()).findFirst().orElse(null)
            );

            loadInventoryHistory(product.getId());
            loadProductSuppliers(product.getId());
        } else {
            clearForm();
        }
    }

    private void clearForm() {
        selectedProduct = null;
        productTableView.getSelectionModel().clearSelection();
        nameField.clear();
        skuField.clear();
        barcodeField.clear();
        descriptionArea.clear();
        sellingPriceField.setText("0.00");
        purchasePriceField.setText("0.00");
        taxRateField.setText("0.0");
        minStockField.setText("0");
        reorderQtyField.setText("0");
        initialStockField.setText("0.0");
        initialStockField.setDisable(false);
        categoryComboBox.getSelectionModel().clearSelection();
        unitComboBox.getSelectionModel().clearSelection();
        productTypeComboBox.getSelectionModel().select("PHYSICAL");
        supplierComboBox.getSelectionModel().clearSelection();
        activeCheckBox.setSelected(true);
        inventoryHistoryList.clear();
        productSuppliersList.clear();
        nameField.requestFocus();
    }

    private void loadInventoryHistory(long productId) {
        List<StockLedgerDTO> dtos = stockLedgerService.getEntriesForProduct(productId);
        List<StockLedgerView> viewModels = dtos.stream()
                .map(dto -> new StockLedgerView(dto.getCreatedAt().toLocalDateTime(), dto.getQuantityDelta(), dto.getReason(), String.valueOf(dto.getUserId())))
                .collect(Collectors.toList());
        inventoryHistoryList.setAll(viewModels);
    }

    private void loadProductSuppliers(long productId) {
        productSuppliersList.clear();
        List<ProductSupplierDTO> dtos = productSupplierService.getSuppliersForProduct(productId);
        List<SupplierDTO> allSuppliers = supplierService.getAllActiveSuppliers();
        List<ProductSupplier> models = dtos.stream().map(dto -> {
            ProductSupplier model = ModelMapper.fromDto(dto);
            allSuppliers.stream()
                    .filter(s -> s.getId() == model.getSupplierId())
                    .findFirst()
                    .ifPresent(s -> model.setSupplierName(s.getName()));
            return model;
        }).collect(Collectors.toList());
        productSuppliersList.setAll(models);
    }

    @FXML
    private void handleAddSupplierLink() {
        if (selectedProduct == null) return;
        List<Long> existingSupplierIds = productSuppliersList.stream().map(ProductSupplier::getSupplierId).collect(Collectors.toList());
        List<SupplierDTO> availableSuppliers = supplierService.getAllActiveSuppliers().stream()
                .filter(s -> !existingSupplierIds.contains(s.getId()))
                .collect(Collectors.toList());

        if (availableSuppliers.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Suppliers", "All available suppliers are already linked to this product.");
            return;
        }

        Dialog<ProductSupplierDTO> dialog = new Dialog<>();
        dialog.setTitle("Add Supplier to Product");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        ComboBox<SupplierDTO> supplierComboBoxDialog = new ComboBox<>(FXCollections.observableArrayList(availableSuppliers));
        supplierComboBoxDialog.setConverter(new StringConverter<>() {
            @Override public String toString(SupplierDTO s) { return s != null ? s.getName() : ""; }
            @Override public SupplierDTO fromString(String s) { return null; }
        });
        TextField codeField = new TextField();
        codeField.setPromptText("Supplier's Part Number / SKU");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Supplier:"), 0, 0);
        grid.add(supplierComboBoxDialog, 1, 0);
        grid.add(new Label("Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && supplierComboBoxDialog.getValue() != null) {
                ProductSupplierDTO dto = new ProductSupplierDTO();
                dto.setProductId(selectedProduct.getId());
                dto.setSupplierId(supplierComboBoxDialog.getValue().getId());
                dto.setProductUuid(selectedProduct.getUuid());
                dto.setSupplierUuid(supplierComboBoxDialog.getValue().getUuid());
                dto.setSupplierProductCode(codeField.getText());
                return dto;
            }
            return null;
        });

        Optional<ProductSupplierDTO> result = dialog.showAndWait();
        result.ifPresent(dto -> {
            productSupplierService.saveProductSupplierLink(dto);
            loadProductSuppliers(selectedProduct.getId());
        });
    }

    @FXML
    private void handleEditSupplierLink() {
        ProductSupplier selectedLink = productSuppliersTableView.getSelectionModel().getSelectedItem();
        if (selectedLink == null) return;
        TextInputDialog dialog = new TextInputDialog(selectedLink.getSupplierProductCode());
        dialog.setTitle("Edit Supplier Code");
        dialog.setHeaderText("Editing code for supplier: " + selectedLink.getSupplierName());
        dialog.setContentText("Please enter the new code:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newCode -> {
            ProductSupplierDTO dto = ModelMapper.toDto(selectedLink);
            dto.setSupplierProductCode(newCode);
            productSupplierService.saveProductSupplierLink(dto);
            loadProductSuppliers(selectedProduct.getId());
        });
    }

    @FXML
    private void handleRemoveSupplierLink() {
        ProductSupplier selectedLink = productSuppliersTableView.getSelectionModel().getSelectedItem();
        if (selectedLink == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Removal");
        confirmation.setHeaderText("Remove link to " + selectedLink.getSupplierName() + "?");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            productSupplierService.deleteProductSupplierLink(selectedLink.getId());
            loadProductSuppliers(selectedProduct.getId());
        }
    }

    private boolean validateForm() {
        // Simple validation, can be enhanced
        return !nameField.getText().trim().isEmpty() &&
                !sellingPriceField.getText().trim().isEmpty() &&
                categoryComboBox.getValue() != null &&
                unitComboBox.getValue() != null;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class StockLedgerView {
        private final SimpleObjectProperty<LocalDateTime> createdAt;
        private final SimpleDoubleProperty quantityDelta;
        private final SimpleStringProperty reason;
        private final SimpleStringProperty userId;

        public StockLedgerView(LocalDateTime createdAt, Double quantityDelta, String reason, String userId) {
            this.createdAt = new SimpleObjectProperty<>(createdAt);
            this.quantityDelta = new SimpleDoubleProperty(quantityDelta);
            this.reason = new SimpleStringProperty(reason);
            this.userId = new SimpleStringProperty(userId);
        }

        public LocalDateTime getCreatedAt() { return createdAt.get(); }
        public Double getQuantityDelta() { return quantityDelta.get(); }
        public String getReason() { return reason.get(); }
        public String getUserId() { return userId.get(); }
    }
}