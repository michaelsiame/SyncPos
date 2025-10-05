// src/main/java/com/kmu/syncpos/dao/SettingsDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.SettingsDTO;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing settings in the database.
 * This class handles all CRUD (Create, Read, Update, Delete) operations
 * for the 'settings' table, including logic for synchronization status.
 */
public class SettingsDAO {

    private static final Logger LOGGER = Logger.getLogger(SettingsDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, setting_key, setting_value,
               last_updated_at, is_synced, is_deleted
        FROM settings
        """;

    private static final String GET_ALL_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_deleted = false";

    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String UPSERT_LOCAL_SQL = """
        INSERT INTO settings(tenant_id, setting_key, setting_value, uuid, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, false, false)
        ON CONFLICT(tenant_id, setting_key)
        DO UPDATE SET
           setting_value = excluded.setting_value,
           last_updated_at = excluded.last_updated_at,
           is_synced = false,
           is_deleted = false
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO settings(uuid, tenant_id, setting_key, setting_value, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id,
            setting_key = excluded.setting_key,
            setting_value = excluded.setting_value,
            last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_SYNCED_SQL = "UPDATE settings SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";

    private static final String MARK_DELETED_SQL = "UPDATE settings SET is_deleted = true, is_synced = false, last_updated_at = ? WHERE setting_key = ? AND tenant_id = ?";


    /**
     * Retrieves all non-deleted settings for a specific tenant.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of active SettingsDTOs.
     */
    public List<SettingsDTO> getAll(String tenantId) {
        List<SettingsDTO> settings = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                settings.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all settings for tenant: " + tenantId, e);
        }
        return settings;
    }

    /**
     * Retrieves all settings that have not been synced for a specific tenant.
     * This includes new, updated, and deleted records.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced SettingsDTOs.
     */
    public List<SettingsDTO> getUnsyncedSettings(String tenantId) {
        List<SettingsDTO> settings = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                settings.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced settings for tenant: " + tenantId, e);
        }
        return settings;
    }

    /**
     * Inserts or updates a setting from a local change (e.g., user input).
     * The record is marked as unsynced. A new UUID is generated for new records.
     * Conflict is resolved based on the (tenant_id, setting_key) unique constraint.
     *
     * @param dto      The setting data to save. Only setting_key and setting_value are used.
     * @param tenantId The UUID of the tenant.
     */
    public void upsertLocal(SettingsDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_LOCAL_SQL)) {

            ps.setString(1, tenantId);
            ps.setString(2, dto.getSettingKey());
            ps.setString(3, dto.getSettingValue());
            ps.setString(4, UUID.randomUUID().toString()); // For potential INSERT
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert local setting: " + dto.getSettingKey(), e);
        }
    }

    /**
     * Inserts or updates a setting from a remote source (e.g., server sync).
     * The record is marked as synced. Conflict is resolved based on the UUID.
     *
     * @param dto The complete setting data from the remote source.
     */
    public void upsertRemote(SettingsDTO dto) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {

            ps.setString(1, dto.getUuid());
            ps.setString(2, dto.getTenantId());
            ps.setString(3, dto.getSettingKey());
            ps.setString(4, dto.getSettingValue());
            ps.setTimestamp(5, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(6, dto.isDeleted());

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote setting with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Marks a specific setting as synced in the database.
     *
     * @param id       The database ID of the setting.
     * @param tenantId The UUID of the tenant.
     */
    public void markAsSynced(long id, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_SYNCED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, id);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark setting as synced, id: " + id, e);
        }
    }

    /**
     * Performs a soft delete on a setting, marking it as deleted and unsynced.
     *
     * @param settingKey The key of the setting to delete.
     * @param tenantId   The UUID of the tenant.
     */
    public void markAsDeleted(String settingKey, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_DELETED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, settingKey);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark setting as deleted, key: " + settingKey, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link SettingsDTO} object.
     *
     * @param rs The ResultSet to map.
     * @return A populated SettingsDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private SettingsDTO mapToDTO(ResultSet rs) throws SQLException {
        SettingsDTO dto = new SettingsDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setSettingKey(rs.getString("setting_key"));
        dto.setSettingValue(rs.getString("setting_value"));

        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) {
            // Assuming database stores timestamps in UTC
            dto.setLastUpdatedAt(ts.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        dto.setIsSynced(rs.getInt("is_synced"));
        dto.setDeleted(rs.getBoolean("is_deleted"));
        return dto;
    }
}