package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UserAccountManagerCredentialManagerTest {

    private Path directory;
    private Path database;
    private UserAccountManager manager;

    @BeforeEach
    void setUp() throws Exception {
        directory = Files.createTempDirectory("topsecret-accounts");
        database = Files.createTempFile("topsecret-accounts", ".db");

        FileHandler fileHandler = new FileHandler(directory.toString());
        String original = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        String encrypted = "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";
        Cipher cipher = new Cipher(new CipherKey(original + "\n" + encrypted));

        manager = new UserAccountManager(
                new DatabaseManager("jdbc:sqlite:" + database.toAbsolutePath()),
                fileHandler,
                cipher
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        try (var files = Files.list(directory)) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        }
        Files.deleteIfExists(directory);
        Files.deleteIfExists(database);
    }

    @Test
    void supportsMultipleUsersWithSeparateCipheredPasswords() {
        UserAccount first = manager.createFirstUser("pat", "Alpha1");
        assertTrue(first.isAdmin());
        assertTrue(manager.verifyLogin("pat", "Alpha1"));

        UserAccount second = manager.createUser("agent_7", false);
        assertFalse(second.isAdmin());
        assertFalse(manager.passwordExists("agent_7"));
        assertTrue(manager.setInitialPassword("agent_7", "Bravo2"));
        assertTrue(manager.verifyLogin("agent_7", "Bravo2"));
        assertFalse(manager.verifyLogin("agent_7", "Alpha1"));

        assertTrue(Files.exists(directory.resolve("credentials-706174.cip")));
        assertTrue(Files.exists(directory.resolve("credentials-6167656e745f37.cip")));
    }

    @Test
    void passwordChangesRemainIndependentAcrossSessionsAndCaseVariants() throws Exception {
        manager.createFirstUser("Agent", "Alpha1");
        manager.createUser("agent", false);
        assertTrue(manager.createCredentials("agent", "Bravo2"));
        assertTrue(manager.changePassword("Agent", "Charlie3"));
        assertFalse(manager.verifyLogin("Agent", "Alpha1"));
        assertTrue(manager.verifyLogin("Agent", "Charlie3"));
        assertTrue(manager.verifyLogin("agent", "Bravo2"));
        assertFalse(manager.verifyLogin("agent", "Charlie3"));
        assertFalse(manager.createCredentials("agent", "Delta4"));
        assertFalse(manager.verifyLogin("unknown", "Bravo2"));
        String original = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        Cipher cipher = new Cipher(new CipherKey(original + "\n"
                + "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a"));
        var reopened = new UserAccountManager(new DatabaseManager("jdbc:sqlite:" + database),
                new FileHandler(directory.toString()), cipher);
        assertTrue(reopened.verifyLogin("Agent", "Charlie3"));
        assertTrue(reopened.verifyLogin("agent", "Bravo2"));
        try (var files = Files.list(directory)) {
            var paths = files.toList();
            assertEquals(2, paths.size());
            for (Path path : paths) {
                String contents = Files.readString(path);
                assertFalse(contents.contains("Charlie3"));
                assertFalse(contents.contains("Bravo2"));
            }
        }
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement();
             var columns = statement.executeQuery("PRAGMA table_info(users)")) {
            var names = new java.util.ArrayList<String>();
            while (columns.next()) names.add(columns.getString("name"));
            assertEquals(java.util.List.of("username", "is_admin"), names);
        }
        byte[] data = Files.readAllBytes(database);
        String raw = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        for (String secret : java.util.List.of("Alpha1", "Bravo2", "Charlie3", "PWD:")) {
            assertFalse(raw.contains(secret), "Password data must stay outside SQLite");
        }
    }

    @Test
    void adminMetadataIsSharedWithAuthorizationConsumers() {
        manager.createFirstUser("admin", "Alpha1");
        manager.createUser("agent_7", false);
        var store = new SqliteUserAccountStore("jdbc:sqlite:" + database);
        assertTrue(store.setAdmin("agent_7", true));
        assertTrue(manager.getUser("agent_7").isAdmin());
        assertTrue(store.setAdmin("agent_7", false));
        assertFalse(manager.getUser("agent_7").isAdmin());
    }
}
