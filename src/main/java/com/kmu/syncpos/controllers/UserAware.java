package com.kmu.syncpos.controllers;

import com.kmu.syncpos.models.User;

/**
 * An interface for controllers that need access to the currently logged-in user.
 * This provides a standard way for the MainController to pass user data to
 * sub-views.
 */
public interface UserAware {
    /**
     * Sets the user object for the controller.
     * @param user The user who is currently logged into the system.
     */
    void setUser(User user);
}