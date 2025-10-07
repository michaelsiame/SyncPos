package com.kmu.syncpos.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class SupplierDTO extends BaseDTO {
    @Expose
    private String uuid;
    @Expose @SerializedName("tenant_id") private String tenantId;
    @Expose private String name;
    @Expose @SerializedName("contact_person") private String contactPerson;
    @Expose private String email;
    @Expose private String phone;
    @Expose private String address;
    @Expose @SerializedName("payment_terms") private String paymentTerms;
    @Expose @SerializedName("credit_limit") private Double creditLimit;
    @Expose @SerializedName("last_updated_at") private OffsetDateTime lastUpdatedAt;
    @Expose @SerializedName("is_deleted") private boolean isDeleted;

    // Local only
    @SerializedName("is_synced") private transient int isSynced;
}