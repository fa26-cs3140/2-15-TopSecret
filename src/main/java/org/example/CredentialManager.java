package org.example;

public interface CredentialManager {
    boolean credentialsExist();

    boolean isValidUsername(String username);

    boolean isValidPassword(String password);

    boolean verifyLogin(String username, String password);

    boolean createCredentials(String username, String password);

    boolean changePassword(String username, String newPassword);
}
