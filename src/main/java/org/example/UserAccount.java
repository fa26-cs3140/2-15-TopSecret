package org.example;

/**
 * Represents one user account.
 * Password information is intentionally not stored in this object.
 */
public class UserAccount {

    private String username;
    private boolean admin;

    public UserAccount(String username, boolean admin) {
        this.username = username;
        this.admin = admin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}