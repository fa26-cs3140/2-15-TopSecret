package org.example;

import java.util.List;

/**
 * Persistence boundary for user-account metadata.
 * Passwords are intentionally not stored through this interface.
 */
public interface UserAccountStore {

    boolean hasUsers();

    boolean addUser(UserAccount account);

    UserAccount getUser(String username);

    List<UserAccount> getUsers();

    boolean setAdmin(String username, boolean admin);
}
