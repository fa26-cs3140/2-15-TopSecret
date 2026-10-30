package org.example;

import java.util.List;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

/**
 * Manages Homework 5 user accounts and their ciphered password files.
 * User metadata is stored through UserAccountStore. Passwords are never stored in SQLite.
 */
public class UserAccountManager implements CredentialManager {

    private final UserAccountStore accountStore;
    private final FileHandler fileHandler;
    private final Cipher cipher;

    public UserAccountManager(UserAccountStore accountStore, FileHandler fileHandler, Cipher cipher) {
        if (accountStore == null || fileHandler == null || cipher == null) {
            throw new IllegalArgumentException("Dependencies cannot be null.");
        }
        this.accountStore = accountStore;
        this.fileHandler = fileHandler;
        this.cipher = cipher;
    }

    public UserAccountManager(String url, FileHandler fileHandler, Cipher cipher) {
        this(createStore(url), fileHandler, cipher);
    }

    public boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9_-]{3,}");
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 5) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        for (char character : password.toCharArray()) {
            if (Character.isUpperCase(character)) {
                hasUpper = true;
            }
            if (Character.isLowerCase(character)) {
                hasLower = true;
            }
        }
        return hasUpper && hasLower;
    }

    /** Compatibility with the existing authentication boundary. */
    @Override
    public boolean credentialsExist() {
        return hasUsers();
    }

    /** Bootstrap the first admin, or set an existing account's initial password.
     * Account provisioning and authorization remain the caller's responsibility.
     */
    @Override
    public boolean createCredentials(String username, String password) {
        if (!isValidUsername(username) || !isValidPassword(password)) {
            return false;
        }
        if (!hasUsers()) {
            createFirstUser(username, password);
            return true;
        }
        return setInitialPassword(username, password);
    }

    public boolean hasUsers() {
        return accountStore.hasUsers();
    }

    /**
     * Creates account metadata only. A password is set by the associated user separately.
     */
    public UserAccount createUser(String username, boolean admin) {
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username.");
        }
        if (accountStore.getUser(username) != null) {
            throw new IllegalArgumentException("Username already exists.");
        }

        UserAccount account = new UserAccount(username, admin);
        if (!accountStore.addUser(account)) {
            throw new IllegalStateException("Unable to create user account.");
        }
        return account;
    }

    /**
     * Creates the first account as an admin and sets that user's initial password.
     */
    public UserAccount createFirstUser(String username, String password) {
        if (hasUsers()) {
            throw new IllegalStateException("A user account already exists.");
        }
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("Invalid password.");
        }

        UserAccount account = createUser(username, true);
        if (!writePassword(username, password)) {
            throw new IllegalStateException("Unable to save password.");
        }
        return account;
    }

    public UserAccount getUser(String username) {
        return accountStore.getUser(username);
    }

    public List<UserAccount> getUsers() {
        return accountStore.getUsers();
    }

    public boolean passwordExists(String username) {
        if (!isValidUsername(username)) {
            return false;
        }
        return fileHandler.getAvailableFiles().contains(credentialFile(username));
    }

    /**
     * Sets a password only when the account exists and does not already have one.
     */
    public boolean setInitialPassword(String username, String password) {
        if (!isValidUsername(username) || accountStore.getUser(username) == null || passwordExists(username) || !isValidPassword(password)) {
            return false;
        }
        return writePassword(username, password);
    }

    public boolean verifyLogin(String username, String password) {
        if (!isValidUsername(username) || accountStore.getUser(username) == null || password == null) {
            return false;
        }
        String encrypted = fileHandler.readFile(credentialFile(username));
        if (encrypted == null) {
            return false;
        }
        return wrapPassword(password).equals(cipher.decipher(encrypted));
    }

    public boolean changePassword(String username, String newPassword) {
        if (!isValidUsername(username) || accountStore.getUser(username) == null || !isValidPassword(newPassword)) {
            return false;
        }
        return writePassword(username, newPassword);
    }

    private boolean writePassword(String username, String password) {
        return fileHandler.writeFile(credentialFile(username), cipher.cipher(wrapPassword(password)));
    }

    private String credentialFile(String username) {
        // SQLite usernames are case-sensitive; filenames must stay distinct even on macOS.
        return "credentials-" + HexFormat.of().formatHex(username.getBytes(StandardCharsets.UTF_8)) + ".cip";
    }

    private String wrapPassword(String password) {
        return "PWD:" + password + ":END";
    }

    private static UserAccountStore createStore(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null.");
        }
        return new SqliteUserAccountStore(url);
    }
}
