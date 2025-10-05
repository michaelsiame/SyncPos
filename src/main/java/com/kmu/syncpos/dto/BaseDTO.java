// src/main/java/com/kmu/syncpos/dto/BaseDTO.java
package com.kmu.syncpos.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * An abstract base class for all DTOs.
 * It guarantees that every DTO will have a local database ID, which is required
 * by the generic pushUnsynced helper method in SyncService.
 */
@Getter
@Setter
public abstract class BaseDTO {
    // This 'id' corresponds to the local SQLite database's primary key.
    private long id;
}