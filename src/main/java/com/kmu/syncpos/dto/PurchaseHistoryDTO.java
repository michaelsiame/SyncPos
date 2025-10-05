// src/main/java/com/kmu/syncpos/dto/PurchaseHistoryDTO.java
package com.kmu.syncpos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A specialized, read-only DTO used to transfer aggregated purchase history
 * data from the service layer to the UI layer. This is not a database entity.
 */
public record PurchaseHistoryDTO(
        LocalDate date,
        BigDecimal total,
        int itemCount
) {
    // A Java Record automatically provides a constructor, getters (date(), total(), itemCount()),
    // equals(), hashCode(), and toString() methods. No additional code is needed.
}