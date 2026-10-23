package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountManagerTest {

    @Mock
    private UserAccountStore store;

    @Mock
    private FileHandler fileHandler;

    @Mock
    private Cipher cipher;

    private UserAccountManager manager;

    @BeforeEach
    void setUp() {
        manager = new UserAccountManager(store, fileHandler, cipher);
    }

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserAccountManager((UserAccountStore) null, fileHandler, cipher));
        assertThrows(IllegalArgumentException.class,
                () -> new UserAccountManager(store, null, cipher));
        assertThrows(IllegalArgumentException.class,
                () -> new UserAccountManager(store, fileHandler, null));
        assertThrows(IllegalArgumentException.class,
                () -> new UserAccountManager((DatabaseManager) null, fileHandler, cipher));
    }

    @Test
    void usernameValidationMatchesHomeworkRules() {
        assertTrue(manager.isValidUsername("abc"));
        assertTrue(manager.isValidUsername("Agent_7"));
        assertTrue(manager.isValidUsername("field-agent"));
        assertTrue(manager.isValidUsername("a1_"));

        assertFalse(manager.isValidUsername(null));
        assertFalse(manager.isValidUsername("ab"));
        assertFalse(manager.isValidUsername("bad name"));
        assertFalse(manager.isValidUsername("bad!"));
    }

    @Test
    void passwordValidationRequiresLengthUpperAndLowercase() {
        assertTrue(manager.isValidPassword("Alpha"));
        assertTrue(manager.isValidPassword("Abcde1"));

        assertFalse(manager.isValidPassword(null));
        assertFalse(manager.isValidPassword("Abcd"));
        assertFalse(manager.isValidPassword("abcde"));
        assertFalse(manager.isValidPassword("ABCDE"));
    }

    @Test
    void hasUsersDelegatesToStore() {
        when(store.hasUsers()).thenReturn(true);
        assertTrue(manager.hasUsers());
        verify(store).hasUsers();
    }

    @Test
    void createUserStoresAccountWithoutPassword() {
        when(store.getUser("agent_7")).thenReturn(null);
        when(store.addUser(any(UserAccount.class))).thenReturn(true);

        UserAccount account = manager.createUser("agent_7", false);

        assertEquals("agent_7", account.getUsername());
        assertFalse(account.isAdmin());
        verify(store).addUser(any(UserAccount.class));
        verifyNoInteractions(fileHandler, cipher);
    }

    @Test
    void createUserRejectsInvalidDuplicateAndStoreFailure() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createUser("a!", false));

        when(store.getUser("agent_7")).thenReturn(new UserAccount("agent_7", false));
        assertThrows(IllegalArgumentException.class,
                () -> manager.createUser("agent_7", false));

        when(store.getUser("new_user")).thenReturn(null);
        when(store.addUser(any(UserAccount.class))).thenReturn(false);
        assertThrows(IllegalStateException.class,
                () -> manager.createUser("new_user", false));
    }

    @Test
    void createFirstUserCreatesAdminAndEncryptedPassword() {
        when(store.hasUsers()).thenReturn(false);
        when(store.getUser("first_user")).thenReturn(null);
        when(store.addUser(any(UserAccount.class))).thenReturn(true);
        when(cipher.cipher("PWD:Alpha1:END")).thenReturn("encrypted");
        when(fileHandler.writeFile("credentials-66697273745f75736572.cip", "encrypted")).thenReturn(true);

        UserAccount account = manager.createFirstUser("first_user", "Alpha1");

        assertTrue(account.isAdmin());
        verify(fileHandler).writeFile("credentials-66697273745f75736572.cip", "encrypted");
    }

    @Test
    void createFirstUserRejectsExistingUsersInvalidPasswordAndWriteFailure() {
        when(store.hasUsers()).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> manager.createFirstUser("first_user", "Alpha1"));

        reset(store);
        when(store.hasUsers()).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> manager.createFirstUser("first_user", "lower"));

        when(store.getUser("first_user")).thenReturn(null);
        when(store.addUser(any(UserAccount.class))).thenReturn(true);
        when(cipher.cipher("PWD:Alpha1:END")).thenReturn("encrypted");
        when(fileHandler.writeFile("credentials-66697273745f75736572.cip", "encrypted")).thenReturn(false);
        assertThrows(IllegalStateException.class,
                () -> manager.createFirstUser("first_user", "Alpha1"));
    }

    @Test
    void getUserAndGetUsersDelegateToStore() {
        UserAccount account = new UserAccount("agent_7", false);
        when(store.getUser("agent_7")).thenReturn(account);
        when(store.getUsers()).thenReturn(List.of(account));

        assertSame(account, manager.getUser("agent_7"));
        assertEquals(List.of(account), manager.getUsers());
    }

    @Test
    void passwordExistsUsesPerUserCipherFile() {
        when(fileHandler.getAvailableFiles())
                .thenReturn(List.of("credentials-6167656e745f37.cip"));

        assertTrue(manager.passwordExists("agent_7"));
        assertFalse(manager.passwordExists("bad!"));
    }

    @Test
    void setInitialPasswordRequiresExistingUserNoPasswordAndValidPassword() {
        when(store.getUser("agent_7")).thenReturn(new UserAccount("agent_7", false));
        when(fileHandler.getAvailableFiles()).thenReturn(List.of());
        when(cipher.cipher("PWD:Alpha1:END")).thenReturn("encrypted");
        when(fileHandler.writeFile("credentials-6167656e745f37.cip", "encrypted")).thenReturn(true);

        assertTrue(manager.setInitialPassword("agent_7", "Alpha1"));

        when(store.getUser("missing")).thenReturn(null);
        assertFalse(manager.setInitialPassword("missing", "Alpha1"));

        when(fileHandler.getAvailableFiles()).thenReturn(List.of("credentials-6167656e745f37.cip"));
        assertFalse(manager.setInitialPassword("agent_7", "Alpha1"));

        when(fileHandler.getAvailableFiles()).thenReturn(List.of());
        assertFalse(manager.setInitialPassword("agent_7", "lower"));
    }

    @Test
    void verifyLoginReadsAndDeciphersAssociatedUsersFile() {
        when(store.getUser("agent_7")).thenReturn(new UserAccount("agent_7", false));
        when(fileHandler.readFile("credentials-6167656e745f37.cip")).thenReturn("encrypted");
        when(cipher.decipher("encrypted")).thenReturn("PWD:Alpha1:END");

        assertTrue(manager.verifyLogin("agent_7", "Alpha1"));
        assertFalse(manager.verifyLogin("agent_7", "Wrong1"));

        when(fileHandler.readFile("credentials-6167656e745f37.cip")).thenReturn(null);
        assertFalse(manager.verifyLogin("agent_7", "Alpha1"));

        when(store.getUser("missing")).thenReturn(null);
        assertFalse(manager.verifyLogin("missing", "Alpha1"));
        assertFalse(manager.verifyLogin("agent_7", null));
    }

    @Test
    void changePasswordValidatesUserAndPasswordThenWritesEncryptedFile() {
        when(store.getUser("agent_7")).thenReturn(new UserAccount("agent_7", false));
        when(cipher.cipher("PWD:Bravo2:END")).thenReturn("encrypted2");
        when(fileHandler.writeFile("credentials-6167656e745f37.cip", "encrypted2")).thenReturn(true);

        assertTrue(manager.changePassword("agent_7", "Bravo2"));

        when(store.getUser("missing")).thenReturn(null);
        assertFalse(manager.changePassword("missing", "Bravo2"));
        assertFalse(manager.changePassword("agent_7", "lower"));
    }

    @Test
    void implementsExistingCredentialManagerWithoutOverwritingPasswords() {
        CredentialManager credentials = manager;
        when(store.hasUsers()).thenReturn(true);
        assertTrue(credentials.credentialsExist());
        assertFalse(credentials.createCredentials("bad!", "Alpha1"));
        assertFalse(credentials.createCredentials("agent_7", "lower"));
        assertFalse(credentials.createCredentials("unknown", "Alpha1"));
        when(store.getUser("agent_7")).thenReturn(new UserAccount("agent_7", false));
        when(fileHandler.getAvailableFiles()).thenReturn(List.of("credentials-6167656e745f37.cip"));
        assertFalse(credentials.createCredentials("agent_7", "Alpha1"));
        verifyNoInteractions(cipher);
    }

    @Test
    void credentialInterfaceBootstrapsFirstAdmin() {
        when(store.addUser(any())).thenReturn(true);
        when(cipher.cipher("PWD:Alpha1:END")).thenReturn("encrypted");
        when(fileHandler.writeFile("credentials-66697273745f75736572.cip", "encrypted")).thenReturn(true);
        assertTrue(manager.createCredentials("first_user", "Alpha1"));
        var captor = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(store).addUser(captor.capture());
        assertTrue(captor.getValue().isAdmin());
    }

    @Test
    void invalidUsernamesNeverReachPasswordFilesOrStore() {
        assertFalse(manager.verifyLogin("../abc", "Alpha1"));
        assertFalse(manager.changePassword("../abc", "Alpha1"));
        assertFalse(manager.setInitialPassword("../abc", "Alpha1"));
        verifyNoInteractions(store, fileHandler, cipher);
    }
}
