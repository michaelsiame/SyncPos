// src/main/java/com/kmu/syncpos/dao/SupplierDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.SupplierDTO;
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
 * Data Access Object for managing suppliers in the database.
 * This class handles all CRUD operations for the 'suppliers' table,
 * including logic for synchronization status.
 */
public class SupplierDAO {

    private static final Logger LOGGER = Logger.getLogger(SupplierDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, name, contact_person, email,
               phone, address, payment_terms, credit_limit,
               last_updated_at, is_synced, is_deleted
        FROM suppliers
        """;

    private static final String GET_ALL_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_deleted = false ORDER BY name";
    private static final String GET_BY_ID_SQL = BASE_SELECT_SQL + " WHERE id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String CREATE_LOCAL_SQL = """
        INSERT INTO suppliers(name, contact_person, email, phone, address, payment_terms, credit_limit,
                              uuid, tenant_id, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_LOCAL_SQL = """
        UPDATE suppliers
        SET name = ?, contact_person = ?, email = ?, phone = ?, address = ?, payment_terms = ?, credit_limit = ?,
            last_updated_at = ?, is_synced = false
        WHERE id = ? AND tenant_id = ? AND is_deleted = false
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO suppliers (uuid, tenant_id, name, contact_person, email, phone, address, payment_terms, credit_limit,
                               last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id, name = excluded.name, contact_person = excluded.contact_person, email = excluded.email,
            phone = excluded.phone, address = excluded.address, payment_terms = excluded.payment_terms, credit_limit = excluded.credit_limit,
            last_updated_at = excluded.last_updated_at, is_deleted = excluded.is_deleted, is_synced = true
        """;

    private static final String MARK_DELETED_SQL = "UPDATE suppliers SET is_deleted = true, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_SYNCED_SQL = "UPDATE suppliers SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";


    /**
     * Retrieves all non-deleted suppliers for a specific tenant.
     * @param tenantId The UUID of the tenant.
     * @return A list of active SupplierDTOs, ordered by name.
     */
    public List<SupplierDTO> getAll(String tenantId) {
        List<SupplierDTO> suppliers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                suppliers.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all suppliers for tenant: " + tenantId, e);
        }
        return suppliers;
    }

    /**
     * Retrieves a single non-deleted supplier by its local database ID.
     * @param supplierId The ID of the supplier.
     * @param tenantId The UUID of the tenant.
     * @return A SupplierDTO if found, otherwise null.
     */
    public SupplierDTO getById(long supplierId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setLong(1, supplierId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get supplier by ID: " + supplierId, e);
        }
        return null;
    }

    /**
     * Creates a new supplier from a local change, marking it as unsynced.
     * @param dto      The supplier data to add.
     * @param tenantId The UUID of the tenant.
     */
    public void createLocal(SupplierDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_LOCAL_SQL)) {
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getContactPerson());
            ps.setString(3, dto.getEmail());
            ps.setString(4, dto.getPhone());
            ps.setString(5, dto.getAddress());
            ps.setString(6, dto.getPaymentTerms());
            ps.setDouble(7, dto.getCreditLimit());
            ps.setString(8, UUID.randomUUID().toString());
            ps.setString(9, tenantId);
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create local supplier: " + dto.getName(), e);
        }
    }

    /**
     * Updates an existing supplier from a local change, marking it as unsynced.
     * @param dto      The supplier data to update.
     * @param tenantId The UUID of the tenant.
     */
    public void updateLocal(SupplierDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOCAL_SQL)) {
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getContactPerson());
            ps.setString(3, dto.getEmail());
            ps.setString(4, dto.getPhone());
            ps.setString(5, dto.getAddress());
            ps.setString(6, dto.getPaymentTerms());
            ps.setDouble(7, dto.getCreditLimit());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(9, dto.getId());
            ps.setString(10, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update local supplier: " + dto.getId(), e);
        }
    }

    /**
     * Inserts or updates a supplier from a remote source, marking it as synced.
     * @param dto The complete supplier data from the remote source.
     */
    public void upsertRemote(SupplierDTO dto) {
        // --- BEGIN DTO SANITIZATION BLOCK ---
        if (dto == null || dto.getUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert a null supplier or a supplier with a null UUID. Aborting operation.");
            return;
        }

        // Sanitize fields with NOT NULL constraints in the schema.
        if (dto.getTenantId() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null tenant_id to empty string for supplier UUID: {0}", dto.getUuid());
            dto.setTenantId("");
        }
        if (dto.getName() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null name to empty string for supplier UUID: {0}", dto.getUuid());
            dto.setName("");
        }
        // Correctly handle nullable Double for credit_limit to prevent NullPointerException
        if (dto.getCreditLimit() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null credit_limit to 0.0 for supplier UUID: {0}", dto.getUuid());
            dto.setCreditLimit(0.0);
        }

        // Sanitize last_updated_at to prevent NullPointerException when calling .toLocalDateTime()
        if (dto.getLastUpdatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null last_updated_at to current UTC time for supplier UUID: {0}", dto.getUuid());
            dto.setLastUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        // --- END DTO SANITIZATION BLOCK ---

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            ps.setString(1, dto.getUuid());
            ps.setString(2, dto.getTenantId());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getContactPerson());
            ps.setString(5, dto.getEmail());
            ps.setString(6, dto.getPhone());
            ps.setString(7, dto.getAddress());
            ps.setString(8, dto.getPaymentTerms());
            // This line is now safe because the sanitization block guarantees getCreditLimit() is not null.
            ps.setDouble(9, dto.getCreditLimit());
            ps.setTimestamp(10, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(11, dto.isDeleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote supplier with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Retrieves all suppliers that have not been synced.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced SupplierDTOs.
     */
    public List<SupplierDTO> getUnsynced(String tenantId) {
        List<SupplierDTO> suppliers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                suppliers.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced suppliers for tenant: " + tenantId, e);
        }
        return suppliers;
    }

    /**
     * Performs a soft delete on a supplier.
     * @param supplierId The local ID of the supplier to delete.
     * @param tenantId   The UUID of the tenant.
     */
    public void markAsDeleted(long supplierId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_DELETED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, supplierId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark supplier as deleted: " + supplierId, e);
        }
    }

    /**
     * Marks a specific supplier as synced.
     * @param supplierId The local database ID of the supplier.
     * @param tenantId   The UUID of the tenant.
     */
    public void markAsSynced(long supplierId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_SYNCED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, supplierId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark supplier as synced: " + supplierId, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link SupplierDTO} object.
     * @param rs The ResultSet to map.
     * @return A populated SupplierDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private SupplierDTO mapToDTO(ResultSet rs) throws SQLException {
        SupplierDTO s = new SupplierDTO();
        s.setId(rs.getLong("id"));
        s.setUuid(rs.getString("uuid"));
        s.setTenantId(rs.getString("tenant_id"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setAddress(rs.getString("address"));
        s.setPaymentTerms(rs.getString("payment_terms"));
        s.setCreditLimit((Double) rs.getObject("credit_limit"));

        Timestamp lastUpdatedAt = rs.getTimestamp("last_updated_at");
        if (lastUpdatedAt != null) {
            s.setLastUpdatedAt(lastUpdatedAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        s.setIsSynced(rs.getInt("is_synced"));
        s.setDeleted(rs.getBoolean("is_deleted"));
        return s;
    }
}