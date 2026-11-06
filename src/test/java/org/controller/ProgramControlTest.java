package org.controller;

import org.managers.AuditLogManager;
import org.managers.FileHandler;
import org.managers.UserAccountManager;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.managers.*;

public class ProgramControlTest {
    private static final String ORIGINAL =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private static final String CIPHERED =
            "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";

    // Helper method: Create a new ProgramControl instance using mocks
    private ProgramControl createControl(FileHandler fileHandler) {
        return new ProgramControl(
                fileHandler,
                mock(BriefManager.class),
                mock(AgentManager.class),
                mock(FacilityManager.class),
                mock(AuditLogManager.class),
                mock(UserAccountManager.class),
                mock(MissionManager.class)
        );
    }

    // mock FileHandler to test ProgramControl by itself
    @Test
    void getAvailableFilesReturnsFiles() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.getAvailableFiles())
                .thenReturn(List.of("carnivore.txt", "cointelpro.txt"));

        ProgramControl control = createControl(fileHandler);
        List<String> result = control.getAvailableFiles();

        assertEquals(
                List.of("carnivore.txt", "cointelpro.txt"),
                result
        );

        verify(fileHandler).getAvailableFiles();
    }

    @Test
    void defaultKeyReturnsReadableText() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.readFile("key.txt"))
                .thenReturn(ORIGINAL + "\n" + CIPHERED);

        when(fileHandler.readFile("secret.txt"))
                .thenReturn("Hello World!");

        ProgramControl control = createControl(fileHandler);
        String result = control.getFileContents("secret.txt");

        assertEquals("Hello World!", result);
    }

    @Test
    void alternateKeyDeciphersContents() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.readFile("key.txt"))
                .thenReturn(ORIGINAL + "\n" + CIPHERED);

        when(fileHandler.readFile("secret.txt"))
                .thenReturn("bcd");

        ProgramControl control = createControl(fileHandler);
        String result = control.getFileContents("secret.txt", 1);

        assertEquals("abc", result);
    }

    // Make sure errors are handled correctly
    @Test
    void missingFileReturnsNull() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.readFile("key.txt"))
                .thenReturn(ORIGINAL + "\n" + CIPHERED);

        when(fileHandler.readFile("missing.txt"))
                .thenReturn(null);

        ProgramControl control = createControl(fileHandler);

        assertNull(control.getFileContents("missing.txt"));
    }

    @Test
    void missingKeyReturnsNull() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.readFile("key.txt"))
                .thenReturn(null);

        ProgramControl control = createControl(fileHandler);

        assertNull(control.getFileContents("secret.txt"));
    }

    @Test
    void invalidKeyIndexThrowsException() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.readFile("key.txt"))
                .thenReturn(ORIGINAL + "\n" + CIPHERED);

        ProgramControl control = createControl(fileHandler);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.getFileContents("secret.txt", 10)
        );
    }

    @Test
    void nullFileHandlerThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createControl(null)
        );
    }

    private ProgramControl controlWithAccounts(UserAccountManager accounts) {
        return new ProgramControl(mock(FileHandler.class), mock(BriefManager.class), mock(AgentManager.class),
                mock(FacilityManager.class), mock(AuditLogManager.class), accounts, mock(MissionManager.class));
    }

    @Test
    void webViewDoesNotStartWithoutAccounts() {
        UserAccountManager accounts = mock(UserAccountManager.class);
        when(accounts.hasUsers()).thenReturn(false);

        assertNull(controlWithAccounts(accounts).startWeb(3013));
    }

    @Test
    void webViewStartsWhenAccountsExist() {
        UserAccountManager accounts = mock(UserAccountManager.class);
        when(accounts.hasUsers()).thenReturn(true);

        WebServer server = controlWithAccounts(accounts).startWeb(3013);

        assertNotNull(server);
        assertEquals(3013, server.getPort());
        server.stop();
    }
}
