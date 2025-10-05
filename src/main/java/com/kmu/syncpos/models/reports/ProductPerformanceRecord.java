package com.kmu.syncpos.models.reports;

import javafx.beans.property.*;

public class ProductPerformanceRecord {
    private final StringProperty productName;
    private final StringProperty sku;
    private final DoubleProperty quantitySold;
    private final DoubleProperty totalRevenue;
    private final StringProperty tenantId; // This is a String because it's a UUID

    public ProductPerformanceRecord(String name, String sku, double qty, double revenue, String tenantId) {
        this.productName = new SimpleStringProperty(name);
        this.sku = new SimpleStringProperty(sku);
        this.quantitySold = new SimpleDoubleProperty(qty);
        this.totalRevenue = new SimpleDoubleProperty(revenue);
        this.tenantId = new SimpleStringProperty(tenantId); // Initialize tenantId
    }

    public String getProductName() { return productName.get(); }
    public StringProperty productNameProperty() { return productName; }

    public String getSku() { return sku.get(); }
    public StringProperty skuProperty() { return sku; }

    public double getQuantitySold() { return quantitySold.get(); }
    public DoubleProperty quantitySoldProperty() { return quantitySold; }

    public double getTotalRevenue() { return totalRevenue.get(); }
    public DoubleProperty totalRevenueProperty() { return totalRevenue; }

    // --- Tenant ID Methods ---
    // Note: No setter is provided, as this is a read-only DTO initialized via constructor.
    public String getTenantId() { return tenantId.get(); }
    public StringProperty tenantIdProperty() { return tenantId; }
}