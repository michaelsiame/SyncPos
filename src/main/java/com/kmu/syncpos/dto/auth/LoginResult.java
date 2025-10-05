// src/main/java/com/kmu/syncpos/dto/auth/LoginResult.java
package com.kmu.syncpos.dto.auth;

import com.kmu.syncpos.models.User;

/**
 * A Data Transfer Object to encapsulate the result of a login attempt.
 * This provides a clean way for the AuthService to communicate all possible
 * outcomes (success, failure, tenant not set) to the UI controller.
 */
public class LoginResult {

    public enum Status {
        SUCCESS,
        INVALID_CREDENTIALS,
        TENANT_NOT_SET
    }

    private final Status status;
    private final User user;
    private final String message;

    private LoginResult(Status status, User user, String message) {
        this.status = status;
        this.user = user;
        this.message = message;
    }

    public static LoginResult success(User user) {
        return new LoginResult(Status.SUCCESS, user, "Login successful.");
    }

    public static LoginResult failure(String message) {
        return new LoginResult(Status.INVALID_CREDENTIALS, null, message);
    }

    public static LoginResult tenantNotSet() {
        return new LoginResult(Status.TENANT_NOT_SET, null, "Error: No active license found. Please activate the application.");
    }

    public Status getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }
}