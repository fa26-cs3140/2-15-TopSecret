package org.controller;

import org.managers.RecordEditor;
import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.model.AuditLogEntry;
import org.managers.AuditLogService;
import org.model.RecordType;
import org.model.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.model.Agent;
import org.model.Brief;
import org.model.Facility;
import org.model.Mission;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
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

    @Mock
    private RecordEditor.Store<Object> changes;

    private AppController controller;

    private final UserAccount admin = new UserAccount("pat", true);
    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() {
        controller = new AppController(briefManager, agentManager, missionManager, facilityManager, auditLog, changes);
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

    @Test
    void adminCreatesBriefAndLogsCreate() {
        when(changes.create(any(Brief.class), any())).thenReturn(101);

        int id = controller.createRecord(RecordType.BRIEF,
                Map.of("title", "Op Nightfall", "date", "2026-01-01", "text", "Watch the port."), admin);

        assertEquals(101, id);
        verify(changes).create(any(), argThat(event -> event.username().equals("pat") && event.action().equals("CREATE")
                && event.type() == RecordType.BRIEF));
    }

    @Test
    void createRejectsMissingRequiredField() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.createRecord(RecordType.BRIEF, Map.of("title", "Op", "text", "Go"), admin));
        verifyNoInteractions(auditLog);
    }

    @Test
    void adminUpdatesAgentAndLogsUpdate() {
        when(changes.update(anyInt(), any(), any())).thenReturn(true);
        when(agentManager.getRecords("id", "%"))
                .thenReturn(List.of(new Agent(2, "Ghost", "1992-11-22", null, null, 2)));
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(3, "Rose Garden", "RG")));

        controller.updateRecord(RecordType.AGENT, 2, Map.of("name", "Ghost", "dateOfBirth", "1992-11-22",
                "dateOfDeath", "", "notes", "Back in the field", "facilityId", "3"), admin);

        verify(changes).update(eq(2), any(), argThat(event -> event.username().equals("pat") && event.action().equals("UPDATE")
                && event.type() == RecordType.AGENT));
    }

    @Test
    void updateRejectsUnknownId() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> controller.updateRecord(RecordType.AGENT, 99,
                Map.of("name", "X", "dateOfBirth", "1990-01-01", "facilityId", "1"), admin));
    }

    @Test
    void formValuesReturnCurrentRecordFields() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));

        assertEquals(Map.of("name", "Blackwood Annex", "abbreviation", "BA"),
                controller.formValues(RecordType.FACILITY, 1));
        assertNull(controller.formValues(RecordType.FACILITY, 9));
    }

    @Test
    void constructorRejectsNullMissionManager() {
        assertThrows(IllegalArgumentException.class,
                () -> new AppController(briefManager, agentManager, null, facilityManager, auditLog));
    }

    @Test
    void adminCreatesMissionWithBriefsAndAgents() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "One", "2020-01-01", "a", null), new Brief(2, "Two", "2020-01-02", "b", null)));
        when(agentManager.getRecords("id", "%")).thenReturn(List.of(new Agent(2, "Ghost", "1992-11-22", null, null, 1)));
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));
        when(changes.create(any(Mission.class), any())).thenReturn(7);

        int id = controller.createRecord(RecordType.MISSION, Map.of("title", " Joint Op ", "facilityId", "1",
                "briefIds", "1, 2, 2,", "agentIds", "2"), admin);

        ArgumentCaptor<Mission> saved = ArgumentCaptor.forClass(Mission.class);
        verify(changes).create(saved.capture(), any());
        assertEquals("Joint Op", saved.getValue().getTitle());
        assertArrayEquals(new int[]{1, 2}, saved.getValue().getBriefIds());
        assertArrayEquals(new int[]{2}, saved.getValue().getAgentIds());
        assertEquals(7, id);
        verify(changes).create(any(), argThat(event -> event.username().equals("pat") && event.action().equals("CREATE")
                && event.type() == RecordType.MISSION));
    }

    @Test
    void createRejectsUnknownOrMalformedIds() {
        assertEquals("No brief found with ID 5.", assertThrows(IllegalArgumentException.class,
                () -> controller.createRecord(RecordType.MISSION, Map.of("title", "Op", "facilityId", "1",
                        "briefIds", "5"), admin)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> controller.createRecord(RecordType.MISSION,
                Map.of("title", "Op", "facilityId", "1", "briefIds", "1, x"), admin));
        assertEquals("No agent found with ID 9.", assertThrows(IllegalArgumentException.class,
                () -> controller.createRecord(RecordType.MISSION, Map.of("title", "Op", "facilityId", "1",
                        "agentIds", "9"), admin)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> controller.createRecord(RecordType.AGENT,
                Map.of("name", "X", "dateOfBirth", "1990-01-01", "facilityId", "abc"), admin));
        assertEquals("Facility ID must be a positive whole number.", assertThrows(IllegalArgumentException.class,
                () -> controller.createRecord(RecordType.AGENT, Map.of("name", "X", "dateOfBirth", "1990-01-01",
                        "facilityId", "0"), admin)).getMessage());
        assertEquals("No facility found with ID 4.", assertThrows(IllegalArgumentException.class,
                () -> controller.createRecord(RecordType.AGENT, Map.of("name", "X", "dateOfBirth", "1990-01-01",
                        "facilityId", "4"), admin)).getMessage());
        verifyNoInteractions(auditLog);
    }

    @Test
    void adminCreatesAgentAndFacility() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));
        when(changes.create(any(Agent.class), any())).thenReturn(11);
        when(changes.create(any(Facility.class), any())).thenReturn(7);

        assertEquals(11, controller.createRecord(RecordType.AGENT, Map.of("name", "Nova", "dateOfBirth", "1999-09-09",
                "dateOfDeath", "", "facilityId", "1"), admin));
        assertEquals(7, controller.createRecord(RecordType.FACILITY, Map.of("name", "Glass House",
                "abbreviation", "GH"), admin));
        verify(changes).create(any(), argThat(event -> event.username().equals("pat") && event.action().equals("CREATE")
                && event.type() == RecordType.AGENT));
        verify(changes).create(any(), argThat(event -> event.username().equals("pat") && event.action().equals("CREATE")
                && event.type() == RecordType.FACILITY));
    }

    @Test
    void adminUpdatesMissionAndReplacesAssignments() {
        when(changes.update(anyInt(), any(), any())).thenReturn(true);
        when(missionManager.getRecords("id", "%")).thenReturn(List.of(new Mission(4, "Op", new int[]{1},
                new int[]{2}, 1)));
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(new Brief(3, "Three", "2020-01-03", "c", null)));
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));

        controller.updateRecord(RecordType.MISSION, 4, Map.of("title", "Op 2", "facilityId", "1", "briefIds", "3",
                "agentIds", ""), admin);

        ArgumentCaptor<Object> updated = ArgumentCaptor.forClass(Object.class);
        verify(changes).update(eq(4), updated.capture(), any());
        Mission saved = (Mission) updated.getValue();
        assertEquals("Op 2", saved.getTitle());
        assertArrayEquals(new int[]{3}, saved.getBriefIds());
        assertArrayEquals(new int[]{}, saved.getAgentIds());
        verify(changes).update(eq(4), any(), argThat(event -> event.username().equals("pat") && event.action().equals("UPDATE")
                && event.type() == RecordType.MISSION));
    }

    @Test
    void adminUpdatesBriefAndFacility() {
        when(changes.update(anyInt(), any(), any())).thenReturn(true);
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(new Brief(1, "Old", "2020-01-01", "a", null)));
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(2, "Rose Garden", "RG")));

        controller.updateRecord(RecordType.BRIEF, 1, Map.of("title", "New", "date", "2021-02-03", "text", "b"), admin);
        controller.updateRecord(RecordType.FACILITY, 2, Map.of("name", "Rose Garden", "abbreviation", "RGN"), admin);

        verify(changes).update(eq(1), any(Brief.class), any());
        verify(changes).update(eq(2), any(Facility.class), any());
        verify(changes).update(eq(1), any(), argThat(event -> event.username().equals("pat") && event.action().equals("UPDATE")
                && event.type() == RecordType.BRIEF));
        verify(changes).update(eq(2), any(), argThat(event -> event.username().equals("pat") && event.action().equals("UPDATE")
                && event.type() == RecordType.FACILITY));
    }

    @Test
    void invalidUpdateSavesNothing() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(new Brief(1, "Old", "2020-01-01", "a", null)));

        assertThrows(IllegalArgumentException.class, () -> controller.updateRecord(RecordType.BRIEF, 1,
                Map.of("title", "New", "date", "not-a-date", "text", "b"), admin));
        verify(briefManager, never()).updateRecord(anyString(), anyString(), anyString(), any());
        verifyNoInteractions(auditLog);
    }

    @Test
    void formValuesForBriefAgentAndMission() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(new Brief(1, "Op", "2020-01-01", "Go", null)));
        when(agentManager.getRecords("id", "%")).thenReturn(List.of(new Agent(2, "Ghost", "1992-11-22", null, null, 3)));
        when(missionManager.getRecords("id", "%")).thenReturn(List.of(new Mission(4, "Joint", new int[]{1, 2},
                new int[]{}, 1)));

        assertEquals(Map.of("title", "Op", "date", "2020-01-01", "text", "Go"), controller.formValues(RecordType.BRIEF, 1));
        assertEquals(Map.of("name", "Ghost", "dateOfBirth", "1992-11-22", "dateOfDeath", "", "notes", "",
                "facilityId", "3"), controller.formValues(RecordType.AGENT, 2));
        assertEquals(Map.of("title", "Joint", "facilityId", "1", "briefIds", "1, 2", "agentIds", ""),
                controller.formValues(RecordType.MISSION, 4));
    }
}
