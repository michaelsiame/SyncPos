// src/main/java/com/kmu/syncpos/models/SaleItem.java
package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * A UI Model (ViewModel) representing a single line item within a sale or purchase.
 * This is used to display data in the POS cart and Purchase screen tables.
 */
public class SaleItem {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final LongProperty saleId = new SimpleLongProperty();
    private final LongProperty productId = new SimpleLongProperty();
    private final StringProperty saleUuid = new SimpleStringProperty();
    private final StringProperty productUuid = new SimpleStringProperty();
    private final StringProperty supplierProductCode = new SimpleStringProperty();
    private final DoubleProperty quantity = new SimpleDoubleProperty();
    private final DoubleProperty unitPrice = new SimpleDoubleProperty();
    private final DoubleProperty costAtSale = new SimpleDoubleProperty();
    private final DoubleProperty taxRate = new SimpleDoubleProperty();
    private final DoubleProperty discount = new SimpleDoubleProperty();
    private final DoubleProperty total = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    /* ---------- getters ---------- */
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public long getSaleId() { return saleId.get(); }
    public long getProductId() { return productId.get(); }
    public String getSaleUuid() { return saleUuid.get(); }
    public String getProductUuid() { return productUuid.get(); }
    public String getSupplierProductCode() { return supplierProductCode.get(); }
    public double getQuantity() { return quantity.get(); }
    public double getUnitPrice() { return unitPrice.get(); }
    public double getCostAtSale() { return costAtSale.get(); }
    public double getTaxRate() { return taxRate.get(); }
    public double getDiscount() { return discount.get(); }
    public double getTotal() { return total.get(); }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean getIsDeleted() { return isDeleted.get(); }

    /* ---------- setters ---------- */
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setSaleId(long saleId) { this.saleId.set(saleId); }
    public void setProductId(long productId) { this.productId.set(productId); }
    public void setSaleUuid(String saleUuid) { this.saleUuid.set(saleUuid); }
    public void setProductUuid(String productUuid) { this.productUuid.set(productUuid); }
    public void setSupplierProductCode(String code) { this.supplierProductCode.set(code); }
    public void setQuantity(double quantity) { this.quantity.set(quantity); }
    public void setUnitPrice(double unitPrice) { this.unitPrice.set(unitPrice); }
    public void setCostAtSale(double costAtSale) { this.costAtSale.set(costAtSale); }
    public void setTaxRate(double taxRate) { this.taxRate.set(taxRate); }
    public void setDiscount(double discount) { this.discount.set(discount); }
    public void setTotal(double total) { this.total.set(total); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }

    /* ---------- property getters ---------- */
    public LongProperty idProperty() { return id; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty tenantIdProperty() { return tenantId; }
    public LongProperty saleIdProperty() { return saleId; }
    public LongProperty productIdProperty() { return productId; }
    public StringProperty saleUuidProperty() { return saleUuid; }
    public StringProperty productUuidProperty() { return productUuid; }
    public StringProperty supplierProductCodeProperty() { return supplierProductCode; }
    public DoubleProperty quantityProperty() { return quantity; }
    public DoubleProperty unitPriceProperty() { return unitPrice; }
    public DoubleProperty costAtSaleProperty() { return costAtSale; }
    public DoubleProperty taxRateProperty() { return taxRate; }
    public DoubleProperty discountProperty() { return discount; }
    public DoubleProperty totalProperty() { return total; }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
    public ObjectProperty<LocalDateTime> lastUpdatedAtProperty() { return lastUpdatedAt; }
    public IntegerProperty isSyncedProperty() { return isSynced; }
    public BooleanProperty isDeletedProperty() { return isDeleted; }
}