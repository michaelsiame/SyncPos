// src/main/java/com/kmu/syncpos/service/SyncService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.dao.*;
import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.Tenant;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;
import java.util.function.Consumer;

/**
 * A background service that supports both initial full download (pull) and
 * incremental upload (push) of unsynced local data.
 */
public class SyncService extends Service<Void> {

    // --- Service & DAO Dependencies ---
    private final ApiService apiService = new ApiService();
    // private final ProductSupplierService productSupplierService = new ProductSupplierService(); // This seems unused, can be removed if so.
    private final UserDAO userDAO = new UserDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final UnitDAO unitDAO = new UnitDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ProductSupplierDAO productSupplierDAO = new ProductSupplierDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final SaleItemDAO saleItemDAO = new SaleItemDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final StockLedgerDAO stockLedgerDAO = new StockLedgerDAO();

    private final Tenant tenant;
    private final boolean isInitialSync;

    public SyncService(Tenant tenant, boolean isInitialSync) {
        if (tenant == null || tenant.getUuid() == null || tenant.getUuid().isEmpty()) {
            throw new IllegalArgumentException("SyncService cannot be started without a valid Tenant.");
        }
        this.tenant = tenant;
        this.isInitialSync = isInitialSync;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (isInitialSync) {
                    updateMessage("Starting initial data download…");
                    initialSync();
                } else {
                    updateMessage("Checking for local changes to upload…");
                    pushUnsyncedChanges();
                }
                updateMessage("Sync cycle finished.");
                return null;
            }

            @Override
            protected void failed() {
                super.failed();
                // It's critical to log the exception to see the stack trace
                getException().printStackTrace();
                updateMessage("Sync failed: " + getException().getMessage());
            }

            // =========================================================
            //  SYNCHRONIZATION LOGIC
            // =========================================================

            /**
             * Performs a full data download from the remote server to the local database.
             * The order of operations is CRITICAL to respect foreign key constraints.
             * We must insert parent records before their children.
             */
            private void initialSync() {
                String tenantId = tenant.getUuid();

                // --- Phase 1: Independent Core Data ---
                // These tables have few or no dependencies on other tables.
                updateMessage("Downloading Users…");
                pullAll(apiService.getAllUsers(tenantId), userDAO::upsertRemote);
                updateMessage("Downloading Categories…");
                pullAll(apiService.getAllCategories(tenantId), categoryDAO::upsertRemote);
                updateMessage("Downloading Units…");
                pullAll(apiService.getAllUnits(tenantId), unitDAO::upsertRemote);
                updateMessage("Downloading Suppliers…");
                pullAll(apiService.getAllSuppliers(tenantId), supplierDAO::upsertRemote);
                updateMessage("Downloading Customers…");
                pullAll(apiService.getAllCustomers(tenantId), customerDAO::upsertRemote);
                updateMessage("Downloading Settings…");
                pullAll(apiService.getAllSettings(tenantId), settingsDAO::upsertRemote);

                // --- Phase 2: Core Data with Dependencies ---
                // Products depend on Categories, Units, and Suppliers.
                updateMessage("Downloading Products…");
                pullAll(apiService.getAllProducts(tenantId), productDAO::upsertRemote);
                // ProductSuppliers depends on Products and Suppliers.
                updateMessage("Downloading Product-Supplier Links…");
                pullAll(apiService.getAllProductSuppliers(tenantId), productSupplierDAO::upsertRemote);

                // --- Phase 3: Parent Transactional Data ---
                // Sales are the parents for SaleItems and Payments.
                updateMessage("Downloading Sales…");
                pullAll(apiService.getAllSales(tenantId), saleDAO::upsertRemote);

                // --- Phase 4: Dependent Child Data ---
                // These MUST be run after their parent records (Sales) are saved locally.
                updateMessage("Downloading Sale Items…");
                pullAll(apiService.getAllSaleItems(tenantId), saleItemDAO::upsertRemote);
                updateMessage("Downloading Payments…");
                pullAll(apiService.getAllPayments(tenantId), paymentDAO::upsertRemote);

                // --- Phase 5: Deeply Dependent Data ---
                // StockLedger depends on Products, Users, and sometimes SaleItems.
                updateMessage("Downloading Stock Ledger…");
                pullAll(apiService.getAllStockLedgerEntries(tenantId), stockLedgerDAO::upsertRemote);
            }

