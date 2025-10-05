// src/main/java/com/kmu/syncpos/service/SettingsService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.SettingsDAO;
import com.kmu.syncpos.dto.SettingsDTO;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for managing application settings.
 * This class encapsulates the business logic for retrieving and saving settings,
 * abstracting the data source from the UI controllers.
 */
public class SettingsService {

    private final SettingsDAO settingsDAO = new SettingsDAO();

    /**
     * Retrieves all settings for the current tenant and returns them as a simple Map.
     * @return A Map of setting keys to setting values (e.g., "companyName" -> "My Company").
     */
    public Map<String, String> getAllSettingsAsMap() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            List<SettingsDTO> dtos = settingsDAO.getAll(tenantId);
            return dtos.stream()
                    .collect(Collectors.toMap(SettingsDTO::getSettingKey, SettingsDTO::getSettingValue));
        } catch (IllegalStateException e) {
            System.err.println("SettingsService: Tenant context not available. " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves a single setting for the current tenant.
     * @param key The key of the setting to retrieve.
     * @param defaultValue The value to return if the setting is not found.
     * @return The setting's value, or the default value if not found.
     */
    public String getSetting(String key, String defaultValue) {
        return getAllSettingsAsMap().getOrDefault(key, defaultValue);
    }

    /**
     * Saves multiple settings for the current tenant in a single database transaction.
     * @param settingsMap A map of setting keys to setting values.
     * @return true if all settings were saved successfully, false otherwise.
     */
    public boolean saveAllSettings(Map<String, String> settingsMap) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    for (Map.Entry<String, String> entry : settingsMap.entrySet()) {
                        SettingsDTO dto = new SettingsDTO();
                        dto.setSettingKey(entry.getKey());
                        dto.setSettingValue(entry.getValue());
                        // Use the new DAO method that accepts a connection
                        settingsDAO.upsertLocal(dto, tenantId);
                    }
                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    System.err.println("SettingsService: Transaction failed, rolling back. " + e.getMessage());
                    return false;
                }
            } catch (SQLException e) {
                System.err.println("SettingsService: Database connection error. " + e.getMessage());
                return false;
            }
        } catch (IllegalStateException e) {
            System.err.println("SettingsService: Cannot save settings, tenant context not available. " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all unsynced settings for a specific tenant.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced SettingsDTOs.
     */
    public List<SettingsDTO> getUnsyncedSettings(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return settingsDAO.getUnsyncedSettings(tenantId);
    }
}