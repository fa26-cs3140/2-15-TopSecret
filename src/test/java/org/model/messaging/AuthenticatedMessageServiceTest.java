
package org.model.messaging;

import org.junit.jupiter.api.Test;

import org.managers.AgentManager;
import org.managers.UserAccountStore;
import org.model.UserAccount;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

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


    /** Helper: Creates a service with a valid, authenticated Ghost account. */
    private AuthenticatedMessageService createService(
            UserAccountStore users,
            AgentManager agents,
            MessageStore store) {

        UserAccount account = new UserAccount("ghost", false);

        when(users.getUser("ghost")).thenReturn(account);
        when(users.getUsers()).thenReturn(List.of(account));

        return new AuthenticatedMessageService(
                agents, users, store, account
        );
    }

    /** Correct user identity is passed to the store */
    @Test
    void readMessageUsesAuthenticatedUsername() {
        UserAccountStore users = mock(UserAccountStore.class);
        AgentManager agents = mock(AgentManager.class);
        MessageStore store = mock(MessageStore.class);

        AuthenticatedMessageService service = createService(users, agents, store);

        Message read = new Message(
                5,
                "sender",
                1,
                "Ghost",
                "Secret mission",
                LocalDateTime.now(),
                true
        );

        when(store.markAsRead(5, "ghost")).thenReturn(read);

        Message result = service.readMessage(5);

        assertSame(read, result);

        // The service must use the captured authenticated username.
        verify(store).markAsRead(5, "ghost");

        // It must never use a different username.
        verify(store, never()).markAsRead(5, "sender");
    }

    /** Invalid message IDs are rejected */
    @Test
    void readMessageRejectsInvalidIds() {
        UserAccountStore users = mock(UserAccountStore.class);
        AgentManager agents = mock(AgentManager.class);
        MessageStore store = mock(MessageStore.class);

        AuthenticatedMessageService service =
                createService(users, agents, store);

        assertThrows(IllegalArgumentException.class,
                () -> service.readMessage(0));

        assertThrows(IllegalArgumentException.class,
                () -> service.readMessage(-1));

        // Invalid IDs should never reach the database.
        verify(store, never()).markAsRead(anyLong(), anyString());
    }

    /** Deleted accounts lose messaging access */
    @Test
    void deletedAccountCannotReadMessages() {
        UserAccountStore users = mock(UserAccountStore.class);
        AgentManager agents = mock(AgentManager.class);
        MessageStore store = mock(MessageStore.class);

        AuthenticatedMessageService service =
                createService(users, agents, store);

        // The account is deleted after the service is constructed.
        when(users.getUser("ghost")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.readMessage(5));

        // The service must reject access before touching message storage.
        verify(store, never()).markAsRead(anyLong(), anyString());
    }
}
