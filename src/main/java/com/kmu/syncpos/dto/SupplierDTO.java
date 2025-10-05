package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class SupplierDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    private String name;

    @SerializedName("contact_person")
    private String contactPerson;
    private String email;
    private String phone;
    private String address;

    @SerializedName("payment_terms")
    private String paymentTerms;

    @SerializedName("credit_limit")
    private Double creditLimit;

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @SerializedName("is_synced")
    private transient int isSynced;

    @SerializedName("is_deleted")
    private boolean isDeleted;
}