package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.SessionContext;
import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.ProductSupplierDAO;
import com.kmu.syncpos.dao.SaleDAO;
import com.kmu.syncpos.dao.SaleItemDAO;
import com.kmu.syncpos.dao.StockLedgerDAO;
import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class PurchaseService {

    private static final String TRANSACTION_TYPE = "purchase";

    private final SaleDAO saleDAO = new SaleDAO();
    private final SaleItemDAO saleItemDAO = new SaleItemDAO();
    private final StockLedgerDAO stockLedgerDAO = new StockLedgerDAO();
    private final ProductSupplierDAO productSupplierDAO = new ProductSupplierDAO();

    /**
     * Processes a new purchase transaction, creating the purchase record, its items,
     * and the corresponding positive stock adjustments. It now also records the
     * supplier-specific product code for each item.
     * @param purchaseDto The main purchase information.
     * @param itemDtos The list of items being purchased.
     * @return True if successful, false otherwise.
     */
    public boolean processNewPurchase(SaleDTO purchaseDto, List<SaleItemDTO> itemDtos) {
        String tenantId = TenantContext.getTenant().getUuid();
        User currentUser = SessionContext.getCurrentUser();

        purchaseDto.setUserId(currentUser.getId());
        purchaseDto.setType("purchase");

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                long purchaseId = saleDAO.insertTransactional(conn, purchaseDto, tenantId);

                for (SaleItemDTO itemDto : itemDtos) {
                    itemDto.setCostAtSale(itemDto.getUnitPrice());

                    // --- CHANGE 2: LOOK UP AND SET THE SUPPLIER-SPECIFIC CODE ---
                    ProductSupplierDTO psLink = productSupplierDAO.findByProductAndSupplier(
                            itemDto.getProductId(),
                            purchaseDto.getSupplierId(), // The supplier is on the main purchase DTO
                            tenantId
                    );

                    if (psLink != null) {
                        itemDto.setSupplierProductCode(psLink.getSupplierProductCode());
                    }
                    // If no link is found, the code will remain null, which is acceptable.

                    long purchaseItemId = saleItemDAO.insertTransactional(conn, itemDto, purchaseId, tenantId);

                    StockLedgerDTO ledgerEntry = new StockLedgerDTO();
                    ledgerEntry.setProductId(itemDto.getProductId());
                    ledgerEntry.setQuantityDelta(itemDto.getQuantity());
                    ledgerEntry.setReason("purchase");
                    ledgerEntry.setSaleItemId(purchaseItemId);
                    ledgerEntry.setUserId(currentUser.getId());
                    ledgerEntry.setNotes("Purchase #" + purchaseId);
                    stockLedgerDAO.insertTransactional(conn, ledgerEntry, tenantId);
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                System.err.println("PurchaseService.processNewPurchase failed, rolling back: " + e.getMessage());
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("PurchaseService: Failed to get or close database connection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a list of all non-deleted purchases for the current tenant.
     * @return A list of SaleDTOs of type 'purchase'.
     */
    public List<SaleDTO> getAllPurchases() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return saleDAO.getAllByType(TRANSACTION_TYPE, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("PurchaseService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single purchase and its associated items.
     * @param purchaseId The ID of the purchase to retrieve.
     * @return A SaleDTO populated with its list of SaleItemDTOs, or null if not found.
     */
    public SaleDTO getPurchaseById(long purchaseId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            SaleDTO purchase = saleDAO.getById(purchaseId, tenantId);
            if (purchase != null && TRANSACTION_TYPE.equals(purchase.getType())) {
                List<SaleItemDTO> items = saleItemDAO.getSaleItemsBySaleId(purchaseId, tenantId);
                purchase.setItems(items);
                return purchase;
            }
        } catch (IllegalStateException e) {
            System.err.println("PurchaseService: Tenant context not available. " + e.getMessage());
        }
        return null; // Return null if not found or if it's not a purchase
    }

    /**
     * Cancels a purchase in a single transaction. This soft-deletes the purchase,
     * its items, and the associated stock adjustments.
     * @param purchaseId The ID of the purchase to cancel.
     * @return true if successful, false otherwise.
     */
    public boolean cancelPurchase(long purchaseId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // The existing SaleDAO.delete is already transactional and handles all related tables.
                    saleDAO.markAsSynced( purchaseId, tenantId);
                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    System.err.println("PurchaseService.cancelPurchase failed, rolling back: " + e.getMessage());
                    return false;
                }
            } catch (SQLException e) {
                System.err.println("PurchaseService: Database connection error during cancellation. " + e.getMessage());
                return false;
            }
        } catch (IllegalStateException e) {
            System.err.println("PurchaseService: Tenant context not available. " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all purchases that have not been synced to a central server.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced SaleDTOs of type 'purchase'.
     */
    public List<SaleDTO> getUnsyncedPurchases(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return saleDAO.getUnsynced( tenantId);
    }
}