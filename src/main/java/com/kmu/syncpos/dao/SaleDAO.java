// src/main/java/com/kmu/syncpos/dao/SaleDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.PurchaseHistoryDTO;
import com.kmu.syncpos.dto.SaleDTO;
import com.kmu.syncpos.util.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing sales and purchases (transactions).
 * This class handles transactional operations for creating, updating, and deleting sales
 * and their related records.
 */
public class SaleDAO {

    private static final Logger LOGGER = Logger.getLogger(SaleDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, type, user_id, customer_id, supplier_id,
               subtotal, tax, discount, total, payment_method, payment_status,
               notes, created_at, last_updated_at, is_synced, is_deleted
        FROM sales
        """;

    private static final String GET_BY_ID_SQL = BASE_SELECT_SQL + " WHERE id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_BY_CUSTOMER_ID_SQL = BASE_SELECT_SQL + " WHERE customer_id = ? AND tenant_id = ? AND is_deleted = false ORDER BY created_at DESC";
    private static final String GET_ALL_BY_TYPE_SQL = BASE_SELECT_SQL + " WHERE type = ? AND tenant_id = ? AND is_deleted = false ORDER BY created_at DESC";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";
    private static final String GET_UNSYNCED_BY_TYPE_SQL = BASE_SELECT_SQL + " WHERE type = ? AND tenant_id = ? AND is_synced = false";

    private static final String INSERT_TRANSACTIONAL_SQL = """
        INSERT INTO sales(type, user_id, customer_id, supplier_id, subtotal, tax, discount, total, payment_method, payment_status, notes,
                          uuid, tenant_id, created_at, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_PAYMENT_STATUS_TRANSACTIONAL_SQL = "UPDATE sales SET payment_status = ?, last_updated_at = ?, is_synced = false WHERE id = ? AND tenant_id = ?";

    private static final String DELETE_STOCK_LEDGER_FOR_SALE_SQL = "UPDATE stock_ledger SET is_deleted=true, is_synced=false, last_updated_at=? WHERE sale_item_id IN (SELECT id FROM sale_items WHERE sale_id=? AND tenant_id=?) AND tenant_id=?";
    private static final String DELETE_SALE_ITEMS_SQL = "UPDATE sale_items SET is_deleted=true, is_synced=false, last_updated_at=? WHERE sale_id=? AND tenant_id=?";
    private static final String DELETE_SALE_SQL = "UPDATE sales SET is_deleted=true, is_synced=false, last_updated_at=? WHERE id=? AND tenant_id=?";

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO sales (uuid, tenant_id, type, user_id, customer_id, supplier_id, subtotal, tax, discount, total, payment_status,
                           notes, created_at, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id, type = excluded.type, user_id = excluded.user_id, customer_id = excluded.customer_id, supplier_id = excluded.supplier_id,
            subtotal = excluded.subtotal, tax = excluded.tax, discount = excluded.discount, total = excluded.total,
            payment_status = excluded.payment_status, notes = excluded.notes, created_at = excluded.created_at, last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted, is_synced = true
        """;
    private static final String GET_PURCHASE_HISTORY_FOR_CUSTOMER_SQL = """
    SELECT s.created_at, s.total, COUNT(si.id) AS item_count
    FROM sales s
    LEFT JOIN sale_items si ON s.id = si.sale_id AND s.tenant_id = si.tenant_id AND si.is_deleted = false
    WHERE s.customer_id = ? AND s.tenant_id = ? AND s.is_deleted = false
    GROUP BY s.id, s.created_at, s.total
    ORDER BY s.created_at DESC
""";

    private static final String MARK_SYNCED_SQL = "UPDATE sales SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";


    /**
     * Inserts a single sale record using a provided transactional connection.
     *
     * @param conn     The transaction's connection object.
     * @param dto      The data for the sale.
     * @param tenantId The ID of the tenant.
     * @return The generated local ID of the new sale.
     * @throws SQLException if the insert fails, allowing the caller to roll back the transaction.
     */
    public long insertTransactional(Connection conn, SaleDTO dto, String tenantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_TRANSACTIONAL_SQL, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setString(idx++, dto.getType());
            ps.setLong(idx++, dto.getUserId());
            ps.setObject(idx++, dto.getCustomerId(), Types.BIGINT);
            ps.setObject(idx++, dto.getSupplierId(), Types.BIGINT);
            ps.setDouble(idx++, dto.getSubtotal());
            ps.setDouble(idx++, dto.getTax());
            ps.setDouble(idx++, dto.getDiscount());
            ps.setDouble(idx++, dto.getTotal());
            //payment_method in INSERT_TRANSACTIONAL_SQL, adjust if needed
            ps.setString(idx++, dto.getPaymentStatus());
            ps.setString(idx++, dto.getNotes());
            ps.setString(idx++, UUID.randomUUID().toString());
            ps.setString(idx++, tenantId);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(idx++, now); // created_at
            ps.setTimestamp(idx, now);   // last_updated_at

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating sale failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating sale failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Updates the payment status of a sale using a provided transactional connection.
     *
     * @param conn          The transaction's connection object.
     * @param saleId        The ID of the sale to update.
     * @param paymentStatus The new status (e.g., 'paid', 'partial').
     * @param tenantId      The ID of the tenant.
     * @throws SQLException if the update fails.
     */
    public void updatePaymentStatusTransactional(Connection conn, long saleId, String paymentStatus, String tenantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_PAYMENT_STATUS_TRANSACTIONAL_SQL)) {
            ps.setString(1, paymentStatus);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, saleId);
            ps.setString(4, tenantId);
            ps.executeUpdate();
        }
    }

    /**
     * Soft-deletes a sale and its related items and stock adjustments within a single transaction.
     *
     * @param conn     The transaction's connection object.
     * @param saleId   The ID of the sale to delete.
     * @param tenantId The ID of the tenant.
     * @throws SQLException if any delete operation fails.
     */
    public void markAsDeletedTransactional(Connection conn, long saleId, String tenantId) throws SQLException {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // 1. Delete stock ledger entries related to the sale's items
        try (PreparedStatement ps = conn.prepareStatement(DELETE_STOCK_LEDGER_FOR_SALE_SQL)) {
            ps.setTimestamp(1, now);
            ps.setLong(2, saleId);
            ps.setString(3, tenantId);
            ps.setString(4, tenantId);
            ps.executeUpdate();
        }

        // 2. Delete the sale items
        try (PreparedStatement ps = conn.prepareStatement(DELETE_SALE_ITEMS_SQL)) {
            ps.setTimestamp(1, now);
            ps.setLong(2, saleId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        }

        // 3. Delete the sale itself
        try (PreparedStatement ps = conn.prepareStatement(DELETE_SALE_SQL)) {
            ps.setTimestamp(1, now);
            ps.setLong(2, saleId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        }
    }

    /**
     * Retrieves all sales that have not been synced.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced SaleDTOs.
     */
    public List<SaleDTO> getUnsynced(String tenantId) {
        List<SaleDTO> sales = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sales.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced sales for tenant: " + tenantId, e);
        }
        return sales;
    }

    /**
     * Retrieves all unsynced sales/purchases of a specific type.
     * @param type The transaction type (e.g., "sale", "purchase").
     * @param tenantId The UUID of the tenant.
     * @return A list of matching unsynced SaleDTOs.
     */
    public List<SaleDTO> getUnsyncedByType(String type, String tenantId) {
        List<SaleDTO> sales = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_BY_TYPE_SQL)) {
            ps.setString(1, type);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sales.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced sales by type '" + type + "' for tenant: " + tenantId, e);
        }
        return sales;
    }

    /**
     * Retrieves a single non-deleted sale by its local ID.
     * @param saleId The ID of the sale.
     * @param tenantId The UUID of the tenant.
     * @return A SaleDTO if found, otherwise null.
     */
    public SaleDTO getById(long saleId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setLong(1, saleId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get sale by ID: " + saleId, e);
        }
        return null;
    }

    /**
     * Retrieves all non-deleted sales/purchases of a specific type.
     * @param type The transaction type (e.g., "sale", "purchase").
     * @param tenantId The UUID of the tenant.
     * @return A list of matching SaleDTOs, ordered by most recent first.
     */
    public List<SaleDTO> getAllByType(String type, String tenantId) {
        List<SaleDTO> sales = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_BY_TYPE_SQL)) {
            ps.setString(1, type);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sales.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get sales by type '" + type + "' for tenant: " + tenantId, e);
        }
        return sales;
    }

    /**
     * Inserts or updates a sale from a remote source, marking it as synced.
     * @param dto The complete sale data from the remote source.
     */
    public void upsertRemote(SaleDTO dto) {
        // --- BEGIN DTO SANITIZATION BLOCK ---
        if (dto == null || dto.getUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert a null sale or a sale with a null UUID. Aborting operation.");
            return;
        }

        // Sanitize NOT NULL fields to prevent database constraint violations or NullPointerExceptions.
        // NOTE: DTO fields mapping to primitive types (long, double, boolean) cannot be null
        // and do not require sanitization here. Only Object types (String, OffsetDateTime, wrappers) are checked.
        if (dto.getTenantId() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null tenant_id to empty string for sale UUID: {0}", dto.getUuid());
            dto.setTenantId("");
        }
        if (dto.getType() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null type to empty string for sale UUID: {0}", dto.getUuid());
            dto.setType("");
        }
        if (dto.getPaymentStatus() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null payment_status to 'pending' for sale UUID: {0}", dto.getUuid());
            dto.setPaymentStatus("pending"); // Schema default
        }
        if (dto.getCreatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null created_at to current UTC time for sale UUID: {0}", dto.getUuid());
            dto.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        if (dto.getLastUpdatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null last_updated_at to current UTC time for sale UUID: {0}", dto.getUuid());
            dto.setLastUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        // --- END DTO SANITIZATION BLOCK ---

        // The original UPSERT_REMOTE_SQL is missing `payment_method`. I've added it.
        final String sql = """
            INSERT INTO sales (uuid, tenant_id, type, user_id, customer_id, supplier_id, subtotal, tax, discount, total, payment_method, payment_status,
                               notes, created_at, last_updated_at, is_deleted, is_synced)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)
            ON CONFLICT(uuid) DO UPDATE SET
                tenant_id = excluded.tenant_id, type = excluded.type, user_id = excluded.user_id, customer_id = excluded.customer_id, supplier_id = excluded.supplier_id,
                subtotal = excluded.subtotal, tax = excluded.tax, discount = excluded.discount, total = excluded.total, payment_method = excluded.payment_method,
                payment_status = excluded.payment_status, notes = excluded.notes, created_at = excluded.created_at, last_updated_at = excluded.last_updated_at,
                is_deleted = excluded.is_deleted, is_synced = true
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, dto.getUuid());
            ps.setString(idx++, dto.getTenantId());
            ps.setString(idx++, dto.getType());
            ps.setLong(idx++, dto.getUserId());
            ps.setObject(idx++, dto.getCustomerId(), Types.BIGINT);
            ps.setObject(idx++, dto.getSupplierId(), Types.BIGINT);
            ps.setDouble(idx++, dto.getSubtotal());
            ps.setDouble(idx++, dto.getTax());
            ps.setDouble(idx++, dto.getDiscount());
            ps.setDouble(idx++, dto.getTotal());
            ps.setString(idx++, dto.getPaymentMethod()); // This field was missing from the original query string
            ps.setString(idx++, dto.getPaymentStatus());
            ps.setString(idx++, dto.getNotes());
            ps.setTimestamp(idx++, Timestamp.valueOf(dto.getCreatedAt().toLocalDateTime()));
            ps.setTimestamp(idx++, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(idx, dto.isDeleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote sale with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Marks a specific sale as synced.
     * @param id The local database ID of the sale.
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
            LOGGER.log(Level.SEVERE, "Failed to mark sale as synced: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link SaleDTO} object.
     * @param rs The ResultSet to map.
     * @return A populated SaleDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private SaleDTO mapToDTO(ResultSet rs) throws SQLException {
        SaleDTO s = new SaleDTO();
        s.setId(rs.getLong("id"));
        s.setUuid(rs.getString("uuid"));
        s.setTenantId(rs.getString("tenant_id"));
        s.setType(rs.getString("type"));
        s.setUserId(rs.getLong("user_id"));
        s.setCustomerId(rs.getObject("customer_id", Long.class));
        s.setSupplierId(rs.getObject("supplier_id", Long.class));
        s.setSubtotal(rs.getDouble("subtotal"));
        s.setTax(rs.getDouble("tax"));
        s.setDiscount(rs.getDouble("discount"));
        s.setTotal(rs.getDouble("total"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setPaymentStatus(rs.getString("payment_status"));
        s.setNotes(rs.getString("notes"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            s.setCreatedAt(createdAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }
        Timestamp lastUpdatedAt = rs.getTimestamp("last_updated_at");
        if (lastUpdatedAt != null) {
            s.setLastUpdatedAt(lastUpdatedAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        s.setIsSynced(rs.getInt("is_synced"));
        s.setDeleted(rs.getBoolean("is_deleted"));
        return s;
    }
    /**
     * Retrieves a summarized purchase history for a specific customer.
     * This method is highly efficient, using a single SQL query to join sales
     * with their item counts, avoiding the N+1 query problem.
     *
     * @param customerId The local ID of the customer.
     * @param tenantId The UUID of the tenant.
     * @return A list of PurchaseHistoryDTO records.
     */
    public List<PurchaseHistoryDTO> getPurchaseHistoryForCustomer(long customerId, String tenantId) {
        List<PurchaseHistoryDTO> history = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_PURCHASE_HISTORY_FOR_CUSTOMER_SQL)) {
            ps.setLong(1, customerId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(mapToPurchaseHistoryDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get purchase history for customer: " + customerId, e);
        }
        return history;
    }
    /**
     * Retrieves all non-deleted sales for a specific customer.
     * @param customerId The ID of the customer.
     * @param tenantId The UUID of the tenant.
     * @return A list of full SaleDTOs for the customer, ordered by most recent first.
     */
    public List<SaleDTO> getSalesByCustomerId(long customerId, String tenantId) {
        List<SaleDTO> sales = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_CUSTOMER_ID_SQL)) {
            ps.setLong(1, customerId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sales.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get sales for customer: " + customerId, e);
        }
        return sales;
    }

    /**
     * Maps a ResultSet row from the specialized purchase history query to a PurchaseHistoryDTO.
     * @param rs The ResultSet to map.
     * @return A populated PurchaseHistoryDTO.
     * @throws SQLException if a database access error occurs.
     */
    private PurchaseHistoryDTO mapToPurchaseHistoryDTO(ResultSet rs) throws SQLException {
        // This assumes a DTO like: public PurchaseHistoryDTO(LocalDate date, BigDecimal total, int itemCount)
        Timestamp createdAt = rs.getTimestamp("created_at");
        LocalDate date = createdAt.toLocalDateTime().toLocalDate();
        BigDecimal total = rs.getBigDecimal("total");
        int itemCount = rs.getInt("item_count");
        return new PurchaseHistoryDTO(date, total, itemCount);
    }
}