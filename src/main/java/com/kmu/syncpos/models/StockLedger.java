// src/main/java/com/kmu/syncpos/models/StockLedger.java
package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class StockLedger {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final LongProperty productId = new SimpleLongProperty();
    private final StringProperty productUuid = new SimpleStringProperty(); // Added for data parity
    private final DoubleProperty quantityDelta = new SimpleDoubleProperty();
    private final StringProperty reason = new SimpleStringProperty();
    private final ObjectProperty<Long> saleItemId = new SimpleObjectProperty<>(); // Corrected for nullability
    private final StringProperty saleItemUuid  = new SimpleStringProperty(); // Added for data parity
    private final LongProperty userId = new SimpleLongProperty();
    private final StringProperty notes = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    /* ---------- getters ---------- */
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public long getProductId() { return productId.get(); }
    public String getProductUuid() { return productUuid.get(); }
    public double getQuantityDelta() { return quantityDelta.get(); }
    public String getReason() { return reason.get(); }
    public Long getSaleItemId() { return saleItemId.get(); }
    public String getSaleItemUuid () { return saleItemUuid .get(); }
    public long getUserId() { return userId.get(); }
    public String getNotes() { return notes.get(); }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean isDeleted() { return isDeleted.get(); }

    /* ---------- setters ---------- */
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setProductId(long productId) { this.productId.set(productId); }
    public void setProductUuid(String productUuid) { this.productUuid.set(productUuid); }
    public void setQuantityDelta(double quantityDelta) { this.quantityDelta.set(quantityDelta); }
    public void setReason(String reason) { this.reason.set(reason); }
    public void setSaleItemId(Long saleItemId) { this.saleItemId.set(saleItemId); }
    public void setSaleItemUuid (String saleItemUuid ) { this.saleItemUuid .set(saleItemUuid ); }
    public void setUserId(long userId) { this.userId.set(userId); }
    public void setNotes(String notes) { this.notes.set(notes); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }


    /* ---------- property getters (abbreviated) ---------- */
    public LongProperty idProperty() { return id; }
}