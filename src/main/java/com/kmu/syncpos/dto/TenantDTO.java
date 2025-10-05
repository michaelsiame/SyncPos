package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class TenantDTO extends BaseDTO {
    private String uuid;

    @SerializedName("license_key")
    private String licenseKey;

    @SerializedName("owner_email")
    private String ownerEmail;
    private String status;

    @SerializedName("expiry_date")
    private LocalDate  expiryDate;

    @SerializedName("created_at")
    private OffsetDateTime createdAt;
    @SerializedName("is_synced")
    private transient int isSynced;
}