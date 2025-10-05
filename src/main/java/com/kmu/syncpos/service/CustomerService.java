// src/main/java/com/kmu/syncpos/service/CustomerService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.CustomerDAO;
import com.kmu.syncpos.dao.SaleDAO;
import com.kmu.syncpos.dao.SaleItemDAO;
import com.kmu.syncpos.dto.CustomerDTO;
import com.kmu.syncpos.dto.PurchaseHistoryDTO;
import com.kmu.syncpos.dto.SaleDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class CustomerService {

    private final CustomerDAO customerDAO;
    // --- DEPENDENCY ADDED ---
    private final SaleDAO saleDAO;
    private final SaleItemDAO saleItemDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
        // --- DEPENDENCY INITIALIZED ---
        this.saleDAO = new SaleDAO();
        this.saleItemDAO = new SaleItemDAO();
    }

    /**
     * Gets all non-deleted customers for the currently logged-in user's tenant.
     * This is where we interact with the TenantContext.
     * @return A list of CustomerDTOs.
     */
    public List<CustomerDTO> getAllActiveCustomers() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return customerDAO.getAll(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("CustomerService: Cannot get customers, tenant context not available.");
            return Collections.emptyList();
        }
    }

    /**
     * Saves a customer. Handles both creating new customers and updating existing ones.
     * @param dto The customer data to save.
     */
    public void saveCustomer(CustomerDTO dto) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            if (dto.getId() == 0) {
                customerDAO.createLocal(dto, tenantId);
            } else {
                customerDAO.updateLocal(dto, tenantId);
            }
        } catch (IllegalStateException e) {
            System.err.println("CustomerService: Cannot save customer, tenant context not available.");
        }
    }

    /**
     * Deletes a customer by marking them as deleted in the database.
     * @param customerId The ID of the customer to delete.
     */
    public void deleteCustomer(long customerId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            customerDAO.markAsDeleted(customerId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("CustomerService: Cannot delete customer, tenant context not available.");
        }
    }

    public List<CustomerDTO> getUnsyncedCustomers(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return customerDAO.getUnsynced(tenantId);
    }

    /**
     * Retrieves the purchase history for a given customer from the database.
     * @param customerId The ID of the customer.
     * @return A list of purchase history records.
     */
    public List<PurchaseHistoryDTO> getPurchaseHistoryForCustomer(long customerId) {
        if (customerId <= 0) {
            return Collections.emptyList();
        }

        try {
            String tenantId = TenantContext.getTenant().getUuid();

            // Single, efficient call to the DAO
            return saleDAO.getPurchaseHistoryForCustomer(customerId, tenantId);

        } catch (IllegalStateException e) {
            System.err.println("CustomerService: Cannot get purchase history, tenant context not available.");
            return Collections.emptyList();
        }
    }
}