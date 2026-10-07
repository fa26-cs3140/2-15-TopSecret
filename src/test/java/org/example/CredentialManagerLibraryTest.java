package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CredentialManagerLibraryTest {

    private FileHandler fileHandler;
    private Cipher cipher;
    private CredentialManagerLibrary credentialManager;

    @BeforeEach
    void setUp() {
        fileHandler = mock(FileHandler.class);
        cipher = mock(Cipher.class);
        credentialManager = new CredentialManagerLibrary(fileHandler, cipher);
    }

    @Test
    void validUsername() {
        assertTrue(credentialManager.isValidUsername("dillon"));
        assertFalse(credentialManager.isValidUsername("KingDillon"));
    }

    @Test
    void validPassword() {
        assertTrue(credentialManager.isValidPassword("dcow12"));
        assertFalse(credentialManager.isValidPassword("dcow"));
    }

    @Test
    void credentialsExist() {
        when(fileHandler.getAvailableFiles())
                .thenReturn(List.of("credentials.cip"));

        assertTrue(credentialManager.credentialsExist());
    }

    @Test
    void correctLogin() {
        when(fileHandler.readFile("credentials.cip"))
                .thenReturn("encrypted");
        when(cipher.decipher("encrypted"))
                .thenReturn("dillon\ndcow12");

        assertTrue(credentialManager.verifyLogin("dillon", "dcow12"));
    }

    @Test
    void createCredentials() {
        when(cipher.cipher("dillon\ndcow12"))
                .thenReturn("encrypted");
        when(fileHandler.writeFile("credentials.cip", "encrypted"))
                .thenReturn(true);

        assertTrue(credentialManager.createCredentials("dillon", "dcow12"));
    }

    @Test
    void changePassword() {
        when(cipher.cipher("dillon\ndcow123"))
                .thenReturn("encrypted");
        when(fileHandler.writeFile("credentials.cip", "encrypted"))
                .thenReturn(true);

        assertTrue(credentialManager.changePassword("dillon", "dcow123"));
    }
}