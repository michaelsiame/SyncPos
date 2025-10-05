package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Product {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final StringProperty sku = new SimpleStringProperty();
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty productType = new SimpleStringProperty();

    // Corrected: Use ObjectProperty for nullable foreign keys to prevent NPEs.
    private final ObjectProperty<Long> categoryId = new SimpleObjectProperty<>();
    private final ObjectProperty<Long> unitId = new SimpleObjectProperty<>();
    private final ObjectProperty<Long> supplierId = new SimpleObjectProperty<>();

    // Added: UUID fields for data parity with DTO to support sync logic.
    private final StringProperty categoryUuid = new SimpleStringProperty();
    private final StringProperty unitUuid = new SimpleStringProperty();
    private final StringProperty supplierUuid = new SimpleStringProperty();

    private final DoubleProperty purchasePrice = new SimpleDoubleProperty();
    private final DoubleProperty sellingPrice = new SimpleDoubleProperty();
    private final DoubleProperty taxRate = new SimpleDoubleProperty();
    private final DoubleProperty minStockLevel = new SimpleDoubleProperty();
    private final DoubleProperty reorderQuantity = new SimpleDoubleProperty();
    private final BooleanProperty isActive = new SimpleBooleanProperty();
    private final ObjectProperty<LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    /* runtime-only helper */
    private final DoubleProperty currentStock = new SimpleDoubleProperty();

    /* ---------- getters ---------- */
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public String getSku() { return sku.get(); }
    public String getBarcode() { return barcode.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }
    public String getProductType() { return productType.get(); }
    public Long getCategoryId() { return categoryId.get(); } // Returns Long
    public Long getUnitId() { return unitId.get(); }       // Returns Long
    public Long getSupplierId() { return supplierId.get(); }   // Returns Long
    public String getCategoryUuid() { return categoryUuid.get(); }
    public String getUnitUuid() { return unitUuid.get(); }
    public String getSupplierUuid() { return supplierUuid.get(); }
    public double getPurchasePrice() { return purchasePrice.get(); }
    public double getSellingPrice() { return sellingPrice.get(); }
    public double getTaxRate() { return taxRate.get(); }
    public double getMinStockLevel() { return minStockLevel.get(); }
    public double getReorderQuantity() {return reorderQuantity.get(); }
    public boolean getIsActive() { return isActive.get(); }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean getIsDeleted() { return isDeleted.get(); }
    public double getCurrentStock() { return currentStock.get(); }

    /* ---------- setters ---------- */
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setSku(String sku) { this.sku.set(sku); }
    public void setBarcode(String barcode) { this.barcode.set(barcode); }
    public void setName(String name) { this.name.set(name); }
    public void setDescription(String description) { this.description.set(description); }
    public void setProductType(String productType) { this.productType.set(productType); }
    public void setCategoryId(Long categoryId) { this.categoryId.set(categoryId); } // Takes Long
    public void setUnitId(Long unitId) { this.unitId.set(unitId); }       // Takes Long
    public void setSupplierId(Long supplierId) { this.supplierId.set(supplierId); }   // Takes Long
    public void setCategoryUuid(String categoryUuid) { this.categoryUuid.set(categoryUuid); }
    public void setUnitUuid(String unitUuid) { this.unitUuid.set(unitUuid); }
    public void setSupplierUuid(String supplierUuid) { this.supplierUuid.set(supplierUuid); }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice.set(purchasePrice); }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice.set(sellingPrice); }
    public void setTaxRate(double taxRate) { this.taxRate.set(taxRate); }
    public void setMinStockLevel(double minStockLevel) { this.minStockLevel.set(minStockLevel); }
    public void setReorderQuantity(double reorderQuantity){this.reorderQuantity.set(reorderQuantity);}
    public void setIsActive(boolean isActive) { this.isActive.set(isActive); }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }
    public void setCurrentStock(double currentStock) { this.currentStock.set(currentStock); }

    /* ---------- property getters ---------- */
    public LongProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Long> categoryIdProperty() { return categoryId; }
    public ObjectProperty<Long> unitIdProperty() { return unitId; }
    public ObjectProperty<Long> supplierIdProperty() { return supplierId; }
    public DoubleProperty sellingPriceProperty() { return sellingPrice; }
    public DoubleProperty currentStockProperty() { return currentStock; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty tenantIdProperty() { return tenantId; }
    public StringProperty skuProperty() { return sku; }
    public StringProperty barcodeProperty() { return barcode; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty productTypeProperty() { return productType; }
    public DoubleProperty purchasePriceProperty() { return purchasePrice; }
    public DoubleProperty taxRateProperty() { return taxRate; }
    public DoubleProperty minStockLevelProperty() { return minStockLevel; }
    public DoubleProperty reorderQuantityProperty(){return reorderQuantity;}
    public BooleanProperty isActiveProperty() { return isActive; }
    public ObjectProperty<java.time.LocalDateTime> lastUpdatedAtProperty() { return lastUpdatedAt; }
    public IntegerProperty isSyncedProperty() { return isSynced; }
    public BooleanProperty isDeletedProperty() { return isDeleted; }
}