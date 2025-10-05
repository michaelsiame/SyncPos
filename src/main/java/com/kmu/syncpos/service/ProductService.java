// src/main/java/com/kmu/syncpos/service/ProductService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.SessionContext; // <-- IMPORT ADDED
import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.ProductDAO;
import com.kmu.syncpos.dao.StockLedgerDAO;
import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.User; // <-- IMPORT ADDED
import com.kmu.syncpos.service.form.ProductFormDependencies;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ProductService {

    private final ProductDAO productDAO;
    private final CategoryService categoryService;
    private final UnitService unitService;
    private final SupplierService supplierService;
    private final StockLedgerDAO stockLedgerDAO;


    public ProductService() {
        this.productDAO = new ProductDAO();
        this.categoryService = new CategoryService();
        this.unitService = new UnitService();
        this.supplierService = new SupplierService();
        this.stockLedgerDAO = new StockLedgerDAO();
    }

    public List<ProductDTO> getAllActiveProducts() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return productDAO.getAll(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ProductService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public ProductDTO getProductById(long productId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return productDAO.getById(productId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ProductService: Tenant context not available. " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves a product. Handles both creation (with initial stock) and updates.
     * When creating a new product, if dto.getCurrentStock() > 0, it will also create
     * an initial stock ledger entry in the same transaction.
     * @param dto The product data to save.
     * @return true if successful, false otherwise.
     */
    public boolean saveProduct(ProductDTO dto) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            // --- CHANGE 2: HANDLE INITIAL STOCK TRANSACTIONALLY ---
            if (dto.getId() == 0) { // This is a new product
                try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        // The DAO's add method must return the new ID
                        long newProductId = productDAO.createLocalTransactional(conn, dto, tenantId);

                        // If an initial stock quantity was provided, create the first ledger entry
                        if (dto.getCurrentStock() > 0) {
                            User currentUser = SessionContext.getCurrentUser();
                            StockLedgerDTO initialStock = new StockLedgerDTO();
                            initialStock.setProductId(newProductId);
                            initialStock.setQuantityDelta(dto.getCurrentStock());
                            initialStock.setReason("Initial Stock");
                            initialStock.setUserId(currentUser.getId());
                            initialStock.setNotes("Initial stock on product creation.");
                            stockLedgerDAO.insertTransactional(conn, initialStock, tenantId);
                        }
                        conn.commit();
                        return true;
                    } catch (SQLException e) {
                        conn.rollback();
                        System.err.println("ProductService: Failed to save new product, rolling back. " + e.getMessage());
                        return false;
                    }
                } catch (SQLException e) {
                    System.err.println("ProductService: Database connection error during product save. " + e.getMessage());
                    return false;
                }
            } else { // This is an existing product update
                productDAO.updateLocal(dto, tenantId);
                return true;
            }
        } catch (IllegalStateException e) {
            System.err.println("ProductService: Cannot save product, tenant context not available. " + e.getMessage());
            return false;
        }
    }

    public void deactivateProduct(long productId) {
        // ... (no changes needed)
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            productDAO.deactivate(productId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ProductService: Cannot deactivate product, tenant context not available. " + e.getMessage());
        }
    }

    public void deleteProduct(long productId) {
        // ... (no changes needed)
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            productDAO.markAsDeleted(productId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ProductService: Cannot delete product, tenant context not available. " + e.getMessage());
        }
    }

    public List<ProductDTO> getLowStockProducts() {
        // ... (no changes needed)
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return productDAO.getLowStock(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ProductService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Manually adjusts the stock for a product and creates a corresponding ledger entry.
     * This is a transactional operation.
     * @param productId The ID of the product to adjust.
     * @param quantityChange The change in quantity (can be positive or negative).
     * @param reason A predefined reason for the adjustment (e.g., "Stock Take", "Damaged Goods").
     * @param notes Optional notes about the adjustment.
     * @return true if the adjustment was successful, false otherwise.
     */
    public boolean adjustProductStock(long productId, double quantityChange, String reason, String notes) {
        // --- CHANGE 1: REMOVED USERID PARAMETER ---
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            User currentUser = SessionContext.getCurrentUser(); // Get user from session

            StockLedgerDTO adjustment = new StockLedgerDTO();
            adjustment.setProductId(productId);
            adjustment.setQuantityDelta(quantityChange);
            adjustment.setReason(reason);
            adjustment.setUserId(currentUser.getId()); // Use the session user's ID
            adjustment.setNotes(notes);

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    stockLedgerDAO.insertTransactional(conn, adjustment, tenantId);
                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    System.err.println("ProductService: Failed to insert stock ledger entry, rolling back. " + e.getMessage());
                    return false;
                }
            } catch (SQLException e) {
                System.err.println("ProductService: Database connection error during stock adjustment. " + e.getMessage());
                return false;
            }
        } catch (IllegalStateException e) {
            System.err.println("ProductService: Tenant context not available. " + e.getMessage());
            return false;
        }
    }

    public ProductFormDependencies getFormDependencies() {
        // ... (no changes needed)
        List<CategoryDTO> categories = categoryService.getAllActiveCategories();
        List<UnitDTO> units = unitService.getAllActiveUnits();
        List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
        return new ProductFormDependencies(categories, units, suppliers);
    }

    public List<ProductDTO> getUnsyncedProducts(String tenantId) {
        // ... (no changes needed)
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return productDAO.getUnsynced(tenantId);
    }
}