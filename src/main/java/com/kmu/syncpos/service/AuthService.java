// src/main/java/com/kmu/syncpos/service/AuthService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.SessionContext;
import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.TenantDAO;
import com.kmu.syncpos.dao.UserDAO;
import com.kmu.syncpos.dto.TenantDTO;
import com.kmu.syncpos.dto.auth.LoginResult;
import com.kmu.syncpos.models.Tenant;
import com.kmu.syncpos.models.User;
import com.kmu.syncpos.util.ModelMapper;
import com.kmu.syncpos.util.PasswordUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class responsible for handling application authentication,
 * session management, and initial activation.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    private final UserDAO userDAO = new UserDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final ApiService apiService = new ApiService();

    /**
     * Attempts to log in a user and set up the application contexts.
     * The login process assumes that usernames are unique across the entire system.
     *
     * @param username The user's username.
     * @param password The user's plain text password.
     * @return A LoginResult object indicating success or failure with a corresponding message.
     */
    public LoginResult login(String username, String password) {
        // 1. Find the user by username globally.
        User user = userDAO.findByUsername(username);

        // 2. Check if the user exists and the password is correct.
        if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            LOGGER.warning("Login failed for username: " + username + ". Invalid credentials.");
            return LoginResult.failure("Invalid username or password.");
        }

        // 3. Use the tenant ID from the successfully authenticated user to find the tenant.
        Tenant tenant = tenantDAO.findByUuid(user.getTenantId());

        // 4. Check if the associated tenant is valid and active.
        if (tenant == null) {
            LOGGER.severe("Login failed for user " + username + ". Associated tenant not found with UUID: " + user.getTenantId());
            return LoginResult.failure("Associated tenant account not found.");
        }
        if (!"ACTIVE".equals(tenant.getStatus())) {
            LOGGER.warning("Login failed for user " + username + ". Tenant '" + tenant.getOwnerEmail() + "' is not active.");
            return LoginResult.failure("Associated tenant account is not active.");
        }

        // 5. Success! Set the application-wide contexts.
        TenantContext.setTenant(tenant);
        SessionContext.setCurrentUser(user);
        LOGGER.info("User '" + username + "' successfully logged in for tenant '" + tenant.getOwnerEmail() + "'.");

        return LoginResult.success(user);
    }

    /**
     * Logs the current user out by clearing the application contexts.
     */
    public void logout() {
        TenantContext.clear();
        SessionContext.clear();
        LOGGER.info("User session cleared (logout).");
    }

    /**
     * Activates the application using a license key by fetching tenant data
     * from a remote API, saving it locally, and setting the tenant context.
     *
     * @param licenseKey The license key to activate.
     * @return true if activation was successful, false otherwise.
     */
    public boolean activateApplication(String licenseKey) {
        // 1. Fetch tenant data from the remote API.
        TenantDTO tenantDto = apiService.getTenantByKey(licenseKey);

        if (tenantDto == null) {
            LOGGER.warning("Activation failed: No tenant found for the provided license key.");
            return false;
        }
        if (!"ACTIVE".equals(tenantDto.getStatus())) {
            LOGGER.warning("Activation failed: Tenant '" + tenantDto.getLicenseKey() + "' is not active.");
            return false;
        }

        try {
            // 2. Save the tenant information to the local database.
            // This assumes a method similar to `upsertRemote` exists in TenantDAO.
            tenantDAO.saveTenant(tenantDto);

            // 3. Convert the DTO to a Model for use in the application context.
            Tenant tenantModel = ModelMapper.fromDto(tenantDto);
            TenantContext.setTenant(tenantModel);

            LOGGER.info("Application successfully activated for tenant: " + tenantDto.getUuid());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Activation failed due to an unexpected error.", e);
            return false;
        }
    }
}