            /**
             * Pushes unsynced local data to the remote server.
             * The order is just as critical as the pull to ensure parent records
             * are created on the remote server before their children arrive.
             */
            private void pushUnsyncedChanges() {
                String tenantId = tenant.getUuid();

                // --- Phase 1: Independent Core Data ---
                updateMessage("Syncing Settings...");
                pushUnsynced(settingsDAO.getUnsyncedSettings(tenantId), apiService::postSettings, settingsDAO::markAsSynced, tenantId);
                updateMessage("Syncing Users...");
                pushUnsynced(userDAO.getUnsynced(tenantId), apiService::postUser, userDAO::markAsSynced, tenantId);
                updateMessage("Syncing Suppliers...");
                pushUnsynced(supplierDAO.getUnsynced(tenantId), apiService::postSupplier, supplierDAO::markAsSynced, tenantId);
                updateMessage("Syncing Customers...");
                pushUnsynced(customerDAO.getUnsynced(tenantId), apiService::postCustomer, customerDAO::markAsSynced, tenantId);
                updateMessage("Syncing Categories...");
                pushUnsynced(categoryDAO.getUnsynced(tenantId), apiService::postCategory, categoryDAO::markAsSynced, tenantId);
                updateMessage("Syncing Units...");
                pushUnsynced(unitDAO.getUnsynced(tenantId), apiService::postUnit, unitDAO::markAsSynced, tenantId);

                // --- Phase 2: Core Data with Dependencies ---
                updateMessage("Syncing Products...");
                pushUnsynced(productDAO.getUnsynced(tenantId), apiService::postProduct, productDAO::markAsSynced, tenantId);
                updateMessage("Syncing Product-Supplier Links...");
                pushUnsynced(productSupplierDAO.getUnsynced(tenantId), apiService::postProductSupplier, productSupplierDAO::markAsSynced, tenantId);

                // --- Phase 3: Parent Transactional Data ---
                updateMessage("Syncing Sales & Purchases...");
                pushUnsynced(saleDAO.getUnsynced(tenantId), apiService::postSale, saleDAO::markAsSynced, tenantId);

                // --- Phase 4: Dependent Child Data ---
                updateMessage("Syncing Sale & Purchase Items...");
                pushUnsynced(saleItemDAO.getUnsynced(tenantId), apiService::postSaleItem, saleItemDAO::markAsSynced, tenantId);
                updateMessage("Syncing Payments...");
                pushUnsynced(paymentDAO.getUnsynced(tenantId), apiService::postPayment, paymentDAO::markAsSynced, tenantId);

                // --- Phase 5: Deeply Dependent Data ---
                updateMessage("Syncing Inventory Adjustments...");
                pushUnsynced(stockLedgerDAO.getUnsynced(tenantId), apiService::postStockLedger, stockLedgerDAO::markAsSynced, tenantId);
            }


            // =========================================================
            //  HELPER METHODS
            // =========================================================

            private <T> void pullAll(List<T> dtoList, Consumer<T> upsertFunction) {
                if (dtoList == null || dtoList.isEmpty()) return;
                for (T dto : dtoList) {
                    // Consider adding a try-catch block here to handle individual record failures
                    // without stopping the entire sync process.
                    upsertFunction.accept(dto);
                }
            }

            @FunctionalInterface interface PostFunction<T> { boolean apply(T dto); }
            @FunctionalInterface interface MarkSyncedFunction { void apply(long id, String tenantId); }
            private <T extends BaseDTO> void pushUnsynced(List<T> dtoList, PostFunction<T> postFunc, MarkSyncedFunction markSyncedFunc, String tenantId) {
                if (dtoList == null || dtoList.isEmpty()) return; // Added null check for safety
                String typeName = dtoList.get(0).getClass().getSimpleName().replace("DTO", "");
                int total = dtoList.size();
                int current = 0;
                for (T dto : dtoList) {
                    current++;
                    updateMessage(String.format("Uploading %s (%d/%d)...", typeName, current, total));
                    // Consider adding a try-catch block here as well.
                    if (postFunc.apply(dto)) {
                        markSyncedFunc.apply(dto.getId(), tenantId);
                    }
                }
            }
        };
    }
}