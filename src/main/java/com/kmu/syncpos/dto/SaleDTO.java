package com.kmu.syncpos.dto;

import com.google.gson.annotations.Expose; // <-- IMPORT
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaleDTO extends BaseDTO {
    @Expose @SerializedName("uuid") private String uuid;
    @Expose @SerializedName("tenant_id") private String tenantId;
    @Expose private String type;
    @Expose @SerializedName("user_id") private long userId;
    @Expose @SerializedName("user_uuid") private String userUuid;
    // --- Relational IDs & UUIDs ---

    // [For Local Use] - DO NOT EXPOSE. This field will NOT be sent to the API.
    @SerializedName("customer_id") private Long customerId;

    // [For Syncing] - EXPOSE THIS. This field WILL be sent to the API.
    @Expose @SerializedName("customer_uuid") private String customerUuid;

    // [For Local Use] - DO NOT EXPOSE.
    @SerializedName("supplier_id") private Long supplierId;

    // [For Syncing] - EXPOSE THIS.
    @Expose @SerializedName("supplier_uuid") private String supplierUuid;

    // --- Financial Data ---
    @Expose private double subtotal;
    @Expose private double tax;
    @Expose private double discount;
    @Expose private double total;

    // --- Status & Notes ---
    @Expose @SerializedName("payment_method") private String paymentMethod;
    @Expose @SerializedName("payment_status") private String paymentStatus;
    @Expose private String notes;

    // --- Timestamps & Flags ---
    @Expose @SerializedName("created_at") private OffsetDateTime createdAt;
    @Expose @SerializedName("last_updated_at") private OffsetDateTime lastUpdatedAt;
    @Expose @SerializedName("is_deleted") private boolean isDeleted;

    // A list to hold related SaleItemDTOs.
    @Expose private List<SaleItemDTO> items = new ArrayList<>();

    // isSynced is local-only, so we correctly do NOT expose it.
    private transient int isSynced;
}