// src/main/java/com/kmu/syncpos/dao/CustomerDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.CustomerDTO;
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
 * Data Access Object for managing customers in the database.
 * This class handles all CRUD operations for the 'customers' table,
 * including logic for synchronization status.
 */
public class CustomerDAO {

    private static final Logger LOGGER = Logger.getLogger(CustomerDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, name, email, phone, address, loyalty_points,
               last_updated_at, is_synced, is_deleted
        FROM customers
        """;

    private static final String GET_ALL_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_deleted = false ORDER BY name";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String CREATE_LOCAL_SQL = """
        INSERT INTO customers(name, email, phone, address, loyalty_points, uuid, tenant_id, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_LOCAL_SQL = """
        UPDATE customers
        SET name = ?, email = ?, phone = ?, address = ?, loyalty_points = ?, last_updated_at = ?, is_synced = false
        WHERE id = ? AND tenant_id = ? AND is_deleted = false
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO customers (uuid, tenant_id, name, email, phone, address, loyalty_points, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id,
            name = excluded.name,
            email = excluded.email,
            phone = excluded.phone,
            address = excluded.address,
            loyalty_points = excluded.loyalty_points,
            last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_DELETED_SQL = "UPDATE customers SET is_deleted = true, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_SYNCED_SQL = "UPDATE customers SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";

    /**
     * Retrieves all non-deleted customers for a specific tenant.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of active CustomerDTOs, ordered by name.
     */
    public List<CustomerDTO> getAll(String tenantId) {
        List<CustomerDTO> customers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                customers.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all customers for tenant: " + tenantId, e);
        }
        return customers;
    }

    /**
     * Retrieves all customers that have not been synced for a specific tenant.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced CustomerDTOs.
     */
    public List<CustomerDTO> getUnsynced(String tenantId) {
        List<CustomerDTO> customers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                customers.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced customers for tenant: " + tenantId, e);
        }
        return customers;
    }

    /**
     * Creates a new customer from a local change, marking it as unsynced.
     *
     * @param dto      The customer data to add.
     * @param tenantId The UUID of the tenant.
     */
    public void createLocal(CustomerDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_LOCAL_SQL)) {
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getEmail());
            ps.setString(3, dto.getPhone());
            ps.setString(4, dto.getAddress());
            ps.setObject(5, dto.getLoyaltyPoints(), Types.INTEGER); // Use setObject for nullable integers
            ps.setString(6, UUID.randomUUID().toString());
            ps.setString(7, tenantId);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create local customer: " + dto.getName(), e);
        }
    }

    /**
     * Updates an existing customer from a local change, marking it as unsynced.
     *
     * @param dto      The customer data to update.
     * @param tenantId The UUID of the tenant.
     */
    public void updateLocal(CustomerDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOCAL_SQL)) {
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getEmail());
            ps.setString(3, dto.getPhone());
            ps.setString(4, dto.getAddress());
            ps.setInt(5, dto.getLoyaltyPoints());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(7, dto.getId());
            ps.setString(8, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update local customer: " + dto.getId(), e);
        }
    }

    /**
     * Inserts or updates a customer from a remote source, marking it as synced.
     *
     * @param dto The complete customer data from the remote source.
     */
    public void upsertRemote(CustomerDTO dto) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            ps.setString(1, dto.getUuid());
            ps.setString(2, dto.getTenantId());
            ps.setString(3, dto.getName());
            ps.setString(4, dto.getEmail());
            ps.setString(5, dto.getPhone());
            ps.setString(6, dto.getAddress());
            ps.setInt(7, dto.getLoyaltyPoints());
            ps.setTimestamp(8, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(9, dto.isDeleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote customer with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Performs a soft delete on a customer by its ID.
     *
     * @param id       The local ID of the customer to delete.
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
            LOGGER.log(Level.SEVERE, "Failed to mark customer as deleted, id: " + id, e);
        }
    }

    /**
     * Marks a specific customer as synced.
     *
     * @param id       The local database ID of the customer.
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
            LOGGER.log(Level.SEVERE, "Failed to mark customer as synced, id: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link CustomerDTO} object.
     *
     * @param rs The ResultSet to map.
     * @return A populated CustomerDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private CustomerDTO mapToDTO(ResultSet rs) throws SQLException {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setName(rs.getString("name"));
        dto.setEmail(rs.getString("email"));
        dto.setPhone(rs.getString("phone"));
        dto.setAddress(rs.getString("address"));
        dto.setLoyaltyPoints(rs.getInt("loyalty_points"));

        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) {
            dto.setLastUpdatedAt(ts.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        dto.setIsSynced(rs.getInt("is_synced"));
        dto.setDeleted(rs.getBoolean("is_deleted"));
        return dto;
    }
}