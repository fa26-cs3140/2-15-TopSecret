package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.model.Brief;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgencyInterfaceBriefManagerTest {

    @TempDir
    Path tempDir;

    private BriefManager briefManager;
    private AgentManager agentManager;
    private FacilityManager facilityManager;

    private UserAccountManager userManager;
    private AuditLogService auditLog;

    private ByteArrayOutputStream output;

    private final UserAccount admin =
            new UserAccount("pat", true);

    private final UserAccount standard =
            new UserAccount("agent_7", false);

    @BeforeEach
    void setup() {
        String url = "jdbc:sqlite:" + tempDir.resolve("briefs.db").toAbsolutePath();

        // Real database manager under test
        briefManager = new BriefManager(url);

        // Unrelated dependencies are mocked
        agentManager = mock(AgentManager.class);
        facilityManager = mock(FacilityManager.class);
        userManager = mock(UserAccountManager.class);
        auditLog = mock(AuditLogService.class);

        output = new ByteArrayOutputStream();
    }

    private String run(UserAccount user, String input) {
        output.reset();

        AgencyInterface ui = new AgencyInterface(
                agentManager,
                facilityManager,
                briefManager,
                userManager,
                auditLog,
                user,
                new Scanner(input),
                new PrintStream(output)
        );

        ui.run();
        return output.toString();
    }

    @Test
    void listsBriefsFromSQLite() {
        briefManager.addRecord("Operation Sandtrap", "1970-11-03", "Bug the diplomatic pouch.");

        briefManager.addRecord("The Munich Lead", "1972-09-15", "Identify the cell.");

        String text = run(standard, "1\n1\n0\n0\n");

        assertTrue(text.contains("Operation Sandtrap"));
        assertTrue(text.contains("The Munich Lead"));
    }

    @Test
    void readsBriefFromSQLiteAndLogsAction() {
        Brief brief = briefManager.addRecord("Operation Sandtrap", "1970-11-03", "Bug the diplomatic pouch.");

        String text = run(standard, "1\n2\n" + brief.getId() + "\n0\n0\n");

        assertTrue(text.contains("Title: Operation Sandtrap"));
        assertTrue(text.contains("Date: 1970-11-03"));
        assertTrue(text.contains("Text: Bug the diplomatic pouch."));

        verify(auditLog).log("agent_7", "READ", "BRIEF", String.valueOf(brief.getId()));
    }

    @Test
    void addingBriefThroughUIActuallyPersistsToSQLite() {
        String text = run(admin, "1\n3\nOperation Nightfall\n" + "1980-05-15\nNew report.\n0\n0\n");

        assertTrue(text.contains("Added brief"));

        List<Brief> stored = briefManager.getRecords("title", "Operation Nightfall");

        assertEquals(1, stored.size());
        assertEquals("Operation Nightfall", stored.get(0).getTitle());
        assertEquals("1980-05-15", stored.get(0).getDate());
        assertEquals("New report.", stored.get(0).getText());

        verify(auditLog).log("pat", "CREATE", "BRIEF", String.valueOf(stored.get(0).getId()));
    }

    @Test
    void newlyAddedBriefAppearsInList() {
        String text = run(admin, "1\n3\nOperation Nightfall\n" + "1980-05-15\nNew report.\n" + "1\n0\n0\n");

        assertTrue(text.contains("Operation Nightfall"));
    }

    @Test
    void invalidDateDoesNotPersistBrief() {
        String text = run(admin, "1\n3\nBad Brief\n" + "November 3, 1970\nSome text\n0\n0\n");

        assertTrue(text.contains("Unable to add brief"));

        assertTrue(briefManager.getRecords("title", "Bad Brief").isEmpty());

        verify(auditLog, never()).log(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void emptyDatabaseDisplaysAppropriateMessage() {
        String text = run(standard, "1\n1\n0\n0\n");

        assertTrue(text.contains("No briefs"));
    }

    @Test
    void standardUserCannotAddBrief() {
        String text = run(standard, "1\n3\n0\n0\n");

        assertTrue(text.contains("Invalid selection"));
        assertTrue(briefManager.getRecords("id", "%").isEmpty());

        verify(auditLog, never()).log(anyString(), anyString(), anyString(), anyString());
    }
}
