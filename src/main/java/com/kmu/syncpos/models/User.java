package com.kmu.syncpos.models;

import javafx.beans.property.*;

public class User {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty uuid = new SimpleStringProperty();
    private final StringProperty tenantId = new SimpleStringProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty firstname = new SimpleStringProperty();
    private final StringProperty lastname = new SimpleStringProperty();
    private final StringProperty passwordHash = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final BooleanProperty isActive = new SimpleBooleanProperty();
    private final ObjectProperty<java.time.LocalDateTime> lastUpdatedAt = new SimpleObjectProperty<>();
    private final IntegerProperty isSynced = new SimpleIntegerProperty();
    private final BooleanProperty isDeleted = new SimpleBooleanProperty();

    // Getters for property values
    public long getId() { return id.get(); }
    public String getUuid() { return uuid.get(); }
    public String getTenantId() { return tenantId.get(); }
    public String getUsername() { return username.get(); }
    public String getFirstname() { return firstname.get(); }
    public String getLastname() { return lastname.get(); }
    public String getPasswordHash() { return passwordHash.get(); }
    public String getEmail() { return email.get(); }
    public String getPhone() { return phone.get(); }
    public String getRole() { return role.get(); }
    public boolean getIsActive() { return isActive.get(); }
    public java.time.LocalDateTime getLastUpdatedAt() { return lastUpdatedAt.get(); }
    public int getIsSynced() { return isSynced.get(); }
    public boolean getIsDeleted() { return isDeleted.get(); }

    // Setters for property values
    public void setId(long id) { this.id.set(id); }
    public void setUuid(String uuid) { this.uuid.set(uuid); }
    public void setTenantId(String tenantId) { this.tenantId.set(tenantId); }
    public void setUsername(String username) { this.username.set(username); }
    public void setFirstname(String firstname) { this.firstname.set(firstname); }
    public void setLastname(String lastname) { this.lastname.set(lastname); }
    public void setPasswordHash(String passwordHash) { this.passwordHash.set(passwordHash); }
    public void setEmail(String email) { this.email.set(email); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public void setRole(String role) { this.role.set(role); }
    public void setIsActive(boolean isActive) { this.isActive.set(isActive); }
    public void setLastUpdatedAt(java.time.LocalDateTime lastUpdatedAt) { this.lastUpdatedAt.set(lastUpdatedAt); }
    public void setIsSynced(int isSynced) { this.isSynced.set(isSynced); }
    public void setIsDeleted(boolean isDeleted) { this.isDeleted.set(isDeleted); }

    // Property getters
    public LongProperty idProperty() { return id; }
    public StringProperty uuidProperty() { return uuid; }
    public StringProperty tenantIdProperty() { return tenantId; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty firstnameProperty() { return firstname; }
    public StringProperty lastnameProperty() { return lastname; }
    public StringProperty passwordHashProperty() { return passwordHash; }
    public StringProperty emailProperty() { return email; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty roleProperty() { return role; }
    public BooleanProperty isActiveProperty() { return isActive; }
    public ObjectProperty<java.time.LocalDateTime> lastUpdatedAtProperty() { return lastUpdatedAt; }
    public IntegerProperty isSyncedProperty() { return isSynced; }
    public BooleanProperty isDeletedProperty() { return isDeleted; }
}