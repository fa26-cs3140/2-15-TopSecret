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
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for AgencyInterface with the real DatabaseManager.
 *
 * Uses a temporary SQLite database. The audit log is mocked here;
 * AgencyInterfaceAuditLogManagerTest covers the real audit log.
 */
class AgencyInterfaceDatabaseManagerTest {

    private Path testDatabasePath;
    private String url;
    private DatabaseManager dataService;
    private ByteArrayOutputStream output;

    private final UserAccount admin = new UserAccount("pat", true);
    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() throws Exception {
        testDatabasePath = Files.createTempFile("topsecret-agency-test", ".db");
        url = "jdbc:sqlite:" + testDatabasePath.toAbsolutePath();

        // The service creates its tables if they are missing.
        dataService = new DatabaseManager(url);

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO facilities (id, name, abbreviation) VALUES (1, 'Blackwood Annex', 'BA')");
            statement.execute("INSERT INTO facilities (id, name, abbreviation) VALUES (2, 'Rose Garden', 'RG')");
            statement.execute("INSERT INTO agents (id, name, date_of_birth, date_of_death, notes, facility_id) "
                    + "VALUES (1, 'Vanguard', '1985-06-14', NULL, NULL, 1)");
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
                mock(AuditLogService.class),
                user,
                new Scanner(input),
                new PrintStream(output)
        ).run();
        return output.toString();
    }

    @Test
    void constructorStoresUrl() {
        assertEquals(url, dataService.getUrl());
    }

    @Test
    void constructorRejectsBlankUrl() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseManager(null));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseManager("  "));
    }

    @Test
    void listsStoredBriefs() {
        String text = run(standard, "1\n1\n0\n0\n");

        assertTrue(text.contains("1. Operation Sandtrap"));
    }

    @Test
    void readsStoredAgent() {
        String text = run(standard, "2\n2\n1\n0\n0\n");

        assertTrue(text.contains("Name: Vanguard"));
        assertTrue(text.contains("Date of birth: 1985-06-14"));
        assertTrue(text.contains("Facility ID: 1"));
    }

    @Test
    void listsStoredFacilities() {
        String text = run(standard, "3\n1\n0\n0\n");

        assertTrue(text.contains("Blackwood Annex (BA)"));
        assertTrue(text.contains("Rose Garden (RG)"));
    }

    @Test
    void addedBriefIsStoredAndImmediatelyListed() {
        String text = run(admin, "1\n3\nOperation Nightfall\n1980-05-15\nNew report.\n1\n0\n0\n");

        assertTrue(text.contains("Operation Nightfall"));
        assertTrue(dataService.getBriefs().stream()
                .anyMatch(b -> b.getTitle().equals("Operation Nightfall")));
    }

    @Test
    void addedAgentIsStoredAndImmediatelyListed() {
        String text = run(admin, "2\n3\nJackal\n1991-02-11\n\n\n2\n1\n0\n0\n");

        assertTrue(text.contains("Jackal"));
        assertTrue(dataService.getAgents().stream()
                .anyMatch(a -> a.getName().equals("Jackal") && a.getFacilityId() == 2));
    }

    @Test
    void addedFacilityIsStoredAndImmediatelyListed() {
        String text = run(admin, "3\n3\nFairbanks Point\nFP\n1\n0\n0\n");

        assertTrue(text.contains("Fairbanks Point (FP)"));
        assertEquals(3, dataService.getFacilities().size());
    }

    @Test
    void agentWithMissingFacilityIsRejectedAndNotStored() {
        String text = run(admin, "2\n3\nOrphan\n1990-01-01\n\n\n99\n0\n0\n");

        assertTrue(text.contains("Unable to add agent"));
        assertEquals(1, dataService.getAgents().size());
    }

    @Test
    void briefWithInvalidDateIsRejectedAndNotStored() {
        String text = run(admin, "1\n3\nBad Brief\nNovember 3, 1970\nText\n0\n0\n");

        assertTrue(text.contains("Unable to add brief"));
        assertEquals(1, dataService.getBriefs().size());
    }

    @Test
    void worksOnCleanDatabaseWithNoRecords() throws Exception {
        Path emptyPath = Files.createTempFile("topsecret-agency-empty", ".db");
        try {
            AgencyDataService emptyService =
                    new DatabaseManager("jdbc:sqlite:" + emptyPath.toAbsolutePath());

            new AgencyInterface(emptyService, mock(AuditLogService.class), standard,
                    new Scanner("1\n1\n0\n2\n1\n0\n3\n1\n0\n0\n"), new PrintStream(output)).run();

            String text = output.toString();
            assertTrue(text.contains("No briefs"));
            assertTrue(text.contains("No agents"));
            assertTrue(text.contains("No facilities"));
        } finally {
            Files.deleteIfExists(emptyPath);
        }
    }
}
