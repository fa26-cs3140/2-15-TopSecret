
package org.model.messaging;

import org.junit.jupiter.api.Test;

import org.managers.AgentManager;
import org.managers.UserAccountStore;
import org.model.UserAccount;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticatedMessageService.
 *
 * Verifies that messaging uses the authenticated user's identity
 * and rejects invalid or nonexistent accounts.
 */
class AuthenticatedMessageServiceTest {

    @Test
    void identityIsCapturedAndNeverTakenFromEditableUserObject() {
        // Create mocked dependencies.
        UserAccountStore users = mock(UserAccountStore.class);
        AgentManager agents = mock(AgentManager.class);
        MessageStore store = mock(MessageStore.class);

        // Create an authenticated user.
        UserAccount account = new UserAccount("ghost", false);

        // Configure the user store to recognize the account.
        when(users.getUser("ghost")).thenReturn(new UserAccount("ghost", false));

        when(users.getUsers()).thenReturn(List.of(new UserAccount("ghost", false)));

        // Create the service while the user is authenticated as "ghost".
        AuthenticatedMessageService service = new AuthenticatedMessageService(
                agents,
                users,
                store,
                account
        );

        // Modify the original UserAccount object after authentication.
        account.setUsername("admin");

        // The service should still use the original authenticated username.
        service.getInbox();

        verify(store).findForRecipient("ghost");
        verify(store, never()).findForRecipient("admin");
    }

    @Test
    void rejectsNullDependenciesAndDeletedAccounts() {
        // All messaging dependencies are required.
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedMessageService(null, null, null, null));

        // A user whose account no longer exists cannot access messaging.
        UserAccountStore users = mock(UserAccountStore.class);

        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedMessageService(
                        mock(AgentManager.class),
                        users,
                        mock(MessageStore.class),
                        new UserAccount("missing", false)
                ));
    }
}
