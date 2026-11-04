package org.view;

import org.model.AuditLogService;
import org.model.UserAccount;
import org.model.UserAccountManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.model.Facility;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgencyInterfaceFacilityManagerTest {

    @TempDir
    Path tempDir;

    private FacilityManager facilityManager;
    private AgentManager agentManager;
    private BriefManager briefManager;
    private UserAccountManager userManager;
    private AuditLogService auditLog;

    private ByteArrayOutputStream output;

    private final UserAccount admin = new UserAccount("pat", true);

    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + tempDir.resolve("facilities.db").toAbsolutePath();

        // Real FacilityManager connected to temporary SQLite
        facilityManager = new FacilityManager(url);

        // Other dependencies are mocked
        agentManager = mock(AgentManager.class);
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

    @Test
    void listsFacilitiesFromSQLite() {
        facilityManager.addRecord("Blackwood Annex", "BA");
        facilityManager.addRecord("Rose Garden", "RG");

        String text = run(standard, "3\n1\n0\n0\n");

        assertTrue(text.contains("Blackwood Annex (BA)"));
        assertTrue(text.contains("Rose Garden (RG)"));
    }

    @Test
    void readsFacilityFromSQLiteAndLogsAction() {
        Facility facility = facilityManager.addRecord("Meridian Observatory", "MO");

        String text = run(
                standard,
                "3\n2\n" + facility.getId() + "\n0\n0\n"
        );

        assertTrue(text.contains("Name: Meridian Observatory"));
        assertTrue(text.contains("Abbreviation: MO"));

        verify(auditLog).log("agent_7", "READ", "FACILITY", String.valueOf(facility.getId()));
    }

    @Test
    void addingFacilityThroughUIActuallyPersistsToSQLite() {
        String text = run(
                admin,
                "3\n3\nFairbanks Point\nFP\n0\n0\n"
        );

        assertTrue(text.contains("Added"));

        List<Facility> stored = facilityManager.getRecords("name", "Fairbanks Point");

        assertEquals(1, stored.size());
        assertEquals("Fairbanks Point", stored.get(0).getName());
        assertEquals("FP", stored.get(0).getAbbreviation());

        verify(auditLog).log("pat", "CREATE", "FACILITY", String.valueOf(stored.get(0).getId()));
    }

    @Test
    void newlyAddedFacilityAppearsInList() {
        String text = run(
                admin,
                "3\n3\nFairbanks Point\nFP\n1\n0\n0\n"
        );

        assertTrue(text.contains("Fairbanks Point (FP)"));
    }

    @Test
    void invalidFacilityDataDoesNotPersist() {
        String text = run(
                admin,
                "3\n3\n\nFP\n0\n0\n"
        );

        assertTrue(text.contains("Unable to add facility"));

        assertTrue(facilityManager.getRecords("name", "").isEmpty());

        verify(auditLog, never()).log(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void emptyDatabaseDisplaysAppropriateMessage() {
        String text = run(standard, "3\n1\n0\n0\n");

        assertTrue(text.contains("No facilities"));
    }

    @Test
    void standardUserCannotAddFacility() {
        String text = run(standard, "3\n3\n0\n0\n");

        assertTrue(text.contains("Invalid selection"));

        assertTrue(facilityManager.getRecords("id", "%").isEmpty());

        verify(auditLog, never()).log(anyString(), anyString(), anyString(), anyString());
    }
}
