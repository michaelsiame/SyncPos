package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    private String username;
    private String firstname;
    private String lastname;

    @SerializedName("password_hash")
    private String passwordHash;

    private String email;
    private String phone;
    private String role;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @SerializedName("is_synced")
    private transient int isSynced;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    private transient String plainPassword;
}