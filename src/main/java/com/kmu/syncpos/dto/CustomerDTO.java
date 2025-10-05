package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CustomerDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    private String name;
    private String email;
    private String phone;
    private String address;

    @SerializedName("loyalty_points")
    private Integer loyaltyPoints;

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    @SerializedName("is_synced")
    private transient int isSynced;
}