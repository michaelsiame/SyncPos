// src/main/java/com/kmu/syncpos/service/DashboardService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.DashboardDAO;
import com.kmu.syncpos.dto.ProductDTO;
import com.kmu.syncpos.dto.dashboard.DashboardKPIs;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DashboardService {

    // The service layer should depend on a new, focused DAO
    private final DashboardDAO dashboardDAO = new DashboardDAO();

    /**
     * Retrieves the key performance indicators (KPIs) for today's activity.
     * @return A DTO containing today's sales total and transaction count.
     */
    public DashboardKPIs getTodaysKPIs() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return dashboardDAO.getTodaysKPIs(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("DashboardService: Tenant context not available. " + e.getMessage());
            return new DashboardKPIs(0.0, 0);
        }
    }

    /**
     * Retrieves a list of products that are at or below their minimum stock level.
     * @return A list of low-stock ProductDTOs.
     */
    public List<ProductDTO> getLowStockProducts() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return dashboardDAO.getLowStockProducts(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("DashboardService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves the total sales for each of the last 7 days.
     * @return A map where the key is the date and the value is the total sales for that day.
     */
    public Map<LocalDate, Double> getSalesForLast7Days() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return dashboardDAO.getSalesForLast7Days(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("DashboardService: Tenant context not available. " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}