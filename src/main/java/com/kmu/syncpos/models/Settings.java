package com.kmu.syncpos.models;

import javafx.beans.property.*;

public class Settings {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final StringProperty settingKey = new SimpleStringProperty();
    private final StringProperty settingValue = new SimpleStringProperty();
    private final ObjectProperty<java.time.LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    // Getters for property values
    public long getId() {
        return id.get();
    }

    public String getUuid() {
        return uuid.get();
    }

    public String getTenantId() {
        return tenantId.get();
    }

    public String getSettingKey() {
        return settingKey.get();
    }

    public String getSettingValue() {
        return settingValue.get();
    }

    public java.time.LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt.get();
    }

    public int getIsSynced() {
        return isSynced.get();
    }

    public BooleanProperty isDeletedProperty() {
        return isDeleted;
    }

    // Setters for property values
    public void setId(long id) {
        this.id.set(id);
    }

    public void setUuid(String uuid) {
        this.uuid.set(uuid);
    }

    public void setTenantId(String tenantId) {
        this.tenantId.set(tenantId);
    }

    public void setSettingKey(String settingKey) {
        this.settingKey.set(settingKey);
    }

    public void setSettingValue(String settingValue) {
        this.settingValue.set(settingValue);
    }

    public void setLastUpdatedAt(java.time.LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt.set(lastUpdatedAt);
    }

    public void setIsSynced(int isSynced) {
        this.isSynced.set(isSynced);
    }
    public void setIsDeleted(boolean isDeleted){
        this.isDeleted.set(isDeleted);
    }

    // Property getters
    public LongProperty idProperty() {
        return id;
    }

    public StringProperty uuidProperty() {
        return uuid;
    }

    public StringProperty tenantIdProperty() {
        return tenantId;
    }

    public StringProperty settingKeyProperty() {
        return settingKey;
    }

    public StringProperty settingValueProperty() {
        return settingValue;
    }

    public ObjectProperty<java.time.LocalDateTime> lastUpdatedAtProperty() {
        return lastUpdatedAt;
    }

    public IntegerProperty isSyncedProperty() {
        return isSynced;
    }

    public boolean getIsDeleted() { return isDeleted.get(); }
}