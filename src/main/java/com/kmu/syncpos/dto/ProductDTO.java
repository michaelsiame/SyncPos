package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class ProductDTO extends BaseDTO {

    // Global unique identifier for this product. This is the primary key for syncing.
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    private String sku;
    private String barcode;
    private String name;
    private String description;

    @SerializedName("product_type")
    private String productType;

    // --- Relational IDs ---
    @SerializedName("category_id")
    private Long categoryId;

    @SerializedName("category_uuid")
    private String categoryUuid;

    @SerializedName("unit_id")
    private Long unitId;

    @SerializedName("unit_uuid")
    private String unitUuid;

    @SerializedName("supplier_id")
    private Long supplierId;

    @SerializedName("supplier_uuid")
    private String supplierUuid;


    // --- Financial & Stock Data ---

    @SerializedName("purchase_price")
    private Double purchasePrice; // REFINED: Use Double wrapper for nullability

    @SerializedName("selling_price")
    private Double sellingPrice;  // REFINED: Use Double wrapper for nullability

    @SerializedName("tax_rate")
    private Double taxRate;       // REFINED: Use Double wrapper for nullability

    @SerializedName("min_stock_level")
    private Double minStockLevel; // REFINED: Use Integer wrapper for nullability

    @SerializedName("reorder_quantity")
    private Double reorderQuantity; // REFINED: Use Integer wrapper for nullability

    @SerializedName("current_stock")
    private Double currentStock; // REFINED: Use Double wrapper for nullability


    // --- Timestamps & Flags ---

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    // Local-only flag, not serialized.
    private transient int isSynced;

    @SerializedName("is_deleted")
    private boolean isDeleted;
}