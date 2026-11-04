package org.view;

import org.example.AuditLogService;
import org.example.UserAccount;
import org.example.UserAccountManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.model.Agent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgencyInterfaceAgentManagerTest {

    @TempDir
    Path tempDir;

    private AgentManager agentManager;
    private FacilityManager facilityManager;
    private BriefManager briefManager;
    private UserAccountManager userManager;
    private AuditLogService auditLog;

    private ByteArrayOutputStream output;

    private final UserAccount admin = new UserAccount("pat", true);

    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setup() {
        String url = "jdbc:sqlite:" + tempDir.resolve("agents.db").toAbsolutePath();

        // Real managers sharing the same SQLite database
        facilityManager = new FacilityManager(url);
        agentManager = new AgentManager(url);

        // Mock unrelated dependencies
        briefManager = mock(BriefManager.class);
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

    private int createFacility() {
        return facilityManager
                .addRecord("Blackwood Annex", "BA")
                .getId();
    }

    @Test
    void listsAgentsFromSQLite() {
        int facility1 = facilityManager.addRecord("Blackwood Annex", "BA").getId();
        int facility2 = facilityManager.addRecord("Rose Garden", "RG").getId();

        agentManager.addRecord("Vanguard", "1985-06-14", null, null, facility1);
        agentManager.addRecord("Ghost", "1992-11-22", null, null, facility2);

        String text = run(standard, "2\n1\n0\n0\n");

        assertTrue(text.contains("Vanguard"));
        assertTrue(text.contains("Ghost"));
    }

    @Test
    void readsAgentFromSQLiteAndLogsAction() {
        int facilityId = createFacility();

        Agent agent = agentManager.addRecord("Falcon", "1982-01-30", "2020-03-01", "Retired.", facilityId);

        String text = run(standard, "2\n2\n" + agent.getId() + "\n0\n0\n");

        assertTrue(text.contains("Name: Falcon"));
        assertTrue(text.contains("Date of birth: 1982-01-30"));
        assertTrue(text.contains("Date of death: 2020-03-01"));
        assertTrue(text.contains("Notes: Retired."));
        assertTrue(text.contains("Facility ID: " + facilityId));

        verify(auditLog).log("agent_7", "READ", "AGENT", String.valueOf(agent.getId()));
    }

    @Test
    void addingAgentThroughUIActuallyPersistsToSQLite() {
        int facilityId = createFacility();

        String text = run(
                admin,
                "2\n3\nJackal\n1991-02-11\n"
                        + "2024-01-01\nDouble agent.\n"
                        + facilityId + "\n0\n0\n"
        );

        assertTrue(text.contains("Added"));

        List<Agent> stored = agentManager.getRecords("name", "Jackal");

        assertEquals(1, stored.size());
        assertEquals("Jackal", stored.get(0).getName());
        assertEquals("1991-02-11", stored.get(0).getDateOfBirth());
        assertEquals("2024-01-01", stored.get(0).getDateOfDeath());
        assertEquals("Double agent.", stored.get(0).getNotes());
        assertEquals(facilityId, stored.get(0).getFacilityId());

        verify(auditLog).log("pat", "CREATE", "AGENT", String.valueOf(stored.get(0).getId()));
    }

    @Test
    void newlyAddedAgentAppearsInList() {
        int facilityId = createFacility();

        String text = run(
                admin,
                "2\n3\nJackal\n1991-02-11\n\n\n"
                        + facilityId + "\n1\n0\n0\n"
        );

        assertTrue(text.contains("Jackal"));
    }

    @Test
    void invalidFacilityIdDoesNotPersistAgent() {
        String text = run(
                admin,
                "2\n3\nOrphan\n1990-01-01\n\n\n999\n0\n0\n"
        );

        assertTrue(text.contains("Unable to add agent"));
        assertTrue(agentManager.getRecords("name", "Orphan").isEmpty());

        verify(auditLog, never()).log(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void emptyDatabaseDisplaysAppropriateMessage() {
        String text = run(standard, "2\n1\n0\n0\n");

        assertTrue(text.contains("No agents"));
    }

    @Test
    void standardUserCannotAddAgent() {
        String text = run(standard, "2\n3\n0\n0\n");

        assertTrue(text.contains("Invalid selection"));
        assertTrue(agentManager.getRecords("id", "%").isEmpty());

        verify(auditLog, never()).log(anyString(), anyString(), anyString(), anyString());
    }
}