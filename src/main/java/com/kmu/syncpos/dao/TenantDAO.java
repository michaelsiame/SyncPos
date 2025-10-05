package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.TenantDTO;
import com.kmu.syncpos.models.Tenant;
import com.kmu.syncpos.util.DatabaseManager;
import com.kmu.syncpos.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class TenantDAO {

    public Tenant findActiveTenant() {
        // Assuming there should only be one active tenant
        String sql = "SELECT uuid, license_key, status FROM tenants WHERE status = 'ACTIVE' LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Tenant tenant = new Tenant();
                tenant.setUuid(rs.getString("uuid"));
                tenant.setLicenseKey(rs.getString("license_key"));
                tenant.setStatus(rs.getString("status"));
                return tenant;
            }
        } catch (SQLException e) {
            System.err.println("Error finding active tenant: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // No active tenant found
    }
    /**
     * Inserts or updates a tenant record in the local database.
     * This is primarily used after fetching tenant info from the server during activation.
     * It uses the UUID as the unique key for conflict resolution.
     * @param dto The TenantDTO containing the data to save.
     */
    public void saveTenant(TenantDTO dto) {
        String sql = """
        INSERT INTO tenants (uuid, license_key, status, is_synced)
        VALUES (?, ?, ?, 1)
        ON CONFLICT(uuid) DO UPDATE SET
            license_key = excluded.license_key,
            status = excluded.status,
            is_synced = 1;
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dto.getUuid());
            ps.setString(2, dto.getLicenseKey());
            ps.setString(3, dto.getStatus());

            ps.executeUpdate();
            System.out.println("Tenant record saved locally for UUID: " + dto.getUuid());

        } catch (SQLException e) {
            System.err.println("TenantDAO.saveTenant failed: " + e.getMessage());
            // In a real app, you might want to throw a custom exception here
        }
    }


// Add this method to your TenantDAO.java file

    /**
     * Retrieves a single tenant by its UUID.
     * @param tenantUuid The UUID of the tenant.
     * @return A Tenant model object if found, otherwise null.
     */
    public Tenant findByUuid(String tenantUuid) {
        String sql = "SELECT * FROM tenants WHERE uuid = ?";
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantUuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToModel(rs); // Assuming you have a mapToModel method in TenantDAO
            }
        } catch (SQLException e) {
            System.err.println("TenantDAO.findByUuid: " + e.getMessage());
        }
        return null;
    }

    // You will also need this helper method in TenantDAO if it doesn't exist
    private Tenant mapToModel(ResultSet rs) throws SQLException {
        Tenant t = new Tenant();
        t.setId(rs.getLong("id"));
        t.setUuid(rs.getString("uuid"));
        t.setLicenseKey(rs.getString("license_key"));
        t.setOwnerEmail(rs.getString("owner_email"));
        t.setStatus(rs.getString("status"));
        Date expiryDate = rs.getDate("expiry_date");
        if (expiryDate != null) {
            t.setExpiryDate(expiryDate.toLocalDate());
        }
        return t;
    }
}