// src/main/java/com/kmu/syncpos/dao/DashboardDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.ProductDTO;
import com.kmu.syncpos.dto.dashboard.DashboardKPIs;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardDAO {

    public DashboardKPIs getTodaysKPIs(String tenantId) {
        String sql = """
            SELECT
                COALESCE(SUM(CASE WHEN type='sale' THEN total ELSE 0 END), 0) as sales,
                COALESCE(SUM(CASE WHEN type='sale' THEN 1 ELSE 0 END), 0) as transactions
            FROM sales
            WHERE is_deleted = 0 AND DATE(created_at) = DATE('now','localtime') AND tenant_id = ?
            """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new DashboardKPIs(rs.getDouble("sales"), rs.getInt("transactions"));
            }
        } catch (SQLException e) {
            System.err.println("DashboardDAO.getTodaysKPIs: " + e.getMessage());
        }
        return new DashboardKPIs(0, 0);
    }

    public List<ProductDTO> getLowStockProducts(String tenantId) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = """
            SELECT p.id, p.name, p.min_stock_level,
                   COALESCE(SUM(ia.quantity_delta),0) AS current_stock
            FROM products p
            LEFT JOIN stock_ledger ia ON ia.product_id = p.id AND ia.tenant_id = p.tenant_id AND ia.is_deleted = 0
            WHERE p.tenant_id = ? AND p.is_deleted = 0 AND p.is_active = 1
            GROUP BY p.id
            HAVING current_stock <= p.min_stock_level
            ORDER BY p.name
            """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ProductDTO dto = new ProductDTO();
                dto.setId(rs.getLong("id"));
                dto.setName(rs.getString("name"));
                dto.setMinStockLevel(rs.getDouble("min_stock_level"));
                dto.setCurrentStock(rs.getDouble("current_stock"));
                list.add(dto);
            }
        } catch (SQLException e) {
            System.err.println("DashboardDAO.getLowStockProducts: " + e.getMessage());
        }
        return list;
    }

    public Map<LocalDate, Double> getSalesForLast7Days(String tenantId) {
        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) map.put(LocalDate.now().minusDays(i), 0.0);
        String sql = """
            SELECT DATE(created_at) AS d, COALESCE(SUM(total),0) AS total
            FROM sales
            WHERE type='sale' AND is_deleted=0 AND DATE(created_at)>=DATE('now','-6 days','localtime')
              AND tenant_id = ?
            GROUP BY d ORDER BY d
            """;
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) map.put(LocalDate.parse(rs.getString("d")), rs.getDouble("total"));
        } catch (SQLException e) {
            System.err.println("DashboardDAO.getSalesForLast7Days: " + e.getMessage());
        }
        return map;
    }
}