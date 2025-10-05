// src/main/java/com/kmu/syncpos/dao/SaleItemDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.SaleItemDTO;
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
 * Data Access Object for managing sale items.
 * Sale items are children of a Sale record and are typically created
 * within a single database transaction.
 */
public class SaleItemDAO {

    private static final Logger LOGGER = Logger.getLogger(SaleItemDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, sale_id, product_id, supplier_product_code, quantity, unit_price, cost_at_sale,
               tax_rate, discount, total, last_updated_at, is_synced, is_deleted
        FROM sale_items
        """;

    private static final String GET_BY_SALE_ID_SQL = BASE_SELECT_SQL + " WHERE sale_id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";
    private static final String GET_COUNT_BY_SALE_ID_SQL = "SELECT COUNT(*) FROM sale_items WHERE sale_id = ? AND tenant_id = ? AND is_deleted = false";

    private static final String INSERT_TRANSACTIONAL_SQL = """
        INSERT INTO sale_items(sale_id, product_id, supplier_product_code, quantity, unit_price, tax_rate, discount, total, cost_at_sale,
                               uuid, tenant_id, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO sale_items (uuid, tenant_id, sale_id, product_id, supplier_product_code,
                                quantity, unit_price, cost_at_sale, tax_rate, discount, total, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, (SELECT id FROM sales WHERE uuid = ?), (SELECT id FROM products WHERE uuid = ?), ?, ?, ?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id,
            sale_id = (SELECT id FROM sales WHERE uuid = ?),
            product_id = (SELECT id FROM products WHERE uuid = ?),
            supplier_product_code = excluded.supplier_product_code,
            quantity = excluded.quantity, unit_price = excluded.unit_price, cost_at_sale = excluded.cost_at_sale, tax_rate = excluded.tax_rate,
            discount = excluded.discount, total = excluded.total, last_updated_at = excluded.last_updated_at, is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_SYNCED_SQL = "UPDATE sale_items SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";


    /**
     * Inserts a single sale item using a provided transactional connection.
     *
     * @param conn     The transaction's connection object.
     * @param dto      The data for the sale item.
     * @param saleId   The ID of the parent sale record.
     * @param tenantId The ID of the tenant.
     * @return The generated local ID of the new sale item.
     * @throws SQLException if the insert fails, allowing the caller to roll back the transaction.
     */
    public long insertTransactional(Connection conn, SaleItemDTO dto, long saleId, String tenantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_TRANSACTIONAL_SQL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, saleId);
            ps.setLong(i++, dto.getProductId());
            ps.setString(i++, dto.getSupplierProductCode());
            ps.setDouble(i++, dto.getQuantity());
            ps.setDouble(i++, dto.getUnitPrice());
            ps.setDouble(i++, dto.getTaxRate());
            ps.setDouble(i++, dto.getDiscount());
            ps.setDouble(i++, dto.getTotal());
            ps.setDouble(i++, dto.getCostAtSale());
            ps.setString(i++, UUID.randomUUID().toString());
            ps.setString(i++, tenantId);
            ps.setTimestamp(i, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating sale item failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating sale item failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Inserts or updates a sale item from a remote source, marking it as synced.
     * Relies on subqueries to find local IDs from UUIDs for the parent sale and product.
     *
     * @param dto The DTO from the remote source. Must contain sale and product UUIDs.
     */
    public void upsertRemote(SaleItemDTO dto) {
        // --- BEGIN DTO SANITIZATION BLOCK ---
        if (dto == null || dto.getUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert a null sale item or an item with a null UUID. Aborting operation.");
            return;
        }

        // A sale item must belong to a sale and a product. Abort if these critical foreign key UUIDs are missing.
        if (dto.getSaleUuid() == null || dto.getProductUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert sale item UUID {0} with a null sale_uuid or product_uuid. Aborting operation.", dto.getUuid());
            return;
        }

        if (dto.getTenantId() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null tenant_id to empty string for sale item UUID: {0}", dto.getUuid());
            dto.setTenantId("");
        }

        // Note: The primitive doubles (quantity, price, etc.) cannot be null.
        // If they were missing from a remote source, the deserializer would have already defaulted them to 0.0.
        // Therefore, null checks are not necessary for them here.

        // Prevent NullPointerException on .toLocalDateTime() later in the method.
        if (dto.getLastUpdatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null last_updated_at to current UTC time for sale item UUID: {0}", dto.getUuid());
            dto.setLastUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        // --- END DTO SANITIZATION BLOCK ---

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            int i = 1;
            // INSERT part
            ps.setString(i++, dto.getUuid());
            ps.setString(i++, dto.getTenantId());
            ps.setString(i++, dto.getSaleUuid());
            ps.setString(i++, dto.getProductUuid());
            ps.setString(i++, dto.getSupplierProductCode());
            ps.setDouble(i++, dto.getQuantity());
            ps.setDouble(i++, dto.getUnitPrice());
            ps.setDouble(i++, dto.getCostAtSale());
            ps.setDouble(i++, dto.getTaxRate());
            ps.setDouble(i++, dto.getDiscount());
            ps.setDouble(i++, dto.getTotal());
            ps.setTimestamp(i++, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(i++, dto.isDeleted());

            // ON CONFLICT UPDATE part (re-binding UUIDs for subqueries)
            ps.setString(i++, dto.getSaleUuid());
            ps.setString(i, dto.getProductUuid());

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote sale item with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Retrieves all non-deleted items for a given sale.
     *
     * @param saleId   The ID of the parent sale.
     * @param tenantId The UUID of the tenant.
     * @return A list of SaleItemDTOs.
     */
    public List<SaleItemDTO> getSaleItemsBySaleId(long saleId, String tenantId) {
        List<SaleItemDTO> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_SALE_ID_SQL)) {
            ps.setLong(1, saleId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get sale items for sale ID: " + saleId, e);
        }
        return items;
    }

    /**
     * Retrieves all sale items that have not been synced.
     *
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced SaleItemDTOs.
     */
    public List<SaleItemDTO> getUnsynced(String tenantId) {
        List<SaleItemDTO> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced sale items for tenant: " + tenantId, e);
        }
        return items;
    }

    /**
     * Marks a specific sale item as synced.
     *
     * @param id       The local database ID of the sale item.
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
            LOGGER.log(Level.SEVERE, "Failed to mark sale item as synced: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link SaleItemDTO} object.
     *
     * @param rs The ResultSet to map.
     * @return A populated SaleItemDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private SaleItemDTO mapToDTO(ResultSet rs) throws SQLException {
        SaleItemDTO dto = new SaleItemDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setSaleId(rs.getLong("sale_id"));
        dto.setProductId(rs.getLong("product_id"));
        dto.setSupplierProductCode(rs.getString("supplier_product_code"));
        dto.setQuantity(rs.getDouble("quantity"));
        dto.setUnitPrice(rs.getDouble("unit_price"));
        dto.setCostAtSale(rs.getDouble("cost_at_sale"));
        dto.setTaxRate(rs.getDouble("tax_rate"));
        dto.setDiscount(rs.getDouble("discount"));
        dto.setTotal(rs.getDouble("total"));
        Timestamp lastUpdatedAt = rs.getTimestamp("last_updated_at");
        if (lastUpdatedAt != null) {
            dto.setLastUpdatedAt(lastUpdatedAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }
        dto.setIsSynced(rs.getInt("is_synced"));
        dto.setDeleted(rs.getBoolean("is_deleted"));
        return dto;
    }
}