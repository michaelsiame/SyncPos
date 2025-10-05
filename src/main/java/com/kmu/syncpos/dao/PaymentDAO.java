// src/main/java/com/kmu/syncpos/dao/PaymentDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.PaymentDTO;
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
 * Data Access Object for managing payments in the database.
 * Payments are typically created as part of a larger sale transaction
 * and are considered mostly immutable once created.
 */
public class PaymentDAO {

    private static final Logger LOGGER = Logger.getLogger(PaymentDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, sale_id, amount, payment_method,
               reference, user_id, created_at, last_updated_at,
               is_synced, is_deleted
        FROM payments
        """;

    private static final String GET_FOR_SALE_SQL = BASE_SELECT_SQL + " WHERE sale_id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String INSERT_TRANSACTIONAL_SQL = """
        INSERT INTO payments(sale_id, amount, payment_method, reference, user_id, uuid, tenant_id, created_at, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO payments (uuid, tenant_id, sale_id, amount, payment_method, reference, user_id, created_at, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, (SELECT id FROM sales WHERE uuid = ?), ?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id,
            sale_id = (SELECT id FROM sales WHERE uuid = ?),
            amount = excluded.amount,
            payment_method = excluded.payment_method,
            reference = excluded.reference,
            user_id = excluded.user_id,
            created_at = excluded.created_at,
            last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_SYNCED_SQL = "UPDATE payments SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";

    /**
     * Inserts a single payment record using a provided transactional connection.
     * This method does not commit or close the connection.
     *
     * @param conn     The transactional connection object.
     * @param dto      The data for the payment.
     * @param tenantId The ID of the tenant.
     * @throws SQLException if the insert fails, allowing the caller to roll back the transaction.
     */
    public void insertTransactional(Connection conn, PaymentDTO dto, String tenantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_TRANSACTIONAL_SQL)) {
            ps.setLong(1, dto.getSaleId());
            ps.setDouble(2, dto.getAmount());
            ps.setString(3, dto.getPaymentMethod());
            ps.setString(4, dto.getReference());
            ps.setLong(5, dto.getUserId());
            ps.setString(6, UUID.randomUUID().toString());
            ps.setString(7, tenantId);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(8, now); // created_at
            ps.setTimestamp(9, now); // last_updated_at
            ps.executeUpdate();
        }
    }

    /**
     * Inserts or updates a payment from a remote source, marking it as synced.
     * This method relies on a subquery to find the local 'sale_id' from the sale's UUID.
     * The associated Sale record must already be synced for this to succeed.
     *
     * @param dto The payment data from the remote source. It must contain the sale's UUID via `getSaleUuid()`.
     */
    public void upsertRemote(PaymentDTO dto) {
        // --- BEGIN DTO SANITIZATION BLOCK ---
        if (dto == null || dto.getUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert a null payment or a payment with a null UUID. Aborting operation.");
            return;
        }

        // A payment must belong to a sale. Abort if this critical foreign key UUID is missing.
        if (dto.getSaleUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert payment UUID {0} with a null sale_uuid. Aborting operation.", dto.getUuid());
            return;
        }

        // Sanitize other NOT NULL fields to prevent database constraint violations.
        if (dto.getTenantId() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null tenant_id to empty string for payment UUID: {0}", dto.getUuid());
            dto.setTenantId("");
        }
        if (dto.getPaymentMethod() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null payment_method to empty string for payment UUID: {0}", dto.getUuid());
            dto.setPaymentMethod("");
        }
        // Note: The primitive types (amount, user_id, is_deleted) cannot be null and do not require sanitization.

        // Sanitize date fields to prevent NullPointerExceptions.
        if (dto.getCreatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null created_at to current UTC time for payment UUID: {0}", dto.getUuid());
            dto.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        if (dto.getLastUpdatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null last_updated_at to current UTC time for payment UUID: {0}", dto.getUuid());
            dto.setLastUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        // --- END DTO SANITIZATION BLOCK ---

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            int i = 1;
            // INSERT part
            ps.setString(i++, dto.getUuid());
            ps.setString(i++, dto.getTenantId());
            ps.setString(i++, dto.getSaleUuid()); // Critical dependency for subquery
            ps.setDouble(i++, dto.getAmount());
            ps.setString(i++, dto.getPaymentMethod());
            ps.setString(i++, dto.getReference());
            ps.setLong(i++, dto.getUserId());
            ps.setTimestamp(i++, Timestamp.valueOf(dto.getCreatedAt().toLocalDateTime()));
            ps.setTimestamp(i++, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(i++, dto.isDeleted());

            // ON CONFLICT UPDATE part
            ps.setString(i, dto.getSaleUuid()); // Re-bind for the update clause's subquery

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote payment with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Retrieves all non-deleted payments for a given sale.
     * This is a read-only operation and manages its own connection.
     *
     * @param saleId   The local ID of the sale.
     * @param tenantId The ID of the tenant.
     * @return A list of PaymentDTOs.
     */
    public List<PaymentDTO> getPaymentsForSale(long saleId, String tenantId) {
        List<PaymentDTO> payments = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_FOR_SALE_SQL)) {
            ps.setLong(1, saleId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                payments.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get payments for sale: " + saleId, e);
        }
        return payments;
    }

    /**
     * Retrieves all payments for a sale using an existing database connection.
     *
     * @param conn     The existing transaction's connection object.
     * @param saleId   The local ID of the sale.
     * @param tenantId The ID of the tenant.
     * @return A list of PaymentDTOs.
     * @throws SQLException if a database access error occurs.
     */
    public List<PaymentDTO> getPaymentsForSaleTransactional(Connection conn, long saleId, String tenantId) throws SQLException {
        List<PaymentDTO> payments = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(GET_FOR_SALE_SQL)) {
            ps.setLong(1, saleId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                payments.add(mapToDTO(rs));
            }
        }
        return payments;
    }

    /**
     * Retrieves all payments that have not been synced.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced PaymentDTOs.
     */
    public List<PaymentDTO> getUnsynced(String tenantId) {
        List<PaymentDTO> payments = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                payments.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced payments for tenant: " + tenantId, e);
        }
        return payments;
    }

    /**
     * Marks a specific payment as synced.
     *
     * @param id       The local database ID of the payment.
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
            LOGGER.log(Level.SEVERE, "Failed to mark payment as synced, id: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link PaymentDTO} object.
     *
     * @param rs The ResultSet to map.
     * @return A populated PaymentDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private PaymentDTO mapToDTO(ResultSet rs) throws SQLException {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setSaleId(rs.getLong("sale_id"));
        dto.setAmount(rs.getDouble("amount"));
        dto.setPaymentMethod(rs.getString("payment_method"));
        dto.setReference(rs.getString("reference"));
        dto.setUserId(rs.getLong("user_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            dto.setCreatedAt(createdAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }
        Timestamp lastUpdatedAt = rs.getTimestamp("last_updated_at");
        if (lastUpdatedAt != null) {
            dto.setLastUpdatedAt(lastUpdatedAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        dto.setIsSynced(rs.getInt("is_synced"));
        dto.setDeleted(rs.getBoolean("is_deleted"));
        return dto;
    }
}