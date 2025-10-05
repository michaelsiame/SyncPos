// src/main/java/com/kmu/syncpos/dao/ReportDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.models.reports.ProductPerformanceRecord;
import com.kmu.syncpos.models.reports.SalesReportRecord;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public List<SalesReportRecord> getSalesDetailReport(LocalDate start, LocalDate end, String tenantId) {
        List<SalesReportRecord> list = new ArrayList<>();
        String sql = """
            SELECT s.id, s.created_at, COALESCE(c.name,'Walk-in Customer') AS customer_name,
                   u.username AS cashier_name, s.total, s.tenant_id
            FROM sales s
            LEFT JOIN customers c ON c.id = s.customer_id AND c.tenant_id = s.tenant_id
            JOIN users u ON u.id = s.user_id AND u.tenant_id = s.tenant_id
            WHERE s.type='sale' AND s.is_deleted=0 AND DATE(s.created_at) BETWEEN ? AND ? AND s.tenant_id = ?
            ORDER BY s.created_at DESC
            """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            ps.setString(3, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new SalesReportRecord(
                        rs.getInt("id"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("customer_name"),
                        rs.getString("cashier_name"),
                        rs.getDouble("total"),
                        rs.getString("tenant_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("ReportDAO.getSalesDetailReport: " + e.getMessage());
        }
        return list;
    }

    public List<ProductPerformanceRecord> getProductPerformanceReport(LocalDate start, LocalDate end, String tenantId) {
        List<ProductPerformanceRecord> list = new ArrayList<>();
        String sql = """
            SELECT p.name, p.sku, SUM(si.quantity) AS total_quantity, SUM(si.total) AS total_revenue, s.tenant_id
            FROM sale_items si
            JOIN products p ON p.id = si.product_id AND p.tenant_id = si.tenant_id
            JOIN sales s ON s.id = si.sale_id AND s.tenant_id = si.tenant_id
            WHERE s.type='sale' AND s.is_deleted=0 AND si.is_deleted=0 AND DATE(s.created_at) BETWEEN ? AND ? AND s.tenant_id = ?
            GROUP BY p.id, p.name, p.sku
            ORDER BY total_revenue DESC
            """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            ps.setString(3, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new ProductPerformanceRecord(
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getDouble("total_quantity"),
                        rs.getDouble("total_revenue"),
                        rs.getString("tenant_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("ReportDAO.getProductPerformanceReport: " + e.getMessage());
        }
        return list;
    }
}