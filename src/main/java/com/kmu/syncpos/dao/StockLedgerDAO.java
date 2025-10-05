// src/main/java/com/kmu/syncpos/dao/StockLedgerDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.StockLedgerDTO;
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
 * Data Access Object for managing stock ledger entries.
 * The stock ledger is an append-only table that records all movements of stock.
 * Entries are typically created within a larger transaction (e.g., a sale or manual adjustment).
 */
public class StockLedgerDAO {

    private static final Logger LOGGER = Logger.getLogger(StockLedgerDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, product_id, quantity_delta, reason,
               sale_item_id, user_id, notes, created_at, last_updated_at,
               is_deleted, is_synced
        FROM stock_ledger
        """;

    private static final String GET_BY_ID_SQL = BASE_SELECT_SQL + " WHERE id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_FOR_PRODUCT_SQL = BASE_SELECT_SQL + " WHERE product_id = ? AND tenant_id = ? AND is_deleted = false ORDER BY created_at DESC";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String INSERT_TRANSACTIONAL_SQL = """
        INSERT INTO stock_ledger(product_id, quantity_delta, reason, sale_item_id, user_id, notes,
                                 uuid, tenant_id, created_at, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO stock_ledger (uuid, tenant_id, product_id, quantity_delta, reason, sale_item_id, user_id, notes,
                                  created_at, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, (SELECT id FROM products WHERE uuid = ?), ?, ?,
                (SELECT id FROM sale_items WHERE uuid = ?), ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id,
            product_id = (SELECT id FROM products WHERE uuid = ?),
            quantity_delta = excluded.quantity_delta,
            reason = excluded.reason,
            sale_item_id = (SELECT id FROM sale_items WHERE uuid = ?),
            user_id = excluded.user_id,
            notes = excluded.notes,
            created_at = excluded.created_at,
            last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_SYNCED_SQL = "UPDATE stock_ledger SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";


    /**
     * Inserts a single stock ledger entry using a provided transactional connection.
     *
     * @param conn     The transaction's connection object.
     * @param dto      The data for the stock adjustment.
     * @param tenantId The ID of the tenant.
     * @throws SQLException if the insert fails, allowing the caller to roll back the transaction.
     */
    public void insertTransactional(Connection conn, StockLedgerDTO dto, String tenantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_TRANSACTIONAL_SQL)) {
            ps.setLong(1, dto.getProductId());
            ps.setDouble(2, dto.getQuantityDelta());
            ps.setString(3, dto.getReason());
            ps.setObject(4, dto.getSaleItemId(), Types.BIGINT);
            ps.setLong(5, dto.getUserId());
            ps.setString(6, dto.getNotes());
            ps.setString(7, UUID.randomUUID().toString());
            ps.setString(8, tenantId);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(9, now);  // created_at
            ps.setTimestamp(10, now); // last_updated_at
            ps.executeUpdate();
        }
    }

    /**
     * Inserts or updates a stock ledger entry from a remote source, marking it as synced.
     * Relies on subqueries to find local IDs from UUIDs for product and the associated sale item.
     *
     * @param dto The DTO from the remote source. Must contain relevant UUIDs.
     */
    public void upsertRemote(StockLedgerDTO dto) {
        // --- BEGIN DTO SANITIZATION BLOCK ---
        if (dto == null || dto.getUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert a null stock ledger entry or one with a null UUID. Aborting operation.");
            return;
        }

        // A stock ledger entry MUST be associated with a product. Abort if this critical foreign key is missing.
        if (dto.getProductUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert stock ledger entry UUID {0} with a null product_uuid. Aborting operation.", dto.getUuid());
            return;
        }

        // Sanitize other NOT NULL fields to prevent database constraint violations or NullPointerExceptions.
        if (dto.getTenantId() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null tenant_id to empty string for stock ledger UUID: {0}", dto.getUuid());
            dto.setTenantId("");
        }
        if (dto.getReason() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null reason to empty string for stock ledger UUID: {0}", dto.getUuid());
            dto.setReason("");
        }
        // Note: The primitive types (quantity_delta, user_id, is_deleted) cannot be null and don't need checks.

        // Prevent NullPointerException on .toLocalDateTime() later in the method for nullable date fields.
        if (dto.getCreatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null created_at to current UTC time for stock ledger UUID: {0}", dto.getUuid());
            dto.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        if (dto.getLastUpdatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null last_updated_at to current UTC time for stock ledger UUID: {0}", dto.getUuid());
            dto.setLastUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        // --- END DTO SANITIZATION BLOCK ---

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            int i = 1;
            // INSERT part
            ps.setString(i++, dto.getUuid());
            ps.setString(i++, dto.getTenantId());
            ps.setString(i++, dto.getProductUuid());
            ps.setDouble(i++, dto.getQuantityDelta());
            ps.setString(i++, dto.getReason());
            ps.setString(i++, dto.getSaleItemUuid()); // Can be null
            ps.setLong(i++, dto.getUserId());
            ps.setString(i++, dto.getNotes());
            ps.setTimestamp(i++, Timestamp.valueOf(dto.getCreatedAt().toLocalDateTime()));
            ps.setTimestamp(i++, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(i++, dto.isDeleted());

            // ON CONFLICT UPDATE part (re-binding UUIDs for subqueries)
            ps.setString(i++, dto.getProductUuid());
            ps.setString(i, dto.getSaleItemUuid());

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote stock ledger entry with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Retrieves a single non-deleted stock ledger entry by its local database ID.
     * @param id The ID of the stock ledger entry.
     * @param tenantId The UUID of the tenant.
     * @return A StockLedgerDTO if found, otherwise null.
     */
    public StockLedgerDTO getById(long id, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setLong(1, id);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get stock ledger entry by ID: " + id, e);
        }
        return null;
    }

    /**
     * Retrieves all stock ledger entries that have not been synced.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced StockLedgerDTOs.
     */
    public List<StockLedgerDTO> getUnsynced(String tenantId) {
        List<StockLedgerDTO> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced stock ledger entries for tenant: " + tenantId, e);
        }
        return entries;
    }

    /**
     * Retrieves all non-deleted stock ledger entries for a specific product.
     *
     * @param productId The ID of the product.
     * @param tenantId  The UUID of the tenant.
     * @return A list of StockLedgerDTOs representing the product's history.
     */
    public List<StockLedgerDTO> getEntriesForProduct(long productId, String tenantId) {
        List<StockLedgerDTO> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_FOR_PRODUCT_SQL)) {
            ps.setLong(1, productId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get ledger entries for product: " + productId, e);
        }
        return entries;
    }

    /**
     * Marks a specific stock ledger entry as synced.
     *
     * @param id       The local database ID of the entry.
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
            LOGGER.log(Level.SEVERE, "Failed to mark stock ledger entry as synced: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link StockLedgerDTO} object.
     *
     * @param rs The ResultSet to map.
     * @return A populated StockLedgerDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private StockLedgerDTO mapToDTO(ResultSet rs) throws SQLException {
        StockLedgerDTO dto = new StockLedgerDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setProductId(rs.getLong("product_id"));
        dto.setQuantityDelta(rs.getDouble("quantity_delta"));
        dto.setReason(rs.getString("reason"));
        dto.setSaleItemId(rs.getObject("sale_item_id", Long.class));
        dto.setUserId(rs.getLong("user_id"));
        dto.setNotes(rs.getString("notes"));
        dto.setDeleted(rs.getBoolean("is_deleted"));
        dto.setIsSynced(rs.getInt("is_synced"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            dto.setCreatedAt(createdAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }
        Timestamp lastUpdatedAt = rs.getTimestamp("last_updated_at");
        if (lastUpdatedAt != null) {
            dto.setLastUpdatedAt(lastUpdatedAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }
        return dto;
    }
}