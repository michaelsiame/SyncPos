// src/main/java/com/kmu/syncpos/dao/CategoryDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.CategoryDTO;
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
 * Data Access Object for managing categories in the database.
 * This class handles all CRUD operations for the 'categories' table,
 * including logic for hierarchical relationships (parent_id) and synchronization.
 */
public class CategoryDAO {

    private static final Logger LOGGER = Logger.getLogger(CategoryDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, name, description, parent_id,
               last_updated_at, is_synced, is_deleted
        FROM categories
        """;

    private static final String GET_ALL_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_deleted = false ORDER BY name";
    private static final String GET_BY_ID_SQL = BASE_SELECT_SQL + " WHERE id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String CREATE_LOCAL_SQL = """
        INSERT INTO categories(name, description, parent_id, uuid, tenant_id, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_LOCAL_SQL = """
        UPDATE categories
        SET name = ?, description = ?, parent_id = ?, last_updated_at = ?, is_synced = false
        WHERE id = ? AND tenant_id = ? AND is_deleted = false
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO categories (uuid, tenant_id, name, description, parent_id, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id,
            name = excluded.name,
            description = excluded.description,
            parent_id = excluded.parent_id,
            last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_DELETED_SQL = "UPDATE categories SET is_deleted = true, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_SYNCED_SQL = "UPDATE categories SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String VALIDATE_PARENT_SQL = "SELECT 1 FROM categories WHERE id = ? AND tenant_id = ? AND is_deleted = false";


    /**
     * Retrieves all non-deleted categories for a specific tenant.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of active CategoryDTOs, ordered by name.
     */
    public List<CategoryDTO> getAll(String tenantId) {
        List<CategoryDTO> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categories.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all categories for tenant: " + tenantId, e);
        }
        return categories;
    }

    /**
     * Retrieves a single non-deleted category by its local database ID.
     *
     * @param categoryId The local ID of the category.
     * @param tenantId   The UUID of the tenant.
     * @return A CategoryDTO if found, otherwise null.
     */
    public CategoryDTO getById(long categoryId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setLong(1, categoryId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get category by id: " + categoryId, e);
        }
        return null;
    }

    /**
     * Retrieves all categories that have not been synced for a specific tenant.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced CategoryDTOs.
     */
    public List<CategoryDTO> getUnsynced(String tenantId) {
        List<CategoryDTO> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categories.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced categories for tenant: " + tenantId, e);
        }
        return categories;
    }

    /**
     * Creates a new category from a local change, marking it as unsynced.
     *
     * @param dto      The category data to add.
     * @param tenantId The UUID of the tenant.
     */
    public void createLocal(CategoryDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_LOCAL_SQL)) {
            validateParent(conn, dto.getParentId(), tenantId);
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getDescription());
            ps.setObject(3, dto.getParentId(), Types.BIGINT);
            ps.setString(4, UUID.randomUUID().toString());
            ps.setString(5, tenantId);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Failed to create local category: " + dto.getName(), e);
        }
    }

    /**
     * Updates an existing category from a local change, marking it as unsynced.
     *
     * @param dto      The category data to update.
     * @param tenantId The UUID of the tenant.
     */
    public void updateLocal(CategoryDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOCAL_SQL)) {
            validateParent(conn, dto.getParentId(), tenantId);
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getDescription());
            ps.setObject(3, dto.getParentId(), Types.BIGINT);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, dto.getId());
            ps.setString(6, tenantId);
            ps.executeUpdate();
        } catch (SQLException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Failed to update local category: " + dto.getId(), e);
        }
    }

    /**
     * Inserts or updates a category from a remote source, marking it as synced.
     *
     * @param dto The complete category data from the remote source.
     */
    public void upsertRemote(CategoryDTO dto) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            ps.setString(1, dto.getUuid());
            ps.setString(2, dto.getTenantId());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getDescription());
            ps.setObject(5, dto.getParentId(), Types.BIGINT);
            ps.setTimestamp(6, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(7, dto.isDeleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote category with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Performs a soft delete on a category by its ID.
     *
     * @param categoryId The local ID of the category to delete.
     * @param tenantId   The UUID of the tenant.
     */
    public void markAsDeleted(long categoryId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_DELETED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, categoryId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark category as deleted, id: " + categoryId, e);
        }
    }

    /**
     * Marks a specific category as synced.
     *
     * @param id       The local database ID of the category.
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
            LOGGER.log(Level.SEVERE, "Failed to mark category as synced, id: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link CategoryDTO} object.
     *
     * @param rs The ResultSet to map.
     * @return A populated CategoryDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private CategoryDTO mapToDTO(ResultSet rs) throws SQLException {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setName(rs.getString("name"));
        dto.setDescription(rs.getString("description"));
        dto.setParentId(rs.getObject("parent_id", Long.class)); // Safely handles NULL

        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) {
            dto.setLastUpdatedAt(ts.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        dto.setIsSynced(rs.getInt("is_synced"));
        dto.setDeleted(rs.getBoolean("is_deleted"));
        return dto;
    }

    /**
     * Validates that the provided parent_id exists and belongs to the same tenant.
     *
     * @param conn     The database connection to use for the query.
     * @param parentId The parent ID to validate. Can be null.
     * @param tenantId The tenant UUID.
     * @throws SQLException             if a database error occurs.
     * @throws IllegalArgumentException if the parent_id is invalid.
     */
    private void validateParent(Connection conn, Long parentId, String tenantId) throws SQLException, IllegalArgumentException {
        if (parentId == null || parentId <= 0) {
            return; // Null or zero is a valid "no parent" state.
        }
        try (PreparedStatement ps = conn.prepareStatement(VALIDATE_PARENT_SQL)) {
            ps.setLong(1, parentId);
            ps.setString(2, tenantId);
            if (!ps.executeQuery().next()) {
                throw new IllegalArgumentException("Invalid parent category ID: " + parentId);
            }
        }
    }
}