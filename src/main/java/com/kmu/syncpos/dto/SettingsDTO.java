package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class SettingsDTO extends BaseDTO {
    private String uuid;

    @SerializedName("tenant_id")
    private String tenantId;

    @SerializedName("setting_key")
    private String settingKey;

    @SerializedName("setting_value")
    private String settingValue;

    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    @SerializedName("is_synced")
    private transient int isSynced;
}