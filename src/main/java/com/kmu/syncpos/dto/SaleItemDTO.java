// src/main/java/com/kmu/syncpos/dto/SaleItemDTO.java
package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class SaleItemDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    // --- Relational IDs ---
    @SerializedName("sale_id")
    private long saleId;
    @SerializedName("sale_uuid")
    private String saleUuid;
    @SerializedName("product_id")
    private long productId;
    @SerializedName("product_uuid")
    private String productUuid;

    // =============================================================
    // CHANGE: ADD THIS NEW FIELD
    // =============================================================
    @SerializedName("supplier_product_code")
    private String supplierProductCode;

    // --- Item-specific Data ---
    private double quantity;
    @SerializedName("unit_price")
    private double unitPrice;
    @SerializedName("cost_at_sale")
    private double costAtSale;
    @SerializedName("tax_rate")
    private double taxRate;
    private double discount;
    private double total;

    // --- Timestamps & Flags ---
    @SerializedName("created_at")
    private OffsetDateTime createdAt;
    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;
    @SerializedName("is_deleted")
    private boolean isDeleted;
    private transient int isSynced;
}