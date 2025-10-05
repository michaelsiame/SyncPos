// src/main/java/com/kmu/syncpos/dao/ProductDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.ProductDTO;
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
 * Data Access Object for managing products in the database.
 * This class handles complex queries including stock level calculation
 * and transactional operations for creating products.
 */
public class ProductDAO {

    private static final Logger LOGGER = Logger.getLogger(ProductDAO.class.getName());

    private static final String SELECT_WITH_STOCK_SQL = """
        SELECT p.id, p.uuid, p.tenant_id, p.sku, p.barcode, p.name, p.description,
               p.product_type, p.category_id, p.unit_id, p.supplier_id,
               p.purchase_price, p.selling_price, p.tax_rate, p.min_stock_level,
               p.reorder_quantity, p.is_active, p.last_updated_at, p.is_synced, p.is_deleted,
               COALESCE(SUM(sl.quantity_delta), 0) AS current_stock
        FROM products p
        LEFT JOIN stock_ledger sl
               ON sl.product_id = p.id AND sl.tenant_id = p.tenant_id AND sl.is_deleted = false
        """;

    private static final String GET_ALL_SQL = SELECT_WITH_STOCK_SQL + " WHERE p.tenant_id = ? AND p.is_deleted = false GROUP BY p.id ORDER BY p.name";
    private static final String GET_BY_ID_SQL = SELECT_WITH_STOCK_SQL + " WHERE p.id = ? AND p.tenant_id = ? AND p.is_deleted = false GROUP BY p.id";
    private static final String GET_LOW_STOCK_SQL = SELECT_WITH_STOCK_SQL + " WHERE p.tenant_id = ? AND p.is_deleted = false AND p.is_active = true GROUP BY p.id HAVING current_stock <= p.min_stock_level ORDER BY p.name";

    private static final String GET_UNSYNCED_SQL = """
        SELECT p.*, 0 AS current_stock
        FROM products p
        WHERE p.tenant_id = ? AND p.is_synced = false
        """;

    private static final String CREATE_LOCAL_TRANSACTIONAL_SQL = """
        INSERT INTO products(sku, barcode, name, description, product_type, category_id, unit_id, supplier_id,
                             purchase_price, selling_price, tax_rate, min_stock_level, reorder_quantity, is_active,
                             uuid, tenant_id, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_LOCAL_SQL = """
        UPDATE products
        SET sku = ?, barcode = ?, name = ?, description = ?, product_type = ?, category_id = ?, unit_id = ?, supplier_id = ?,
            purchase_price = ?, selling_price = ?, tax_rate = ?, min_stock_level = ?, reorder_quantity = ?, is_active = ?,
            last_updated_at = ?, is_synced = false
        WHERE id = ? AND tenant_id = ? AND is_deleted = false
        """;

    // --- CORRECTED SQL QUERY ---
    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO products (uuid, tenant_id, sku, barcode, name, description, product_type,
                              category_id, unit_id, supplier_id,
                              purchase_price, selling_price, tax_rate, min_stock_level, reorder_quantity, is_active,
                              last_updated_at, is_deleted, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, ?,
                (SELECT id FROM categories WHERE uuid = ?),
                (SELECT id FROM units WHERE uuid = ?),
                (SELECT id FROM suppliers WHERE uuid = ?),
                ?, ?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id, sku = excluded.sku, barcode = excluded.barcode, name = excluded.name, description = excluded.description,
            product_type = excluded.product_type,
            category_id = (SELECT id FROM categories WHERE uuid = ?),
            unit_id = (SELECT id FROM units WHERE uuid = ?),
            supplier_id = (SELECT id FROM suppliers WHERE uuid = ?),
            purchase_price = excluded.purchase_price, selling_price = excluded.selling_price, tax_rate = excluded.tax_rate,
            min_stock_level = excluded.min_stock_level, reorder_quantity = excluded.reorder_quantity, is_active = excluded.is_active,
            last_updated_at = excluded.last_updated_at, is_deleted = excluded.is_deleted, is_synced = true
        """;

    private static final String DEACTIVATE_SQL = "UPDATE products SET is_active = false, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_DELETED_SQL = "UPDATE products SET is_deleted = true, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_SYNCED_SQL = "UPDATE products SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";

