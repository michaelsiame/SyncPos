// src/main/java/com/kmu/syncpos/service/UserService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.UserDAO;
import com.kmu.syncpos.dto.UserDTO;
import com.kmu.syncpos.models.User; // <-- Import the User model

import java.util.Collections;
import java.util.List;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Authenticates a user for a given tenant. This is the primary entry point for login logic.
     * @param username The username to authenticate.
     * @param password The plain text password to check.
     * @param tenantUuid The UUID of the tenant the user is trying to log into.
     * @return A User model object if authentication is successful, otherwise null.
     */
    public User authenticate(String username, String password, String tenantUuid) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty() || tenantUuid == null || tenantUuid.isEmpty()) {
            return null;
        }
        return userDAO.authenticateUser(username, password, tenantUuid);
    }

    /**
     * Retrieves a single user by their ID for the current tenant.
     * @param userId The ID of the user to retrieve.
     * @return A UserDTO if found, otherwise null.
     */
    public UserDTO getUserById(long userId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return userDAO.getById(userId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("UserService: Tenant context not available.");
            return null;
        }
    }

    /**
     * Checks if a username is already taken within the current tenant.
     * @param username The username to check.
     * @return true if the username exists, false otherwise.
     */
    public boolean isUsernameTaken(String username) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return userDAO.findByUsernameAndTenant(username, tenantId) != null;
        } catch (IllegalStateException e) {
            System.err.println("UserService: Tenant context not available.");
            return false;
        }
    }

    public List<UserDTO> getAllActiveUsers() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return userDAO.getAll(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("UserService: Tenant context not available.");
            return Collections.emptyList();
        }
    }

    public void saveUser(UserDTO dto) {
        if (dto == null) return;
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            if (dto.getId() == 0) {
                userDAO.createLocal(dto, tenantId);
            } else {
                userDAO.updateLocal(dto, tenantId);
            }
        } catch (IllegalStateException e) {
            System.err.println("UserService: Tenant context not available.");
        }
    }

    public void deleteUser(long userId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            userDAO.markAsDeleted(userId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("UserService: Tenant context not available.");
        }
    }

    public List<UserDTO> getUnsyncedUsers(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return userDAO.getUnsynced(tenantId);
    }
}