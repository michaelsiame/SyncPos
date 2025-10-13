// Add this helper class to: src/main/java/com/kmu/syncpos/util/UUIDLookupHelper.java
package com.kmu.syncpos.util;

import com.kmu.syncpos.dao.*;
import com.kmu.syncpos.dto.*;

/**
 * Helper utility for looking up UUIDs of related entities.
 * This is needed when converting Models to DTOs for syncing.
 */
public class UUIDLookupHelper {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final UnitDAO unitDAO = new UnitDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final SaleItemDAO saleItemDAO = new SaleItemDAO();

    /**
     * Enriches a ProductDTO with the UUIDs of its related entities.
     */
    public void enrichProductDTO(ProductDTO dto, String tenantId) {
        if (dto.getCategoryId() != null && dto.getCategoryId() > 0) {
            CategoryDTO category = categoryDAO.getById(dto.getCategoryId(), tenantId);
            if (category != null) {
                dto.setCategoryUuid(category.getUuid());
            }
        }

        if (dto.getUnitId() != null && dto.getUnitId() > 0) {
            UnitDTO unit = unitDAO.getById(dto.getUnitId(), tenantId);
            if (unit != null) {
                dto.setUnitUuid(unit.getUuid());
            }
        }

        if (dto.getSupplierId() != null && dto.getSupplierId() > 0) {
            SupplierDTO supplier = supplierDAO.getById(dto.getSupplierId(), tenantId);
            if (supplier != null) {
                dto.setSupplierUuid(supplier.getUuid());
            }
        }
    }

    /**
     * Enriches a SaleDTO with the UUIDs of its related entities.
     */
    public void enrichSaleDTO(SaleDTO dto, String tenantId) {
        if (dto.getCustomerId() != null && dto.getCustomerId() > 0) {
            // Note: CustomerDAO doesn't have getById, would need to add it
            // For now, this shows the pattern
        }

        if (dto.getSupplierId() != null && dto.getSupplierId() > 0) {
            SupplierDTO supplier = supplierDAO.getById(dto.getSupplierId(), tenantId);
            if (supplier != null) {
                dto.setSupplierUuid(supplier.getUuid());
            }
        }
    }

    /**
     * Enriches a SaleItemDTO with the UUIDs of its related entities.
     */
    public void enrichSaleItemDTO(SaleItemDTO dto, String tenantId) {
        // Get sale UUID
        SaleDTO sale = saleDAO.getById(dto.getSaleId(), tenantId);
        if (sale != null) {
            dto.setSaleUuid(sale.getUuid());
        }

        // Get product UUID
        ProductDTO product = productDAO.getById(dto.getProductId(), tenantId);
        if (product != null) {
            dto.setProductUuid(product.getUuid());
        }
    }

    /**
     * Enriches a PaymentDTO with the UUID of its related sale.
     */
    public void enrichPaymentDTO(PaymentDTO dto, String tenantId) {
        SaleDTO sale = saleDAO.getById(dto.getSaleId(), tenantId);
        if (sale != null) {
            dto.setSaleUuid(sale.getUuid());
        }
    }

    /**
     * Enriches a StockLedgerDTO with UUIDs of related entities.
     */
    public void enrichStockLedgerDTO(StockLedgerDTO dto, String tenantId) {
        // Get product UUID
        ProductDTO product = productDAO.getById(dto.getProductId(), tenantId);
        if (product != null) {
            dto.setProductUuid(product.getUuid());
        }

        // Get sale item UUID if applicable
        if (dto.getSaleItemId() != null && dto.getSaleItemId() > 0) {
            // Would need SaleItemDAO.getById() method
            // dto.setSaleItemUuid(saleItem.getUuid());
        }
    }

    /**
     * Enriches a CategoryDTO with the UUID of its parent category.
     */
    public void enrichCategoryDTO(CategoryDTO dto, String tenantId) {
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            CategoryDTO parent = categoryDAO.getById(dto.getParentId(), tenantId);
            if (parent != null) {
                dto.setParentUuid(parent.getUuid());
            }
        }
    }

    /**
     * Enriches a ProductSupplierDTO with UUIDs of related entities.
     */
    public void enrichProductSupplierDTO(ProductSupplierDTO dto, String tenantId) {
        ProductDTO product = productDAO.getById(dto.getProductId(), tenantId);
        if (product != null) {
            dto.setProductUuid(product.getUuid());
        }

        SupplierDTO supplier = supplierDAO.getById(dto.getSupplierId(), tenantId);
        if (supplier != null) {
            dto.setSupplierUuid(supplier.getUuid());
        }
    }
}