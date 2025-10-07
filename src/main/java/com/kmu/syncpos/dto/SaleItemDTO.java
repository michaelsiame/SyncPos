package com.kmu.syncpos.dto;

import com.google.gson.annotations.Expose; // <-- IMPORT
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
// ... other imports

@Getter
@Setter
public class SaleItemDTO extends BaseDTO {
    @Expose @SerializedName("uuid") private String uuid;
    @Expose @SerializedName("tenant_id") private String tenantId;

    // --- Relational IDs ---

    // [For Local Use] - DO NOT EXPOSE.
    @SerializedName("sale_id") private long saleId;

    // [For Syncing] - EXPOSE THIS.
    @Expose @SerializedName("sale_uuid") private String saleUuid;

    // [For Local Use] - DO NOT EXPOSE.
    @SerializedName("product_id") private long productId;

    // [For Syncing] - EXPOSE THIS.
    @Expose @SerializedName("product_uuid") private String productUuid;

    @Expose @SerializedName("supplier_product_code") private String supplierProductCode;

    // --- Item-specific Data ---
    @Expose private double quantity;
    @Expose @SerializedName("unit_price") private double unitPrice;
    @Expose @SerializedName("cost_at_sale") private double costAtSale;
    @Expose @SerializedName("tax_rate") private double taxRate;
    @Expose private double discount;
    @Expose private double total;

    // --- Timestamps & Flags ---
    @Expose @SerializedName("created_at") private OffsetDateTime createdAt;
    @Expose @SerializedName("last_updated_at") private OffsetDateTime lastUpdatedAt;
    @Expose @SerializedName("is_deleted") private boolean isDeleted;

    private transient int isSynced;
}