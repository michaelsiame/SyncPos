package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.SessionContext;
import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.StockLedgerDAO;
import com.kmu.syncpos.dto.StockLedgerDTO;
import com.kmu.syncpos.dto.UserDTO;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Service layer for managing stock ledger (inventory adjustment) operations.
 * This class encapsulates the business logic for creating and reversing adjustments
 * and retrieving stock history.
 */
public class StockLedgerService {

    private final StockLedgerDAO stockLedgerDAO = new StockLedgerDAO();

    /**
     * Retrieves the complete inventory adjustment history for a specific product.
     * @param productId The ID of the product to query.
     * @return A list of StockLedgerDTOs, ordered from newest to oldest.
     */
    public List<StockLedgerDTO> getEntriesForProduct(long productId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return stockLedgerDAO.getEntriesForProduct(productId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("StockLedgerService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Creates a new manual inventory adjustment (e.g., for stock take, damage, or other reasons).
     * This method manages its own database transaction.
     * @param dto A DTO containing the product ID, quantity change, reason, and notes.
     * @return true if the adjustment was successfully created, false otherwise.
     */
    public boolean addManualAdjustment(StockLedgerDTO dto) {
        String tenantId;
        User currentUser;
        try {
            tenantId = TenantContext.getTenant().getUuid();
            currentUser = SessionContext.getCurrentUser();
        } catch (IllegalStateException e) {
            System.err.println("StockLedgerService: Cannot add adjustment, context not available. " + e.getMessage());
            return false;
        }

        dto.setUserId(currentUser.getId());

        // Refined: Use try-with-resources for safer connection management
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                stockLedgerDAO.insertTransactional(conn, dto, tenantId);
                conn.commit();
                System.out.println("Manual adjustment for product " + dto.getProductId() + " created successfully.");
                return true;
            } catch (SQLException e) {
                System.err.println("StockLedgerService.addManualAdjustment failed, rolling back: " + e.getMessage());
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("StockLedgerService: Database connection error during manual adjustment. " + e.getMessage());
            return false;
        }
    }

    /**
     * Cancels a previous stock adjustment by creating a new, opposing entry.
     * This preserves the audit trail.
     * @param adjustmentId The ID of the stock ledger entry to cancel.
     * @return true if the cancellation was successful, false otherwise.
     */
    public boolean cancelAdjustment(long adjustmentId) {
        String tenantId;
        User currentUser;
        try {
            tenantId = TenantContext.getTenant().getUuid();
            currentUser = SessionContext.getCurrentUser();
        } catch (IllegalStateException e) {
            System.err.println("StockLedgerService: Cannot cancel adjustment, context not available. " + e.getMessage());
            return false;
        }

        // 1. Find the original adjustment
        StockLedgerDTO original = stockLedgerDAO.getById(adjustmentId, tenantId);
        if (original == null) {
            System.err.println("Cannot cancel adjustment: Original entry with ID " + adjustmentId + " not found.");
            return false;
        }

        // 2. Create the reversing entry
        StockLedgerDTO reversal = new StockLedgerDTO();
        reversal.setProductId(original.getProductId());
        reversal.setQuantityDelta(-original.getQuantityDelta()); // The core reversal logic
        reversal.setReason("Reversal");
        reversal.setUserId(currentUser.getId());
        reversal.setNotes("Reversal of adjustment #" + original.getId());

        // 3. Insert the new entry in a transaction
        return addManualAdjustment(reversal); // We can just reuse the existing transactional method
    }

    /**
     * Retrieves all unsynced stock ledger entries for a given tenant.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced StockLedgerDTOs.
     */
    public List<StockLedgerDTO> getUnsyncedStockLedger(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return stockLedgerDAO.getUnsynced(tenantId);
    }
}