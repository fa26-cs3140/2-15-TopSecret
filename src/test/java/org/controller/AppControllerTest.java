package org.controller;

import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.model.AuditLogEntry;
import org.managers.AuditLogService;
import org.model.RecordType;
import org.model.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.model.Agent;
import org.model.Brief;
import org.model.Facility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    @Mock
    private BriefManager briefManager;

    @Mock
    private AgentManager agentManager;

    @Mock
    private FacilityManager facilityManager;

    @Mock 
    private MissionManager missionManager;

    @Mock
    private AuditLogService auditLog;

    private AppController controller;

    private final UserAccount admin = new UserAccount("pat", true);
    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() {
        controller = new AppController(briefManager, agentManager, missionManager, facilityManager, auditLog);
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> new AppController(null, agentManager, missionManager, facilityManager, auditLog));
        assertThrows(IllegalArgumentException.class,
                () -> new AppController(briefManager, null, missionManager,  facilityManager, auditLog));
        assertThrows(IllegalArgumentException.class,
                () -> new AppController(briefManager, agentManager, missionManager, null, auditLog));
        assertThrows(IllegalArgumentException.class,
                () -> new AppController(briefManager, agentManager, missionManager, facilityManager, null));
    }

    @Test
    void listBriefsShowsIdsAndTitles() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null),
                new Brief(2, "The Munich Lead", "1972-09-15", "Find the cell.", null)));

        String text = controller.listRecords(RecordType.BRIEF);

        assertTrue(text.contains("1. Operation Sandtrap"));
        assertTrue(text.contains("2. The Munich Lead"));
    }

    @Test
    void listFacilitiesShowsAbbreviations() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));

        assertTrue(controller.listRecords(RecordType.FACILITY).contains("1. Blackwood Annex (BA)"));
    }

    @Test
    void emptyListShowsMessage() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of());

        assertTrue(controller.listRecords(RecordType.AGENT).contains("No agents are available."));
    }

    @Test
    void readRecordShowsDetailsAndIsAudited() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of(
                new Agent(4, "Jackal", "1991-02-11", null, null, 1)));

        String text = controller.readRecord(RecordType.AGENT, 4, standard);

        assertTrue(text.contains("Name: Jackal"));
        assertTrue(text.contains("Date of death: N/A"));
        verify(auditLog).log("agent_7", "READ", "AGENT", "4");
    }

    @Test
    void readUnknownRecordIsNotAudited() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of());

        assertTrue(controller.readRecord(RecordType.BRIEF, 99, standard).contains("No brief found with ID 99."));
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    @Test
    void adminSeesAuditLog() {
        when(auditLog.getAuditLog()).thenReturn(List.of(
                new AuditLogEntry(1, "pat", "READ", "BRIEF", "2", LocalDateTime.of(2026, 11, 2, 9, 30))));

        String text = controller.auditLog(admin);

        assertTrue(text.contains("pat"));
        assertTrue(text.contains("READ"));
        assertTrue(text.contains("BRIEF 2"));
    }

    @Test
    void emptyAuditLogShowsMessage() {
        when(auditLog.getAuditLog()).thenReturn(List.of());

        assertTrue(controller.auditLog(admin).contains("No audit entries have been recorded."));
    }

    @Test
    void standardUserCannotSeeAuditLog() {
        assertTrue(controller.auditLog(standard).contains("Access denied"));
        verify(auditLog, never()).getAuditLog();
    }

    // ---- HW7 editing ----

    @Test
    void onlyAdminsCanEdit() {
        assertTrue(controller.canEdit(admin));
        assertFalse(controller.canEdit(standard));
        assertFalse(controller.canEdit(null));
    }

    @Test
    void standardUserCannotCreateOrUpdate() {
        Map<String, String> values = Map.of("title", "Op", "date", "2026-01-01", "text", "Go");

        assertThrows(SecurityException.class, () -> controller.createRecord(RecordType.BRIEF, values, standard));
        assertThrows(SecurityException.class, () -> controller.updateRecord(RecordType.BRIEF, 1, values, standard));
        assertThrows(SecurityException.class, () -> controller.createRecord(RecordType.BRIEF, values, null));
        verifyNoInteractions(briefManager, auditLog);
    }

    // Drafted in week 1; enabled in week 2. Manager calls may change to Team D's create/update API.
    @Test
    @Disabled("HW7 week 2: createRecord not implemented yet")
    void adminCreatesBriefAndLogsCreate() {
        when(briefManager.addRecord("Op Nightfall", "2026-01-01", "Watch the port."))
                .thenReturn(new Brief(101, "Op Nightfall", "2026-01-01", "Watch the port.", null));

        int id = controller.createRecord(RecordType.BRIEF,
                Map.of("title", "Op Nightfall", "date", "2026-01-01", "text", "Watch the port."), admin);

        assertEquals(101, id);
        verify(auditLog).log("pat", "CREATE", "BRIEF", "101");
    }

    @Test
    @Disabled("HW7 week 2: createRecord not implemented yet")
    void createRejectsMissingRequiredField() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.createRecord(RecordType.BRIEF, Map.of("title", "Op", "text", "Go"), admin));
        verifyNoInteractions(auditLog);
    }

    @Test
    @Disabled("HW7 week 2: updateRecord not implemented yet")
    void adminUpdatesAgentAndLogsUpdate() {
        when(agentManager.getRecords("id", "%"))
                .thenReturn(List.of(new Agent(2, "Ghost", "1992-11-22", null, null, 2)));

        controller.updateRecord(RecordType.AGENT, 2, Map.of("name", "Ghost", "dateOfBirth", "1992-11-22",
                "dateOfDeath", "", "notes", "Back in the field", "facilityId", "3"), admin);

        verify(auditLog).log("pat", "UPDATE", "AGENT", "2");
    }

    @Test
    @Disabled("HW7 week 2: updateRecord not implemented yet")
    void updateRejectsUnknownId() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> controller.updateRecord(RecordType.AGENT, 99,
                Map.of("name", "X", "dateOfBirth", "1990-01-01", "facilityId", "1"), admin));
    }

    @Test
    @Disabled("HW7 week 2: formValues not implemented yet")
    void formValuesReturnCurrentRecordFields() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));

        assertEquals(Map.of("name", "Blackwood Annex", "abbreviation", "BA"),
                controller.formValues(RecordType.FACILITY, 1));
        assertNull(controller.formValues(RecordType.FACILITY, 9));
    }
}
