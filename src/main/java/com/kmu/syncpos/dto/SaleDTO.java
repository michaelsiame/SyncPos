package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaleDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    private String type;

    @SerializedName("user_id")
    private long userId;

    // --- Relational IDs & UUIDs ---
    // Both local IDs and UUIDs are needed for different purposes.

    // [For Local Use] The local database ID of the customer.
    @SerializedName("customer_id")
    private Long customerId;

    // [For Syncing] The UUID of the customer. ESSENTIAL for the DAO's upsert method.
    @SerializedName("customer_uuid")
    private String customerUuid;

    // [For Local Use] The local database ID of the supplier.
    @SerializedName("supplier_id")
    private Long supplierId;

    // [For Syncing] The UUID of the supplier. ESSENTIAL for the DAO's upsert method.
    @SerializedName("supplier_uuid")
    private String supplierUuid;


    // --- Financial Data ---
    private double subtotal;
    private double tax;
    private double discount;
    private double total;


    // --- Status & Notes ---
    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("payment_status")
    private String paymentStatus;
    private String notes;


    // --- Timestamps & Flags ---
    @SerializedName("created_at")
    private OffsetDateTime createdAt;

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    private transient int isSynced;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    // A list to hold related SaleItemDTOs.
    private List<SaleItemDTO> items = new ArrayList<>();
}