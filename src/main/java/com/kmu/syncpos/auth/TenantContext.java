// src/main/java/com/kmu/syncpos/auth/TenantContext.java
package com.kmu.syncpos.auth;

import com.kmu.syncpos.models.Tenant; // <-- The context will work with the Tenant ViewModel

/**
 * A singleton context to hold the active Tenant's data (ViewModel) for the application's environment.
 * This is set after activation or on app startup if a tenant exists.
 */
public final class TenantContext {

    private static TenantContext instance;

    // --- The single, consistently named field ---
    private Tenant activeTenant;

    private TenantContext() {}

    public static synchronized TenantContext getInstance() {
        if (instance == null) {
            instance = new TenantContext();
        }
        return instance;
    }

    /**
     * Sets the active tenant for the application session.
     * @param tenant The Tenant ViewModel.
     */
    public static void setTenant(Tenant tenant) { // <-- Accepts the Tenant ViewModel
        getInstance().activeTenant = tenant;
    }

    /**
     * Retrieves the active tenant.
     * @return The active Tenant ViewModel.
     * @throws IllegalStateException if no tenant is currently active.
     */
    public static Tenant getTenant() { // <-- Returns the Tenant ViewModel
        if (getInstance().activeTenant == null) {
            throw new IllegalStateException("No tenant is active. TenantContext not initialized.");
        }
        return getInstance().activeTenant;
    }

    /**
     * Clears the tenant context on logout.
     */
    public static void clear() {
        getInstance().activeTenant = null;
    }
}