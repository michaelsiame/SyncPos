package com.kmu.syncpos.models.reports;

import javafx.beans.property.*;
import java.time.LocalDateTime;

// This class is a "Data Transfer Object" (DTO) specifically for the sales report.
public class SalesReportRecord {
    private final IntegerProperty transactionId;
    private final ObjectProperty<LocalDateTime> createdAt;
    private final StringProperty customerName;
    private final StringProperty cashierName;
    private final DoubleProperty total;
    private final StringProperty tenantId; // This is a String because it's a UUID

    public SalesReportRecord(int id, LocalDateTime date, String customer, String cashier, double total, String tenantId) {
        this.transactionId = new SimpleIntegerProperty(id);
        this.createdAt = new SimpleObjectProperty<>(date);
        this.customerName = new SimpleStringProperty(customer);
        this.cashierName = new SimpleStringProperty(cashier);
        this.total = new SimpleDoubleProperty(total);
        this.tenantId = new SimpleStringProperty(tenantId); // Initialize tenantId
    }

    public int getTransactionId() { return transactionId.get(); }
    public IntegerProperty transactionIdProperty() { return transactionId; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }

    public String getCustomerName() { return customerName.get(); }
    public StringProperty customerNameProperty() { return customerName; }

    public String getCashierName() { return cashierName.get(); }
    public StringProperty cashierNameProperty() { return cashierName; }

    public double getTotal() { return total.get(); }
    public DoubleProperty totalProperty() { return total; }

    // --- Tenant ID Methods ---
    // Note: No setter is provided, as this is a read-only DTO initialized via constructor.
    public String getTenantId() { return tenantId.get(); }
    public StringProperty tenantIdProperty() { return tenantId; }
}