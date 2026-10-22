package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AgencyInterface with Team D's real AuditLogManager
 * and the real SqliteAgencyDataService, sharing one temporary SQLite database.
 */
class AgencyInterfaceAuditLogManagerTest {

    private Path testDatabasePath;
    private SqliteAgencyDataService dataService;
    private AuditLogManager auditLogManager;
    private ByteArrayOutputStream output;

    private final UserAccount admin = new UserAccount("pat", true);
    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() throws Exception {
        testDatabasePath = Files.createTempFile("topsecret-audit-ui-test", ".db");
        String url = "jdbc:sqlite:" + testDatabasePath.toAbsolutePath();

        dataService = new SqliteAgencyDataService(url);
        auditLogManager = new AuditLogManager(new DatabaseManager(url));

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            // audit_log.username references users.username
            statement.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "username TEXT PRIMARY KEY, is_admin INTEGER NOT NULL DEFAULT 0)");
            statement.execute("INSERT INTO users (username, is_admin) VALUES ('pat', 1)");
            statement.execute("INSERT INTO users (username, is_admin) VALUES ('agent_7', 0)");
            statement.execute("INSERT INTO facilities (id, name, abbreviation) VALUES (1, 'Blackwood Annex', 'BA')");
            statement.execute("INSERT INTO briefs (id, title, date, text, mission_id) "
                    + "VALUES (1, 'Operation Sandtrap', '1970-11-03', 'Bug the diplomatic pouch.', NULL)");
        }

        output = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(testDatabasePath);
    }

    private String run(UserAccount user, String input) {
        new AgencyInterface(
                dataService,
                auditLogManager,
                user,
                new Scanner(input),
                new PrintStream(output)
        ).run();
        return output.toString();
    }

    @Test
    void readingABriefIsStoredInAuditLog() {
        run(standard, "1\n2\n1\n0\n0\n");

        List<AuditLogEntry> entries = auditLogManager.getAuditLog();
        assertEquals(1, entries.size());
        AuditLogEntry entry = entries.get(0);
        assertEquals("agent_7", entry.getUsername());
        assertEquals("READ", entry.getAction());
        assertEquals("BRIEF", entry.getContentType());
        assertEquals("1", entry.getContentIdentifier());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void addingAFacilityIsStoredInAuditLog() {
        run(admin, "3\n3\nFairbanks Point\nFP\n0\n0\n");

        List<AuditLogEntry> entries = auditLogManager.getAuditLog();
        assertEquals(1, entries.size());
        assertEquals("pat", entries.get(0).getUsername());
        assertEquals("CREATE", entries.get(0).getAction());
        assertEquals("FACILITY", entries.get(0).getContentType());
    }

    @Test
    void failedAddIsNotStoredInAuditLog() {
        run(admin, "2\n3\nOrphan\n1990-01-01\n\n\n99\n0\n0\n");

        assertTrue(auditLogManager.getAuditLog().isEmpty());
    }

    @Test
    void adminSeesOperationsFromEarlierSessionsInAuditLog() {
        run(standard, "1\n2\n1\n0\n0\n");
        output.reset();

        String text = run(admin, "4\n0\n");

        assertTrue(text.contains("agent_7"));
        assertTrue(text.contains("READ"));
        assertTrue(text.contains("BRIEF"));
    }

    @Test
    void emptyAuditLogShowsMessage() {
        String text = run(admin, "4\n0\n");

        assertTrue(text.contains("No audit entries"));
    }
}
