// src/main/java/com/kmu/syncpos/dao/UnitDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.UnitDTO;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing units of measure in the database.
 * This class handles all CRUD operations for the 'units' table,
 * including logic for synchronization status.
 */
public class UnitDAO {

    private static final Logger LOGGER = Logger.getLogger(UnitDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, name, abbreviation,
               last_updated_at, is_synced, is_deleted
        FROM units
        """;

    private static final String GET_ALL_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_deleted = false ORDER BY name";
    private static final String GET_BY_ID_SQL = BASE_SELECT_SQL + " WHERE id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String CREATE_LOCAL_SQL = """
        INSERT INTO units(name, abbreviation, uuid, tenant_id, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_LOCAL_SQL = """
        UPDATE units
        SET name = ?, abbreviation = ?, last_updated_at = ?, is_synced = false
        WHERE id = ? AND tenant_id = ? AND is_deleted = false
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO units (uuid, tenant_id, name, abbreviation, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id,
            name = excluded.name,
            abbreviation = excluded.abbreviation,
            last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_DELETED_SQL = "UPDATE units SET is_deleted = true, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_SYNCED_SQL = "UPDATE units SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";

    /**
     * Retrieves all non-deleted units for a specific tenant.
     * @param tenantId The UUID of the tenant.
     * @return A list of active UnitDTOs, ordered by name.
     */
    public List<UnitDTO> getAll(String tenantId) {
        List<UnitDTO> units = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                units.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all units for tenant: " + tenantId, e);
        }
        return units;
    }

    /**
     * Retrieves a single non-deleted unit by its local database ID.
     * @param unitId The ID of the unit.
     * @param tenantId The UUID of the tenant.
     * @return A UnitDTO if found, otherwise null.
     */
    public UnitDTO getById(long unitId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setLong(1, unitId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unit by ID: " + unitId, e);
        }
        return null;
    }

    /**
     * Creates a new unit from a local change, marking it as unsynced.
     * @param dto      The unit data to add.
     * @param tenantId The UUID of the tenant.
     */
    public void createLocal(UnitDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_LOCAL_SQL)) {
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getAbbreviation());
            ps.setString(3, UUID.randomUUID().toString());
            ps.setString(4, tenantId);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create local unit: " + dto.getName(), e);
        }
    }

    /**
     * Updates an existing unit from a local change, marking it as unsynced.
     * @param dto      The unit data to update.
     * @param tenantId The UUID of the tenant.
     */
    public void updateLocal(UnitDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOCAL_SQL)) {
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getAbbreviation());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, dto.getId());
            ps.setString(5, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update local unit: " + dto.getId(), e);
        }
    }

    /**
     * Inserts or updates a unit from a remote source, marking it as synced.
     * @param dto The complete unit data from the remote source.
     */
    public void upsertRemote(UnitDTO dto) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            ps.setString(1, dto.getUuid());
            ps.setString(2, dto.getTenantId());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getAbbreviation());
            ps.setTimestamp(5, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(6, dto.isDeleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote unit with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Retrieves all units that have not been synced.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced UnitDTOs.
     */
    public List<UnitDTO> getUnsynced(String tenantId) {
        List<UnitDTO> units = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                units.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced units for tenant: " + tenantId, e);
        }
        return units;
    }

    /**
     * Performs a soft delete on a unit.
     * @param id       The local ID of the unit to delete.
     * @param tenantId The UUID of the tenant.
     */
    public void markAsDeleted(long id, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_DELETED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, id);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark unit as deleted: " + id, e);
        }
    }

    /**
     * Marks a specific unit as synced.
     * @param id       The local database ID of the unit.
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
            LOGGER.log(Level.SEVERE, "Failed to mark unit as synced: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link UnitDTO} object.
     * @param rs The ResultSet to map.
     * @return A populated UnitDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private UnitDTO mapToDTO(ResultSet rs) throws SQLException {
        UnitDTO dto = new UnitDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setName(rs.getString("name"));
        dto.setAbbreviation(rs.getString("abbreviation"));
        dto.setIsSynced(rs.getInt("is_synced"));
        dto.setDeleted(rs.getBoolean("is_deleted"));

        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) {
            dto.setLastUpdatedAt(ts.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        return dto;
    }
}