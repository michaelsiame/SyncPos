package com.kmu.syncpos.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserDTO extends BaseDTO {
    @Expose
    private String uuid;
    @Expose @SerializedName("tenant_id") private String tenantId;
    @Expose private String username;
    @Expose private String firstname;
    @Expose private String lastname;
    @Expose @SerializedName("password_hash") private String passwordHash;
    @Expose private String email;
    @Expose private String phone;
    @Expose private String role;
    @Expose @SerializedName("is_active") private boolean isActive;
    @Expose @SerializedName("last_updated_at") private OffsetDateTime lastUpdatedAt;
    @Expose @SerializedName("is_deleted") private boolean isDeleted;

    // Local only fields
    @SerializedName("is_synced") private transient int isSynced;
    private transient String plainPassword; // Already transient, good
}