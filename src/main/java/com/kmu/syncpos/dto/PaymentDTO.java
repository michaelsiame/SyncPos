package com.kmu.syncpos.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class PaymentDTO extends BaseDTO {
    @SerializedName("sale_id")
    private long saleId;

    private double amount;

    @SerializedName("payment_method")
    private String paymentMethod;
    private String reference;

    @SerializedName("user_id")
    private long userId;
    private transient int isSynced;
    private String uuid;
    @SerializedName("tenant_id")
    private String tenantId;
    @SerializedName("created_at")
    private OffsetDateTime createdAt;
    @SerializedName("last_updated_at")
    private OffsetDateTime lastUpdatedAt;
    @SerializedName("is_deleted")
    private boolean isDeleted;

    @SerializedName("sale_uuid")
    private String saleUuid;
}