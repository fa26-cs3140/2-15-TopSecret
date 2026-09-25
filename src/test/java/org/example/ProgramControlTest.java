package org.example;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProgramControlTest {
    private static final String ORIGINAL =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private static final String CIPHERED =
            "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";

    // mock FileHandler to test ProgramControl by itself
    @Test
    void getAvailableFilesReturnsFiles() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.getAvailableFiles())
                .thenReturn(List.of("carnivore.txt", "cointelpro.txt"));

        ProgramControl control = new ProgramControl(fileHandler);
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

        ProgramControl control = new ProgramControl(fileHandler);
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

        ProgramControl control = new ProgramControl(fileHandler);
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

        ProgramControl control = new ProgramControl(fileHandler);

        assertNull(control.getFileContents("missing.txt"));
    }

    @Test
    void missingKeyReturnsNull() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.readFile("key.txt"))
                .thenReturn(null);

        ProgramControl control = new ProgramControl(fileHandler);

        assertNull(control.getFileContents("secret.txt"));
    }

    @Test
    void invalidKeyIndexThrowsException() {
        FileHandler fileHandler = mock(FileHandler.class);

        when(fileHandler.readFile("key.txt"))
                .thenReturn(ORIGINAL + "\n" + CIPHERED);

        ProgramControl control = new ProgramControl(fileHandler);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.getFileContents("secret.txt", 10)
        );
    }

    @Test
    void nullFileHandlerThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProgramControl(null)
        );
    }
}
