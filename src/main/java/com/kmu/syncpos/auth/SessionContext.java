// src/main/java/com/kmu/syncpos/auth/SessionContext.java
package com.kmu.syncpos.auth;

import com.kmu.syncpos.models.User; // <-- CHANGED from UserDTO to User model

/**
 * A singleton context to hold the state for the current user's session.
 * This is set upon successful login and cleared upon logout.
 * It stores the User *model* object, as this is what the UI layer consumes.
 */
public final class SessionContext {

    private static SessionContext instance;
    // The type has been changed from UserDTO to User.
    private User currentUser;

    private SessionContext() {}

    public static synchronized SessionContext getInstance() {
        if (instance == null) {
            instance = new SessionContext();
        }
        return instance;
    }

    /**
     * Sets the currently logged-in user for the application session.
     * @param user The authenticated user's model object.
     */
    public static void setCurrentUser(User user) { // <-- CHANGED parameter type
        getInstance().currentUser = user;
    }

    /**
     * Retrieves the currently logged-in user.
     * @return The authenticated user's model object.
     * @throws IllegalStateException if no user is currently logged in.
     */
    public static User getCurrentUser() { // <-- CHANGED return type
        if (getInstance().currentUser == null) {
            throw new IllegalStateException("No user is currently logged in. SessionContext not initialized.");
        }
        return getInstance().currentUser;
    }

    /**
     * Clears the user session, typically on logout.
     */
    public static void clear() {
        getInstance().currentUser = null;
    }
}