    /**
     * Retrieves all non-deleted, active products for a tenant, including their current stock levels.
     * @param tenantId The UUID of the tenant.
     * @return A list of ProductDTOs.
     */
    public List<ProductDTO> getAll(String tenantId) {
        List<ProductDTO> products = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                products.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all products for tenant: " + tenantId, e);
        }
        return products;
    }

    /**
     * Retrieves a single product by its local ID, including its current stock level.
     * @param productId The local ID of the product.
     * @param tenantId The UUID of the tenant.
     * @return A ProductDTO if found, otherwise null.
     */
    public ProductDTO getById(long productId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setLong(1, productId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get product by ID: " + productId, e);
        }
        return null;
    }

    /**
     * Creates a new product using a provided transactional connection. Returns the new product's generated ID.
     *
     * @param conn     The transactional connection object.
     * @param dto      The ProductDTO containing the new product's data.
     * @param tenantId The UUID of the tenant.
     * @return The generated local ID of the new product.
     * @throws SQLException if the insert fails, allowing the caller to roll back the transaction.
     */
    public long createLocalTransactional(Connection conn, ProductDTO dto, String tenantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(CREATE_LOCAL_TRANSACTIONAL_SQL, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            ps.setString(idx++, dto.getSku());
            ps.setString(idx++, dto.getBarcode());
            ps.setString(idx++, dto.getName());
            ps.setString(idx++, dto.getDescription());
            ps.setString(idx++, dto.getProductType());
            ps.setObject(idx++, dto.getCategoryId(), Types.BIGINT);
            ps.setObject(idx++, dto.getUnitId(), Types.BIGINT);
            ps.setObject(idx++, dto.getSupplierId(), Types.BIGINT);
            ps.setObject(idx++, dto.getPurchasePrice(), Types.DOUBLE);
            ps.setObject(idx++, dto.getSellingPrice(), Types.DOUBLE);
            ps.setObject(idx++, dto.getTaxRate(), Types.DOUBLE);
            ps.setObject(idx++, dto.getMinStockLevel(), Types.DOUBLE);
            ps.setObject(idx++, dto.getReorderQuantity(), Types.DOUBLE);
            ps.setBoolean(idx++, dto.isActive());
            ps.setString(idx++, UUID.randomUUID().toString());
            ps.setString(idx++, tenantId);
            ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Updates an existing product from a local change, marking it as unsynced.
     * @param dto      The product data to update.
     * @param tenantId The UUID of the tenant.
     */
    public void updateLocal(ProductDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOCAL_SQL)) {
            int idx = 1;
            ps.setString(idx++, dto.getSku());
            ps.setString(idx++, dto.getBarcode());
            ps.setString(idx++, dto.getName());
            ps.setString(idx++, dto.getDescription());
            ps.setString(idx++, dto.getProductType());
            ps.setObject(idx++, dto.getCategoryId(), Types.BIGINT);
            ps.setObject(idx++, dto.getUnitId(), Types.BIGINT);
            ps.setObject(idx++, dto.getSupplierId(), Types.BIGINT);
            ps.setObject(idx++, dto.getPurchasePrice(), Types.DOUBLE);
            ps.setObject(idx++, dto.getSellingPrice(), Types.DOUBLE);
            ps.setObject(idx++, dto.getTaxRate(), Types.DOUBLE);
            ps.setObject(idx++, dto.getMinStockLevel(), Types.DOUBLE);
            ps.setObject(idx++, dto.getReorderQuantity(), Types.DOUBLE);
            ps.setBoolean(idx++, dto.isActive());
            ps.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(idx++, dto.getId());
            ps.setString(idx, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update local product: " + dto.getId(), e);
        }
    }

    /**
     * Inserts or updates a product from a remote source, marking it as synced.
     * This method relies on subqueries to find local IDs from UUIDs for related entities.
     * The DTO must provide UUIDs for category, unit, and supplier via their respective `get...Uuid()` methods.
     *
     * @param dto The complete product data from the remote source.
     */
    public void upsertRemote(ProductDTO dto) {
        // --- BEGIN DTO SANITIZATION BLOCK ---
        if (dto == null || dto.getUuid() == null) {
            LOGGER.log(Level.SEVERE, "Attempted to upsert a null product or a product with a null UUID. Aborting operation.");
            return;
        }

        if (dto.getTenantId() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null tenant_id to empty string for product UUID: {0}", dto.getUuid());
            dto.setTenantId("");
        }
        if (dto.getName() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null name to empty string for product UUID: {0}", dto.getUuid());
            dto.setName("");
        }
        if (dto.getProductType() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null product_type to 'PHYSICAL' for product UUID: {0}", dto.getUuid());
            dto.setProductType("PHYSICAL");
        }
        if (dto.getPurchasePrice() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null purchase_price to 0.0 for product UUID: {0}", dto.getUuid());
            dto.setPurchasePrice(0.0);
        }
        if (dto.getSellingPrice() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null selling_price to 0.0 for product UUID: {0}", dto.getUuid());
            dto.setSellingPrice(0.0);
        }
        if (dto.getTaxRate() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null tax_rate to 0.0 for product UUID: {0}", dto.getUuid());
            dto.setTaxRate(0.0);
        }
        if (dto.getMinStockLevel() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null min_stock_level to 0.0 for product UUID: {0}", dto.getUuid());
            dto.setMinStockLevel(0.0);
        }
        if (dto.getReorderQuantity() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null reorder_quantity to 0.0 for product UUID: {0}", dto.getUuid());
            dto.setReorderQuantity(0.0);
        }
        if (dto.getLastUpdatedAt() == null) {
            LOGGER.log(Level.WARNING, "Sanitizing null last_updated_at to current UTC time for product UUID: {0}", dto.getUuid());
            dto.setLastUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        // --- END DTO SANITIZATION BLOCK ---

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            int idx = 1;
            // INSERT part (18 parameters)
            ps.setString(idx++, dto.getUuid());
            ps.setString(idx++, dto.getTenantId());
            ps.setString(idx++, dto.getSku());
            ps.setString(idx++, dto.getBarcode());
            ps.setString(idx++, dto.getName());
            ps.setString(idx++, dto.getDescription());
            ps.setString(idx++, dto.getProductType());
            ps.setString(idx++, dto.getCategoryUuid());
            ps.setString(idx++, dto.getUnitUuid());
            ps.setString(idx++, dto.getSupplierUuid());
            ps.setDouble(idx++, dto.getPurchasePrice());
            ps.setDouble(idx++, dto.getSellingPrice());
            ps.setDouble(idx++, dto.getTaxRate());
            ps.setDouble(idx++, dto.getMinStockLevel());
            ps.setDouble(idx++, dto.getReorderQuantity());
            ps.setBoolean(idx++, dto.isActive());
            ps.setTimestamp(idx++, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.setBoolean(idx++, dto.isDeleted());

            // ON CONFLICT UPDATE part (3 parameters)
            ps.setString(idx++, dto.getCategoryUuid());
            ps.setString(idx++, dto.getUnitUuid());
            ps.setString(idx, dto.getSupplierUuid());

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote product with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Retrieves all products that are low on stock.
     * @param tenantId The UUID of the tenant.
     * @return A list of ProductDTOs that need reordering.
     */
    public List<ProductDTO> getLowStock(String tenantId) {
        List<ProductDTO> products = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_LOW_STOCK_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                products.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get low stock products for tenant: " + tenantId, e);
        }
        return products;
    }

    /**
     * Retrieves all products that have not been synced. Stock level is not calculated.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced ProductDTOs.
     */
    public List<ProductDTO> getUnsynced(String tenantId) {
        List<ProductDTO> products = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                products.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced products for tenant: " + tenantId, e);
        }
        return products;
    }

    /**
     * Marks a product as inactive.
     * @param productId The ID of the product to deactivate.
     * @param tenantId The UUID of the tenant.
     */
    public void deactivate(long productId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DEACTIVATE_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, productId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to deactivate product: " + productId, e);
        }
    }

    /**
     * Performs a soft delete on a product.
     * @param productId The local ID of the product to delete.
     * @param tenantId The UUID of the tenant.
     */
    public void markAsDeleted(long productId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_DELETED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, productId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark product as deleted: " + productId, e);
        }
    }

    /**
     * Marks a specific product as synced.
     * @param id The local database ID of the product.
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
            LOGGER.log(Level.SEVERE, "Failed to mark product as synced: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link ProductDTO} object.
     * @param rs The ResultSet to map.
     * @return A populated ProductDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private ProductDTO mapToDTO(ResultSet rs) throws SQLException {
        ProductDTO p = new ProductDTO();
        p.setId(rs.getLong("id"));
        p.setUuid(rs.getString("uuid"));
        p.setTenantId(rs.getString("tenant_id"));
        p.setSku(rs.getString("sku"));
        p.setBarcode(rs.getString("barcode"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setProductType(rs.getString("product_type"));
        p.setCategoryId(rs.getObject("category_id", Long.class));
        p.setUnitId(rs.getObject("unit_id", Long.class));
        p.setSupplierId(rs.getObject("supplier_id", Long.class));
        p.setPurchasePrice(rs.getDouble("purchase_price"));
        p.setSellingPrice(rs.getDouble("selling_price"));
        p.setTaxRate(rs.getDouble("tax_rate"));
        p.setMinStockLevel(rs.getDouble("min_stock_level"));
        p.setReorderQuantity(rs.getDouble("reorder_quantity"));
        p.setActive(rs.getBoolean("is_active"));

        Timestamp lastUpdatedAt = rs.getTimestamp("last_updated_at");
        if (lastUpdatedAt != null) {
            p.setLastUpdatedAt(lastUpdatedAt.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }

        p.setIsSynced(rs.getInt("is_synced"));
        p.setDeleted(rs.getBoolean("is_deleted"));
        // This column only exists in the special SELECT_WITH_STOCK_SQL query
        if (hasColumn(rs, "current_stock")) {
            p.setCurrentStock(rs.getDouble("current_stock"));
        }

        return p;
    }

    /**
     * Checks if a column exists in the ResultSet to avoid errors in mapToDTO.
     * @param rs The ResultSet to check.
     * @param columnName The name of the column.
     * @return True if the column exists, false otherwise.
     * @throws SQLException if metadata cannot be accessed.
     */
    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equalsIgnoreCase(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
}