package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqliteUserAccountStoreTest {

    private Path database;
    private SqliteUserAccountStore store;

    @BeforeEach
    void setUp() throws Exception {
        database = Files.createTempFile("topsecret-users", ".db");
        store = new SqliteUserAccountStore("jdbc:sqlite:" + database.toAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(database);
    }

    @Test
    void constructorRejectsBlankUrlAndReturnsUrl() {
        assertThrows(IllegalArgumentException.class, () -> new SqliteUserAccountStore(null));
        assertThrows(IllegalArgumentException.class, () -> new SqliteUserAccountStore(" "));
        assertEquals("jdbc:sqlite:" + database.toAbsolutePath(), store.getUrl());
    }

    @Test
    void userCrudMethodsWork() {
        assertFalse(store.hasUsers());

        UserAccount agent = new UserAccount("agent_7", false);
        assertTrue(store.addUser(agent));
        assertTrue(store.hasUsers());
        assertFalse(store.addUser(agent));

        UserAccount loaded = store.getUser("agent_7");
        assertNotNull(loaded);
        assertEquals("agent_7", loaded.getUsername());
        assertFalse(loaded.isAdmin());
        assertNull(store.getUser("missing"));

        assertEquals(1, store.getUsers().size());
        assertTrue(store.setAdmin("agent_7", true));
        assertTrue(store.getUser("agent_7").isAdmin());
        assertFalse(store.setAdmin("missing", true));
    }

    @Test
    void addUserRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> store.addUser(null));
    }

    @Test
    void failedReadsCannotTriggerFirstAdminBootstrap() throws Exception {
        try (var connection = java.sql.DriverManager.getConnection(store.getUrl());
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE users");
        }
        assertThrows(IllegalStateException.class, () -> store.hasUsers());
        assertThrows(IllegalStateException.class, () -> store.getUser("agent_7"));
        assertThrows(IllegalStateException.class, () -> store.getUsers());
        assertThrows(IllegalStateException.class, () -> store.setAdmin("agent_7", true));
    }
}
