package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Category {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    // Corrected: Use ObjectProperty for nullable foreign key to prevent NPEs.
    private final ObjectProperty<Long> parentId = new SimpleObjectProperty<>();

    // Added: UUID field for data parity with DTO to support sync logic.
    private final StringProperty parentUuid = new SimpleStringProperty();

    private final ObjectProperty<LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    /* ---------- getters ---------- */
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }
    public Long getParentId() { return parentId.get(); } // Returns Long
    public String getParentUuid() { return parentUuid.get(); }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean getIsDeleted() { return isDeleted.get(); }

    /* ---------- setters ---------- */
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setName(String name) { this.name.set(name); }
    public void setDescription(String description) { this.description.set(description); }
    public void setParentId(Long parentId) { this.parentId.set(parentId); } // Takes Long
    public void setParentUuid(String parentUuid) { this.parentUuid.set(parentUuid); }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }

    /* ---------- property getters ---------- */
    public LongProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Long> parentIdProperty() { return parentId; } // Returns ObjectProperty<Long>
    public StringProperty parentUuidProperty() { return parentUuid; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty tenantIdProperty() { return tenantId; }
    public StringProperty descriptionProperty() { return description; }
    public ObjectProperty<java.time.LocalDateTime> lastUpdatedAtProperty() { return lastUpdatedAt; }
    public IntegerProperty isSyncedProperty() { return isSynced; }
    public BooleanProperty isDeletedProperty() { return isDeleted; }
    @Override
    public String toString() {
        return getName(); // Display the category's name
    }
}