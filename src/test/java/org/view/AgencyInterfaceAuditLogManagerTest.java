package org.view;

import org.managers.AuditLogManager;
import org.managers.UserAccountManager;
import org.model.AuditLogEntry;
import org.model.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgencyInterfaceAuditLogManagerTest {

    @TempDir
    Path tempDir;

    private AgentManager agentManager;
    private FacilityManager facilityManager;
    private BriefManager briefManager;
    private UserAccountManager userManager;
    private AuditLogManager auditLogManager;
    private ByteArrayOutputStream output;

    private final UserAccount admin =
            new UserAccount("pat", true);

    private final UserAccount standard =
            new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:sqlite:"
                + tempDir.resolve("audit.db").toAbsolutePath();

        facilityManager = new FacilityManager(url);
        agentManager = new AgentManager(url);
        briefManager = new BriefManager(url);
        this.createTables();

        auditLogManager = new AuditLogManager(url);
        userManager = mock(UserAccountManager.class);

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // The audit-log store creates the users table.
            stmt.execute("""
                INSERT INTO users (username, is_admin)
                VALUES ('pat', 1)
                """);

            stmt.execute("""
                INSERT INTO users (username, is_admin)
                VALUES ('agent_7', 0)
                """);

            stmt.execute("""
                INSERT INTO facilities (id, name, abbreviation)
                VALUES (1, 'Blackwood Annex', 'BA')
                """);

            stmt.execute("""
                INSERT INTO briefs
                (id, title, date, text, mission_id)
                VALUES
                (1, 'Operation Sandtrap', '1970-11-03',
                 'Bug the diplomatic pouch.', NULL)
                """);
        }

        output = new ByteArrayOutputStream();
    }

    private void createTables()
    {
        facilityManager.createTable();
        briefManager.createTable();
        agentManager.createTable();
    }

    private String run(UserAccount user, String input) {
        new AgencyInterface(
                agentManager,
                facilityManager,
                briefManager,
                userManager,
                mock(MissionManager.class),
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

        List<AuditLogEntry> entries =
                auditLogManager.getAuditLog().stream().filter(e -> !"LOGOUT".equals(e.getAction())).toList();

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

        List<AuditLogEntry> entries =
                auditLogManager.getAuditLog().stream().filter(e -> !"LOGOUT".equals(e.getAction())).toList();

        assertEquals(1, entries.size());
        assertEquals("pat", entries.get(0).getUsername());
        assertEquals("CREATE", entries.get(0).getAction());
        assertEquals("FACILITY", entries.get(0).getContentType());
    }

    @Test
    void failedAddIsNotStoredInAuditLog() {
        run(
                admin,
                "2\n3\nOrphan\n1990-01-01\n\n\n99\n0\n0\n"
        );

        assertTrue(auditLogManager.getAuditLog().stream().noneMatch(e -> !"LOGOUT".equals(e.getAction())));
    }

    @Test
    void adminSeesOperationsFromEarlierSessionsInAuditLog() {
        run(standard, "1\n2\n1\n0\n0\n");

        output.reset();

        // Audit-log review is menu option 5, not 4.
        String text = run(admin, "5\n0\n");

        assertTrue(text.contains("agent_7"));
        assertTrue(text.contains("READ"));
        assertTrue(text.contains("BRIEF"));
    }

    @Test
    void emptyAuditLogShowsMessage() {
        String text = run(admin, "5\n0\n");

        assertTrue(text.contains("No audit entries"));
    }
    @Test
    void normalExitRecordsExactlyOneLogout() {
        run(standard, "0\n");
        var entries = auditLogManager.getAuditLog();
        assertEquals(1, entries.size());
        assertEquals("LOGOUT", entries.get(0).getAction());
        assertEquals("agent_7", entries.get(0).getUsername());
    }
}
