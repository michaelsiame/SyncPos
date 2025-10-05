// src/main/java/com/kmu/syncpos/service/ReportService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.ReportDAO;
import com.kmu.syncpos.dto.ProductDTO;
import com.kmu.syncpos.models.reports.ProductPerformanceRecord;
import com.kmu.syncpos.models.reports.SalesReportRecord;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();
    private final ProductService productService = new ProductService();

    public List<SalesReportRecord> getSalesDetailReport(LocalDate start, LocalDate end) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return reportDAO.getSalesDetailReport(start, end, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ReportService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<ProductPerformanceRecord> getProductPerformanceReport(LocalDate start, LocalDate end) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return reportDAO.getProductPerformanceReport(start, end, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("ReportService: Tenant context not available. " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<ProductDTO> getInventoryValueReport() {
        // This can just reuse the existing ProductService method.
        return productService.getAllActiveProducts();
    }
}