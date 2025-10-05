// src/main/java/com/kmu/syncpos/models/Sale.java
package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Sale {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final LongProperty userId = new SimpleLongProperty();

    // Corrected: Use ObjectProperty for nullable foreign keys to prevent NPEs.
    private final ObjectProperty<Long> customerId = new SimpleObjectProperty<>();
    private final ObjectProperty<Long> supplierId = new SimpleObjectProperty<>();

    // Added: UUIDs for data parity with DTO.
    private final StringProperty customerUuid = new SimpleStringProperty();
    private final StringProperty supplierUuid = new SimpleStringProperty();

    private final DoubleProperty subtotal = new SimpleDoubleProperty();
    private final DoubleProperty tax = new SimpleDoubleProperty();
    private final DoubleProperty discount = new SimpleDoubleProperty();
    private final DoubleProperty total = new SimpleDoubleProperty();
    private final StringProperty paymentMethod = new SimpleStringProperty();
    private final StringProperty paymentStatus = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    /* ---------- getters ---------- */
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public String getType() { return type.get(); }
    public long getUserId() { return userId.get(); }
    public Long getCustomerId() { return customerId.get(); } // Returns Long
    public Long getSupplierId() { return supplierId.get(); } // Returns Long
    public String getCustomerUuid() { return customerUuid.get(); }
    public String getSupplierUuid() { return supplierUuid.get(); }
    public double getSubtotal() { return subtotal.get(); }
    public double getTax() { return tax.get(); }
    public double getDiscount() { return discount.get(); }
    public double getTotal() { return total.get(); }
    public String getPaymentMethod() { return paymentMethod.get(); }
    public String getPaymentStatus() { return paymentStatus.get(); }
    public String getNotes() { return notes.get(); }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean getIsDeleted() { return isDeleted.get(); }

    /* ---------- setters ---------- */
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setType(String type) { this.type.set(type); }
    public void setUserId(long userId) { this.userId.set(userId); }
    public void setCustomerId(Long customerId) { this.customerId.set(customerId); } // Takes Long
    public void setSupplierId(Long supplierId) { this.supplierId.set(supplierId); } // Takes Long
    public void setCustomerUuid(String customerUuid) { this.customerUuid.set(customerUuid); }
    public void setSupplierUuid(String supplierUuid) { this.supplierUuid.set(supplierUuid); }
    public void setSubtotal(double subtotal) { this.subtotal.set(subtotal); }
    public void setTax(double tax) { this.tax.set(tax); }
    public void setDiscount(double discount) { this.discount.set(discount); }
    public void setTotal(double total) { this.total.set(total); }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod.set(paymentMethod); }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus.set(paymentStatus); }
    public void setNotes(String notes) { this.notes.set(notes); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }

    /* ---------- property getters ---------- */
    public LongProperty idProperty() { return id; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty tenantIdProperty() { return tenantId; }
    public StringProperty typeProperty() { return type; }
    public LongProperty userIdProperty() { return userId; }
    public ObjectProperty<Long> customerIdProperty() { return customerId; }
    public ObjectProperty<Long> supplierIdProperty() { return supplierId; }
    public DoubleProperty subtotalProperty() { return subtotal; }
    public DoubleProperty taxProperty() { return tax; }
    public DoubleProperty discountProperty() { return discount; }
    public DoubleProperty totalProperty() { return total; }
    public StringProperty paymentMethodProperty() { return paymentMethod; }
    public StringProperty paymentStatusProperty() { return paymentStatus; }
    public StringProperty notesProperty() { return notes; }
    public ObjectProperty<java.time.LocalDateTime> createdAtProperty() { return createdAt; }
    public ObjectProperty<java.time.LocalDateTime> lastUpdatedAtProperty() { return lastUpdatedAt; }
    public IntegerProperty isSyncedProperty() { return isSynced; }
    public BooleanProperty isDeletedProperty() { return isDeleted; }
}