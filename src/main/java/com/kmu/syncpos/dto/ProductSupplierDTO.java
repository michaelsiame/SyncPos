// src/main/java/com/kmu/syncpos/dto/ProductSupplierDTO.java
package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * DTO representing the link between a Product and a Supplier, including
 * the supplier-specific product code.
 */
@Getter
@Setter
public class ProductSupplierDTO extends BaseDTO{
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    // --- Relational IDs for Local Use ---
    @SerializedName("product_id")
    private long productId;

    @SerializedName("supplier_id")
    private long supplierId;

    // --- Relational UUIDs for Syncing ---
    @SerializedName("product_uuid")
    private String productUuid;

    @SerializedName("supplier_uuid")
    private String supplierUuid;

    // --- Data Field ---
    @SerializedName("supplier_product_code")
    private String supplierProductCode;

    // --- Timestamps & Flags ---
    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    private transient int isSynced;

    @SerializedName("is_deleted")
    private boolean isDeleted;
}