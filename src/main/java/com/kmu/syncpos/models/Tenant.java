package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Tenant {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty licenseKey = new SimpleStringProperty();
    private final StringProperty ownerEmail = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> expiryDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();

    // Getters for property values
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getLicenseKey() { return licenseKey.get(); }
    public String getOwnerEmail() { return ownerEmail.get(); }
    public String getStatus() { return status.get(); }
    public LocalDate getExpiryDate() { return expiryDate.get(); }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }

    // Setters for property values
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setLicenseKey(String licenseKey) { this.licenseKey.set(licenseKey); }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail.set(ownerEmail); }
    public void setStatus(String status) { this.status.set(status); }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate.set(expiryDate); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }

    // Property getters
    public LongProperty idProperty() { return id; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty licenseKeyProperty() { return licenseKey; }
    public StringProperty ownerEmailProperty() { return ownerEmail; }
    public StringProperty statusProperty() { return status; }
    public ObjectProperty<LocalDate> expiryDateProperty() { return expiryDate; }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
}