// src/main/java/com/kmu/syncpos/models/Payment.java
package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Payment {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final LongProperty saleId = new SimpleLongProperty();

    // Added: UUID for data parity with DTO.
    private final StringProperty saleUuid = new SimpleStringProperty();

    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty paymentMethod = new SimpleStringProperty();
    private final StringProperty reference = new SimpleStringProperty();

    // Corrected: Data type changed from StringProperty to LongProperty to match DB.
    private final LongProperty userId = new SimpleLongProperty();

    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    // Getters for property values
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public long getSaleId() { return saleId.get(); }
    public String getSaleUuid() { return saleUuid.get(); }
    public double getAmount() { return amount.get(); }
    public String getPaymentMethod() { return paymentMethod.get(); }
    public String getReference() { return reference.get(); }
    public long getUserId() { return userId.get(); } // Returns long
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean getIsDeleted() { return isDeleted.get(); }

    // Setters for property values
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setSaleId(long saleId) { this.saleId.set(saleId); }
    public void setSaleUuid(String saleUuid) { this.saleUuid.set(saleUuid); }
    public void setAmount(double amount) { this.amount.set(amount); }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod.set(paymentMethod); }
    public void setReference(String reference) { this.reference.set(reference); }
    public void setUserId(long userId) { this.userId.set(userId); } // Takes long
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }

    /* ---------- property getters ---------- */
    public LongProperty userIdProperty() { return userId; }
    public LongProperty idProperty() { return id; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty tenantIdProperty() { return tenantId; }
    public LongProperty saleIdProperty() { return saleId; }
    public DoubleProperty amountProperty() { return amount; }
    public StringProperty paymentMethodProperty() { return paymentMethod; }
    public StringProperty referenceProperty() { return reference; }
    public ObjectProperty<java.time.LocalDateTime> createdAtProperty() { return createdAt; }
    public ObjectProperty<java.time.LocalDateTime> lastUpdatedAtProperty() { return lastUpdatedAt; }
    public IntegerProperty isSyncedProperty() { return isSynced; }
    public BooleanProperty isDeletedProperty() { return isDeleted; }
}