package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogManagerDatabaseManagerTest {

    private Path database;
    private AuditLogManager manager;

    @BeforeEach
    void setUp() throws Exception {
        database = Files.createTempFile("topsecret-audit-manager", ".db");
        String url = "jdbc:sqlite:" + database.toAbsolutePath();

        SqliteUserAccountStore users = new SqliteUserAccountStore(url);
        users.addUser(new UserAccount("agent_7", false));
        manager = new AuditLogManager(new DatabaseManager(url));
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(database);
    }

    @Test
    void auditEventsPersistAcrossManagerInstances() {
        manager.log("agent_7", "READ", "BRIEF", "1");
        manager.log("agent_7", "CREATE", "FACILITY", "3");

        AuditLogManager secondManager = new AuditLogManager(
                new DatabaseManager("jdbc:sqlite:" + database.toAbsolutePath())
        );

        List<AuditLogEntry> entries = secondManager.getAuditLog();
        assertEquals(2, entries.size());
        assertEquals("READ", entries.get(0).getAction());
        assertEquals("CREATE", entries.get(1).getAction());
        assertNotNull(entries.get(0).getTimestamp());
    }
}
