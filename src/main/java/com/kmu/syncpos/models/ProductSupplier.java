// src/main/java/com/kmu/syncpos/models/ProductSupplier.java
package com.kmu.syncpos.models;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A UI Model (ViewModel) representing the link between a Product and a Supplier.
 * This is used to display data in the Product Management screen's associated suppliers table.
 */
public class ProductSupplier {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final LongProperty productId = new SimpleLongProperty();
    private final LongProperty supplierId = new SimpleLongProperty();

    // These fields are for display purposes in the UI table and are not part of the core DTO.
    private final StringProperty supplierName = new SimpleStringProperty();
    private final StringProperty supplierProductCode = new SimpleStringProperty();

    // --- Getters ---
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public long getProductId() { return productId.get(); }
    public long getSupplierId() { return supplierId.get(); }
    public String getSupplierName() { return supplierName.get(); }
    public String getSupplierProductCode() { return supplierProductCode.get(); }

    // --- Setters ---
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setProductId(long productId) { this.productId.set(productId); }
    public void setSupplierId(long supplierId) { this.supplierId.set(supplierId); }
    public void setSupplierName(String name) { this.supplierName.set(name); }
    public void setSupplierProductCode(String code) { this.supplierProductCode.set(code); }

    // --- Property Getters for TableView ---
    public LongProperty idProperty() { return id; }
    public StringProperty supplierNameProperty() { return supplierName; }
    public StringProperty supplierProductCodeProperty() { return supplierProductCode; }
}