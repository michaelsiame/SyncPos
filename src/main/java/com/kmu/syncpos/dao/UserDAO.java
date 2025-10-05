// src/main/java/com/kmu/syncpos/dao/UserDAO.java
package com.kmu.syncpos.dao;

import com.kmu.syncpos.dto.UserDTO;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.util.DatabaseManager;
import com.kmu.syncpos.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing users.
 * This class handles user authentication, CRUD operations, and synchronization.
 * It is unique in that it maps to both UserDTOs (for data transfer) and
 * User models (for application session state).
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    private static final String BASE_SELECT_SQL = """
        SELECT id, uuid, tenant_id, username, firstname, lastname, password_hash, email, phone, role, is_active,
               last_updated_at, is_synced, is_deleted
        FROM users
        """;

    private static final String AUTHENTICATE_USER_SQL = BASE_SELECT_SQL + " WHERE username = ? AND tenant_id = ? AND is_active = true AND is_deleted = false";
    private static final String FIND_BY_USERNAME_AND_TENANT_SQL = BASE_SELECT_SQL + " WHERE username = ? AND tenant_id = ? AND is_deleted = false";
    private static final String FIND_BY_USERNAME_GLOBAL_SQL = BASE_SELECT_SQL + " WHERE username = ? AND is_active = true AND is_deleted = false"; // Added
    private static final String GET_ALL_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_deleted = false ORDER BY username";
    private static final String GET_BY_ID_SQL = BASE_SELECT_SQL + " WHERE id = ? AND tenant_id = ? AND is_deleted = false";
    private static final String GET_UNSYNCED_SQL = BASE_SELECT_SQL + " WHERE tenant_id = ? AND is_synced = false";

    private static final String CREATE_LOCAL_SQL = """
        INSERT INTO users(username, firstname, lastname, password_hash, email, phone, role, is_active,
                          uuid, tenant_id, last_updated_at, is_synced, is_deleted)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, false)
        """;

    private static final String UPDATE_LOCAL_BASE_SQL = "UPDATE users SET username = ?, firstname = ?, lastname = ?, email = ?, phone = ?, role = ?, is_active = ?, last_updated_at = ?, is_synced = false";
    private static final String UPDATE_LOCAL_SQL = UPDATE_LOCAL_BASE_SQL + " WHERE id = ? AND tenant_id = ?";
    private static final String UPDATE_LOCAL_WITH_PASSWORD_SQL = UPDATE_LOCAL_BASE_SQL + ", password_hash = ? WHERE id = ? AND tenant_id = ?";

    private static final String UPSERT_REMOTE_SQL = """
        INSERT INTO users (uuid, tenant_id, username, firstname, lastname, password_hash, email, phone, role, is_active, is_deleted, last_updated_at, is_synced)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)
        ON CONFLICT(uuid) DO UPDATE SET
            tenant_id = excluded.tenant_id, username = excluded.username, firstname = excluded.firstname, lastname = excluded.lastname, password_hash = excluded.password_hash,
            email = excluded.email, phone = excluded.phone, role = excluded.role, is_active = excluded.is_active, is_deleted = excluded.is_deleted,
            last_updated_at = excluded.last_updated_at, is_synced = true
        """;

    private static final String MARK_DELETED_SQL = "UPDATE users SET is_deleted = true, is_active = false, is_synced = false, last_updated_at = ? WHERE id = ? AND tenant_id = ?";
    private static final String MARK_SYNCED_SQL = "UPDATE users SET is_synced = true, last_updated_at = ? WHERE id = ? AND tenant_id = ?";

    /**
     * Authenticates a user against the database. On success, returns a full User model
     * suitable for managing the application's session state.
     *
     * @param username     The user's username.
     * @param plainPassword The user's plain text password.
     * @param tenantUuid   The tenant's UUID to scope the login.
     * @return A User model if authentication is successful; otherwise, null.
     */
    public User authenticateUser(String username, String plainPassword, String tenantUuid) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(AUTHENTICATE_USER_SQL)) {
            ps.setString(1, username);
            ps.setString(2, tenantUuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                if (PasswordUtil.checkPassword(plainPassword, hash)) {
                    return mapToModel(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during user authentication for: " + username, e);
        }
        return null;
    }

    /**
     * Finds a user by their username across all tenants.
     * This should only be used for the initial login step where the tenant is not yet known.
     * It assumes that usernames are globally unique across the system.
     *
     * @param username The username to search for.
     * @return A User model if found, otherwise null.
     */
    public User findByUsername(String username) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME_GLOBAL_SQL)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToModel(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find user by global username: " + username, e);
        }
        return null;
    }

    /**
     * Finds a user by their username within a specific tenant. Useful for checking
     * if a username is already taken before creating a new user.
     *
     * @param username The username to search for.
     * @param tenantId The tenant's UUID to scope the search.
     * @return A User model if found, otherwise null.
     */
    public User findByUsernameAndTenant(String username, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME_AND_TENANT_SQL)) {
            ps.setString(1, username);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToModel(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find user by username: " + username + " for tenant: " + tenantId, e);
        }
        return null;
    }

    /**
     * Retrieves all non-deleted users for a specific tenant.
     * @param tenantId The UUID of the tenant.
     * @return A list of UserDTOs.
     */
    public List<UserDTO> getAll(String tenantId) {
        List<UserDTO> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all users for tenant: " + tenantId, e);
        }
        return users;
    }

    /**
     * Retrieves a single non-deleted user by their local database ID.
     * @param userId The ID of the user.
     * @param tenantId The UUID of the tenant.
     * @return A UserDTO if found, otherwise null.
     */
    public UserDTO getById(long userId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BY_ID_SQL)) {
            ps.setLong(1, userId);
            ps.setString(2, tenantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapToDTO(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get user by ID: " + userId, e);
        }
        return null;
    }

    /**
     * Creates a new user from local input, hashing the provided plain text password.
     * @param dto      The user data to add. Must contain a plain password.
     * @param tenantId The UUID of the tenant.
     */
    public void createLocal(UserDTO dto, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_LOCAL_SQL)) {
            ps.setString(1, dto.getUsername());
            ps.setString(2, dto.getFirstname());
            ps.setString(3, dto.getLastname());
            ps.setString(4, PasswordUtil.hashPassword(dto.getPlainPassword()));
            ps.setString(5, dto.getEmail());
            ps.setString(6, dto.getPhone());
            ps.setString(7, dto.getRole());
            ps.setBoolean(8, dto.isActive());
            ps.setString(9, UUID.randomUUID().toString());
            ps.setString(10, tenantId);
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create local user: " + dto.getUsername(), e);
        }
    }

    /**
     * Updates an existing user. If a plain password is provided in the DTO, it will be
     * hashed and updated; otherwise, the password remains unchanged.
     * @param dto      The user data to update.
     * @param tenantId The UUID of the tenant.
     */
    public void updateLocal(UserDTO dto, String tenantId) {
        boolean changePassword = dto.getPlainPassword() != null && !dto.getPlainPassword().isEmpty();
        String sql = changePassword ? UPDATE_LOCAL_WITH_PASSWORD_SQL : UPDATE_LOCAL_SQL;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, dto.getUsername());
            ps.setString(idx++, dto.getFirstname());
            ps.setString(idx++, dto.getLastname());
            ps.setString(idx++, dto.getEmail());
            ps.setString(idx++, dto.getPhone());
            ps.setString(idx++, dto.getRole());
            ps.setBoolean(idx++, dto.isActive());
            ps.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.now()));
            if (changePassword) {
                ps.setString(idx++, PasswordUtil.hashPassword(dto.getPlainPassword()));
            }
            ps.setLong(idx++, dto.getId());
            ps.setString(idx, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update local user: " + dto.getId(), e);
        }
    }

    /**
     * Inserts or updates a user from a remote source, marking it as synced.
     * This method assumes the DTO contains a pre-hashed password.
     * @param dto The complete user data from the remote source.
     */
    public void upsertRemote(UserDTO dto) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_REMOTE_SQL)) {
            int idx = 1;
            ps.setString(idx++, dto.getUuid());
            ps.setString(idx++, dto.getTenantId());
            ps.setString(idx++, dto.getUsername());
            ps.setString(idx++, dto.getFirstname());
            ps.setString(idx++, dto.getLastname());
            ps.setString(idx++, dto.getPasswordHash());
            ps.setString(idx++, dto.getEmail());
            ps.setString(idx++, dto.getPhone());
            ps.setString(idx++, dto.getRole());
            ps.setBoolean(idx++, dto.isActive());
            ps.setBoolean(idx++, dto.isDeleted());
            ps.setTimestamp(idx, Timestamp.valueOf(dto.getLastUpdatedAt().toLocalDateTime()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to upsert remote user with UUID: " + dto.getUuid(), e);
        }
    }

    /**
     * Performs a soft delete on a user, also marking them as inactive.
     * @param userId   The local ID of the user to delete.
     * @param tenantId The UUID of the tenant.
     */
    public void markAsDeleted(long userId, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_DELETED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, userId);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark user as deleted: " + userId, e);
        }
    }

    /**
     * Retrieves all users that have not been synced.
     * @param tenantId The UUID of the tenant.
     * @return A list of unsynced UserDTOs.
     */
    public List<UserDTO> getUnsynced(String tenantId) {
        List<UserDTO> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_UNSYNCED_SQL)) {
            ps.setString(1, tenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapToDTO(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get unsynced users for tenant: " + tenantId, e);
        }
        return users;
    }

    /**
     * Marks a specific user as synced.
     * @param id The local database ID of the user.
     * @param tenantId The UUID of the tenant.
     */
    public void markAsSynced(long id, String tenantId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(MARK_SYNCED_SQL)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, id);
            ps.setString(3, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark user as synced: " + id, e);
        }
    }

    /**
     * Maps a {@link ResultSet} row to a {@link UserDTO} object for data transfer.
     * @param rs The ResultSet to map.
     * @return A populated UserDTO object.
     * @throws SQLException if a database access error occurs.
     */
    private UserDTO mapToDTO(ResultSet rs) throws SQLException {
        UserDTO u = new UserDTO();
        u.setId(rs.getLong("id"));
        u.setUuid(rs.getString("uuid"));
        u.setTenantId(rs.getString("tenant_id"));
        u.setUsername(rs.getString("username"));
        u.setFirstname(rs.getString("firstname"));
        u.setLastname(rs.getString("lastname"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) {
            u.setLastUpdatedAt(ts.toLocalDateTime().atOffset(ZoneOffset.UTC));
        }
        u.setIsSynced(rs.getInt("is_synced")); // Corrected to use getBoolean
        u.setDeleted(rs.getBoolean("is_deleted"));
        return u;
    }

    /**
     * Maps a {@link ResultSet} row to a {@link User} model object for application logic.
     * @param rs The ResultSet to map.
     * @return A populated User model object.
     * @throws SQLException if a database access error occurs.
     */
    private User mapToModel(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUuid(rs.getString("uuid"));
        u.setTenantId(rs.getString("tenant_id"));
        u.setUsername(rs.getString("username"));
        u.setFirstname(rs.getString("firstname"));
        u.setLastname(rs.getString("lastname"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setIsActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) {
            u.setLastUpdatedAt(ts.toLocalDateTime());
        }
        u.setIsSynced(rs.getInt("is_synced")); // Corrected to use getBoolean
        u.setIsDeleted(rs.getBoolean("is_deleted"));
        return u;
    }
}