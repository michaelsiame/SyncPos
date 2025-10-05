// src/main/java/com/kmu/syncpos/service/SupplierService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.SupplierDAO;
import com.kmu.syncpos.dto.SupplierDTO;

import java.util.Collections;
import java.util.List;

public class SupplierService {

    private final SupplierDAO supplierDAO;

    public SupplierService() {
        this.supplierDAO = new SupplierDAO();
    }

    /**
     * Retrieves all non-deleted suppliers for the current tenant.
     * @return A list of active SupplierDTOs.
     */
    public List<SupplierDTO> getAllActiveSuppliers() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return supplierDAO.getAll(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("SupplierService: Tenant context not available.");
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single supplier by its ID.
     * @param supplierId The ID of the supplier to retrieve.
     * @return A SupplierDTO if found, otherwise null.
     */
    public SupplierDTO getSupplierById(long supplierId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return supplierDAO.getById(supplierId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("SupplierService: Tenant context not available.");
            return null;
        }
    }

    /**
     * Saves a supplier, handling both creation of new suppliers and updates to existing ones.
     * @param dto The supplier data to save.
     */

    public void saveSupplier(SupplierDTO dto) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            if (dto.getId() == 0) {
                supplierDAO.createLocal(dto, tenantId);
            } else {
                supplierDAO.updateLocal(dto, tenantId);
            }

        } catch (IllegalStateException e) {
            System.err.println("SupplierService: Cannot save supplier, tenant context not available.");
        }
    }

    /**
     * Soft-deletes a supplier.
     * @param supplierId The ID of the supplier to delete.
     */
    public void deleteSupplier(long supplierId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            supplierDAO.markAsDeleted(supplierId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("SupplierService: Tenant context not available.");
        }
    }

    public List<SupplierDTO> getUnsyncedSuppliers(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return supplierDAO.getUnsynced(tenantId);
    }
}