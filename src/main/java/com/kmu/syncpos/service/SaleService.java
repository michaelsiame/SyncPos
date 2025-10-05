package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.SessionContext;
import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.*;
import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class SaleService {

    private static final String TRANSACTION_TYPE = "sale";

    private final SaleDAO saleDAO = new SaleDAO();
    private final SaleItemDAO saleItemDAO = new SaleItemDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final StockLedgerDAO stockLedgerDAO = new StockLedgerDAO();

    /**
     * Processes a new sale transaction. This method manages the database transaction,
     * creating the sale, its items, and the corresponding negative stock adjustments.
     * @param saleDto The main sale information.
     * @param itemDtos The list of items being sold.
     * @param currentUser The user performing the sale.
     * @return The ID of the created sale, or 0 if it failed.
     */
    /**
     * Processes a new sale transaction. This method manages the database transaction,
     * creating the sale, its items, and the corresponding negative stock adjustments.
     * @param saleDto The main sale information.
     * @param itemDtos The list of items being sold.
     * @return The ID of the created sale, or 0 if it failed.
     */
    public long processNewSale(SaleDTO saleDto, List<SaleItemDTO> itemDtos) { // <-- UserDTO parameter removed
        String tenantId = TenantContext.getTenant().getUuid();
        User currentUser = SessionContext.getCurrentUser(); // <-- Get user from session

        saleDto.setUserId(currentUser.getId());
        saleDto.setType("sale");

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                long saleId = saleDAO.insertTransactional(conn, saleDto, tenantId);
                for (SaleItemDTO itemDto : itemDtos) {
                    ProductDTO product = productDAO.getById(itemDto.getProductId(), tenantId);
                    if (product == null) {
                        throw new SQLException("Product with ID " + itemDto.getProductId() + " not found.");
                    }
                    itemDto.setCostAtSale(product.getPurchasePrice());
                    long saleItemId = saleItemDAO.insertTransactional(conn, itemDto, saleId, tenantId);

                    StockLedgerDTO ledgerEntry = new StockLedgerDTO();
                    ledgerEntry.setProductId(itemDto.getProductId());
                    ledgerEntry.setQuantityDelta(-itemDto.getQuantity());
                    ledgerEntry.setReason("sale");
                    ledgerEntry.setSaleItemId(saleItemId);
                    ledgerEntry.setUserId(currentUser.getId());
                    ledgerEntry.setNotes("Sale #" + saleId);
                    stockLedgerDAO.insertTransactional(conn, ledgerEntry, tenantId);
                }
                conn.commit();
                return saleId;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("SaleService.processNewSale failed, rolling back: " + e.getMessage());
                return 0;
            }
        } catch (SQLException e) {
            System.err.println("SaleService: Failed to get or close database connection: " + e.getMessage());
            return 0;
        }
    }
    /**
     * Retrieves a list of all non-deleted sales for the current tenant.
     * @return A list of SaleDTOs of type 'sale'.
     */
    public List<SaleDTO> getAllSales() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return saleDAO.getAllByType(TRANSACTION_TYPE, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("SaleService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single sale and its associated items.
     * @param saleId The ID of the sale to retrieve.
     * @return A SaleDTO populated with its list of SaleItemDTOs, or null if not found.
     */
    public SaleDTO getSaleById(long saleId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            SaleDTO sale = saleDAO.getById(saleId, tenantId);
            if (sale != null && TRANSACTION_TYPE.equals(sale.getType())) {
                List<SaleItemDTO> items = saleItemDAO.getSaleItemsBySaleId(saleId, tenantId);
                sale.setItems(items);
                return sale;
            }
        } catch (IllegalStateException e) {
            System.err.println("SaleService: Tenant context not available. " + e.getMessage());
        }
        return null; // Return null if not found or if it's not a sale
    }

    /**
     * Cancels a sale in a single transaction. This soft-deletes the sale,
     * its items, and the associated stock adjustments.
     * @param saleId The ID of the sale to cancel.
     * @return true if successful, false otherwise.
     */
    public boolean cancelSale(long saleId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    saleDAO.markAsSynced( saleId, tenantId);
                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    System.err.println("SaleService.cancelSale failed, rolling back: " + e.getMessage());
                    return false;
                }
            } catch (SQLException e) {
                System.err.println("SaleService: Database connection error during cancellation. " + e.getMessage());
                return false;
            }
        } catch (IllegalStateException e) {
            System.err.println("SaleService: Tenant context not available. " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all sales (type 'sale') that have not been synced.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced SaleDTOs.
     */
    public List<SaleDTO> getUnsyncedSales(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return saleDAO.getAllByType(TRANSACTION_TYPE, tenantId);
    }

    public List<SaleItemDTO> getUnsyncedSaleItems(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return saleItemDAO.getUnsynced(tenantId);
    }
}