package org.managers;

import java.util.List;
import org.model.UserAccount;

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
