package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CategoryDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    private String name;
    private String description;

    // --- Relational IDs & UUIDs ---

    // [For Local Use] The local database ID of the parent category.
    @SerializedName("parent_id")
    private Long parentId;

    // [For Syncing] The UUID of the parent category. ESSENTIAL for the DAO's upsert method.
    @SerializedName("parent_uuid")
    private String parentUuid;

    // --- Timestamps & Flags ---

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    private transient int isSynced;

    @SerializedName("is_deleted")
    private boolean isDeleted;
}