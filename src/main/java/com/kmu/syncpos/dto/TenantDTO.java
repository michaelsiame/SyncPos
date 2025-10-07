package com.kmu.syncpos.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class TenantDTO extends BaseDTO {
    @Expose
    private String uuid;
    @Expose @SerializedName("license_key") private String licenseKey;
    @Expose @SerializedName("owner_email") private String ownerEmail;
    @Expose private String status;
    @Expose @SerializedName("expiry_date") private LocalDate expiryDate;
    @Expose @SerializedName("created_at") private OffsetDateTime createdAt;

    // Local only
    @SerializedName("is_synced") private transient int isSynced;
}