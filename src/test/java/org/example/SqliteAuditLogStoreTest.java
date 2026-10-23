package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SqliteAuditLogStoreTest {

    private Path database;
    private SqliteAuditLogStore store;
    private SqliteUserAccountStore users;

    @BeforeEach
    void setUp() throws Exception {
        database = Files.createTempFile("topsecret-audit", ".db");
        String url = "jdbc:sqlite:" + database.toAbsolutePath();
        users = new SqliteUserAccountStore(url);
        store = new SqliteAuditLogStore(url);
        users.addUser(new UserAccount("agent_7", false));
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(database);
    }

    @Test
    void constructorRejectsBlankUrlAndReturnsUrl() {
        assertThrows(IllegalArgumentException.class, () -> new SqliteAuditLogStore(null));
        assertThrows(IllegalArgumentException.class, () -> new SqliteAuditLogStore(" "));
        assertEquals("jdbc:sqlite:" + database.toAbsolutePath(), store.getUrl());
    }

    @Test
    void addAndGetAllPersistAuditEntry() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 22, 20, 15);
        store.add(new AuditLogEntry(0, "agent_7", "READ", "BRIEF", "1", timestamp));

        assertEquals(1, store.getAll().size());
        AuditLogEntry entry = store.getAll().get(0);
        assertTrue(entry.getId() > 0);
        assertEquals("agent_7", entry.getUsername());
        assertEquals("READ", entry.getAction());
        assertEquals("BRIEF", entry.getContentType());
        assertEquals("1", entry.getContentIdentifier());
        assertEquals(timestamp, entry.getTimestamp());
    }

    @Test
    void addRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> store.add(null));
    }

    @Test
    void rejectsUnknownUsersAndRetainsOrderedHistoryOnReopen() {
        var time = LocalDateTime.now();
        assertThrows(IllegalStateException.class, () -> store.add(
                new AuditLogEntry(0, "unknown", "READ", "AGENT", "7", time)));
        store.add(new AuditLogEntry(0, "agent_7", "READ", "AGENT", "7", time));
        store.add(new AuditLogEntry(0, "agent_7", "CREATE", "FACILITY", "8", time));
        var reopened = new SqliteAuditLogStore(store.getUrl());
        var entries = reopened.getAll();
        assertEquals(2, entries.size());
        assertTrue(entries.get(0).getId() < entries.get(1).getId());
        assertEquals("CREATE", entries.get(1).getAction());
        assertEquals("FACILITY", entries.get(1).getContentType());
        assertEquals("8", entries.get(1).getContentIdentifier());
        assertEquals(time, entries.get(1).getTimestamp());
    }

    @Test
    void failedReadsAreReportedRatherThanReturnedAsEmptyHistory() throws Exception {
        try (var connection = java.sql.DriverManager.getConnection(store.getUrl());
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE audit_log");
        }
        assertThrows(IllegalStateException.class, () -> store.getAll());
    }
}
