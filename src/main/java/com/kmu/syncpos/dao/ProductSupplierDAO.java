// src/main/java/com/kmu/syncpos/dao/ProductSupplierDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.ProductSupplierDTO;
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
 * DAO for managing the link between products and suppliers (product_suppliers table),
 * including their specific product codes.
 */
public class ProductSupplierDAO {

    private static final Logger LOGGER = Logger.getLogger(ProductSupplierDAO.class.getName());

    private static final String BASE_SELECT_SQL = "SELECT * FROM product_suppliers";
    private static final String GET_ALL_BY_PRODUCT_ID_SQL = BASE_SELECT_SQL + " WHERE product_id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String FIND_BY_PRODUCT_AND_SUPPLIER_SQL = BASE_SELECT_SQL + " WHERE product_id = ? AND supplier_id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String CREATE_LOCAL_SQL = """
        INSERT INTO product_suppliers (uuid, tenant_id, product_id, supplier_id, supplier_product_code, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_LOCAL_SQL = """
        UPDATE product_suppliers
        SET supplier_product_code = ?, last_updated_at = ?, is_synced = false
        WHERE id = ? AND tenant_id = ? AND is_deleted = false
        """;

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO product_suppliers (uuid, tenant_id, product_id, supplier_id, supplier_product_code, last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, (SELECT id FROM products WHERE uuid = ?), (SELECT id FROM suppliers WHERE uuid = ?), ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            product_id = excluded.product_id,
            supplier_id = excluded.supplier_id,
            supplier_product_code = excluded.supplier_product_code,
            last_updated_at = excluded.last_updated_at,
            is_deleted = excluded.is_deleted,
            is_synced = true
        """;

    private static final String MARK_DELETED_SQL = "UPDATE product_suppliers SET is_deleted = true, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_SYNCED_SQL = "UPDATE product_suppliers SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";

    /**
     * Retrieves all non-deleted supplier links for a specific product.
     * @param productId The local ID of the product.
     * @param tenantId The UUID of the tenant.
     * @return A list of ProductSupplierDTOs.
     */
    public List<ProductSupplierDTO> getAllByProductId(long productId, String tenantId) {
        List<ProductSupplierDTO> links = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_BY_PRODUCT_ID_SQL)) {
            ps.setLong(1, productId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                links.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get links for product ID: " + productId, e);
        }
        return links;
    }

    /**
     * Finds a specific product-supplier link.
     * @param productId The local ID of the product.
     * @param supplierId The local ID of the supplier.
     * @param tenantId The UUID of the tenant.
     * @return A single ProductSupplierDTO if the link exists, otherwise null.
     */
    public ProductSupplierDTO findByProductAndSupplier(long productId, long supplierId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_PRODUCT_AND_SUPPLIER_SQL)) {
            ps.setLong(1, productId);
            ps.setLong(2, supplierId);
            ps.setString(3, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find link for product " + productId + " and supplier " + supplierId, e);
        }
        return null;
    }

    /**
     * Creates a new product-supplier link from a local change, marking it as unsynced.
     * @param dto The DTO containing the link information.
     * @param tenantId The UUID of the tenant.
     */
    public void createLocal(ProductSupplierDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_LOCAL_SQL)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, tenantId);
            ps.setLong(3, dto.getProductId());
            ps.setLong(4, dto.getSupplierId());
            ps.setString(5, dto.getSupplierProductCode());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create local product-supplier link", e);
        }
    }

    /**
     * Updates an existing product-supplier link from a local change, marking it as unsynced.
     * @param dto The DTO with the updated information.
     */
    public void updateLocal(ProductSupplierDTO dto) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOCAL_SQL)) {
            ps.setString(1, dto.getSupplierProductCode());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, dto.getId());
            ps.setString(4, dto.getTenantId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update local product-supplier link: " + dto.getId(), e);
        }
    }

    /**
     * Inserts or updates a product-supplier link from a remote source, marking it as synced.
     * This relies on subqueries to find local IDs from UUIDs for product and supplier.
     *
     * @param dto The DTO received from the server. Must contain product and supplier UUIDs.
     */
    public void upsertRemote(ProductSupplierDTO dto) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            int i = 1;
            ps.setString(i++, dto.getUuid());
            ps.setString(i++, dto.getTenantId());
            ps.setString(i++, dto.getProductUuid());
            ps.setString(i++, dto.getSupplierUuid());
            ps.setString(i++, dto.getSupplierProductCode());
            ps.setTimestamp(i++, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(i, dto.isDeleted());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote product-supplier link with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Retrieves all unsynced product-supplier links for a specific tenant.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced DTOs.
     */
    public List<ProductSupplierDTO> getUnsynced(String tenantId) {
        List<ProductSupplierDTO> links = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                links.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced product-supplier links for tenant: " + tenantId, e);
        }
        return links;
    }

    /**
     * Performs a soft delete on a product-supplier link.
     * @param id The local ID of the link to delete.
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
            LOGGER.log(Level.SEVERE, "Failed to mark product-supplier link as deleted: " + id, e);
        }
    }

    /**
     * Marks a specific product-supplier link as synced.
     * @param id The local ID of the link to mark as synced.
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
            LOGGER.log(Level.SEVERE, "Failed to mark product-supplier link as synced: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link ProductSupplierDTO} object.
     * @param rs The ResultSet to map.
     * @return A populated ProductSupplierDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private ProductSupplierDTO mapToDTO(ResultSet rs) throws SQLException {
        ProductSupplierDTO dto = new ProductSupplierDTO();
        dto.setId(rs.getLong("id"));
        dto.setUuid(rs.getString("uuid"));
        dto.setTenantId(rs.getString("tenant_id"));
        dto.setProductId(rs.getLong("product_id"));
        dto.setSupplierId(rs.getLong("supplier_id"));
        dto.setSupplierProductCode(rs.getString("supplier_product_code"));
        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) {
            dto.setLastUpdatedAt(ts.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }
        dto.setIsSynced(rs.getInt("is_synced"));
        dto.setDeleted(rs.getBoolean("is_deleted"));
        return dto;
    }
}