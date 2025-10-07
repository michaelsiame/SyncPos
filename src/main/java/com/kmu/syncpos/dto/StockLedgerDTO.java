// src/main/java/com/kmu/syncpos/dto/StockLedgerDTO.java
package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class StockLedgerDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;
    @SerializedName("user_uuid") private String userUuid;

    // --- Relational IDs & UUIDs ---

    @SerializedName("product_id")
    private long productId; // Local DB ID

    // This is ESSENTIAL for the DAO's upsert method.
    @SerializedName("product_uuid")
    private String productUuid; // UUID for syncing

    // Maps to the 'sale_item_id' column in the database.
    @SerializedName("sale_item_id")
    private Long saleItemId; // Local DB ID (nullable)

    // This is ESSENTIAL for the DAO's upsert method.
    @SerializedName("sale_item_uuid")
    private String saleItemUuid;

    @SerializedName("user_id")
    private long userId;

    // --- Data Fields ---

    @SerializedName("quantity_delta")
    private double quantityDelta;

    private String reason;
    private String notes;

    // --- Timestamps & Flags ---

    @SerializedName("created_at")
    private OffsetDateTime createdAt;

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    private transient int isSynced;
}