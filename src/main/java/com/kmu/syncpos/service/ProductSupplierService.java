// src/main/java/com/kmu/syncpos/service/ProductSupplierService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.ProductSupplierDAO;
import com.kmu.syncpos.dto.ProductSupplierDTO;

import java.util.Collections;
import java.util.List;

/**
 * Service layer for managing the relationships between products and suppliers.
 */
public class ProductSupplierService {

    private final ProductSupplierDAO productSupplierDAO = new ProductSupplierDAO();

    /**
     * Retrieves all supplier links for a given product.
     * @param productId The ID of the product.
     * @return A list of ProductSupplierDTOs.
     */
    public List<ProductSupplierDTO> getSuppliersForProduct(long productId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return productSupplierDAO.getAllByProductId(productId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ProductSupplierService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Saves a product-supplier link. Handles both creation of new links and
     * updates to existing ones (e.g., changing the supplier's product code).
     * @param dto The data for the link to be saved.
     */
    public void saveProductSupplierLink(ProductSupplierDTO dto) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            if (dto.getId() == 0) {
                // Check if a link already exists to prevent duplicates.
                ProductSupplierDTO existing = productSupplierDAO.findByProductAndSupplier(dto.getProductId(), dto.getSupplierId(), tenantId);
                if (existing != null) {
                    // If it exists but was soft-deleted, we can handle re-activation or just update it.
                    // For simplicity, we'll just update the existing one.
                    dto.setId(existing.getId());
                    productSupplierDAO.updateLocal(dto);
                } else {
                    productSupplierDAO.createLocal(dto, tenantId);
                }
            } else {
                productSupplierDAO.updateLocal(dto);
            }
        } catch (IllegalStateException e) {
            System.err.println("ProductSupplierService: Tenant context not available. " + e.getMessage());
        }
    }

    /**
     * Deletes a link between a product and a supplier.
     * @param productSupplierId The ID of the product_suppliers table entry to delete.
     */
    public void deleteProductSupplierLink(long productSupplierId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            productSupplierDAO.markAsDeleted(productSupplierId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ProductSupplierService: Tenant context not available. " + e.getMessage());
        }
    }

    /**
     * Retrieves all unsynced product-supplier links for a given tenant.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced DTOs.
     */
    public List<ProductSupplierDTO> getUnsyncedProductSuppliers(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return productSupplierDAO.getUnsynced(tenantId);
    }
}