// src/main/java/com/kmu/syncpos/models/Supplier.java
package com.kmu.syncpos.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Supplier {
    // Corrected: Use ObjectProperty for nullable numbers to avoid NPEs and represent null correctly.
    private final ObjectProperty<Long> id = new SimpleObjectProperty<>();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty contactPerson = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty paymentTerms = new SimpleStringProperty();
    private final ObjectProperty<Double> creditLimit = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    // Getters for property values
    public Long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public String getName() { return name.get(); }
    public String getContactPerson() { return contactPerson.get(); }
    public String getEmail() { return email.get(); }
    public String getPhone() { return phone.get(); }
    public String getAddress() { return address.get(); }
    public String getPaymentTerms() { return paymentTerms.get(); }
    public Double getCreditLimit() { return creditLimit.get(); }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean getIsDeleted() { return isDeleted.get(); }

    // Setters for property values
    public void setId(Long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setName(String name) { this.name.set(name); }
    public void setContactPerson(String contactPerson) { this.contactPerson.set(contactPerson); }
    public void setEmail(String email) { this.email.set(email); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public void setAddress(String address) { this.address.set(address); }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms.set(paymentTerms); }
    public void setCreditLimit(Double creditLimit) { this.creditLimit.set(creditLimit); }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }

    // Property getters
    public ObjectProperty<Long> idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Double> creditLimitProperty() { return creditLimit; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty tenantIdProperty() { return tenantId; }
    public StringProperty contactPersonProperty() { return contactPerson; }
    public StringProperty emailProperty() { return email; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty addressProperty() { return address; }
    public StringProperty paymentTermsProperty() { return paymentTerms; }
    public ObjectProperty<java.time.LocalDateTime> lastUpdatedAtProperty() { return lastUpdatedAt; }
    public IntegerProperty isSyncedProperty() { return isSynced; }
    public BooleanProperty isDeletedProperty() { return isDeleted; }
}