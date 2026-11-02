package org.example.controller;

import org.example.AgencyDataService;
import org.example.AuditLogEntry;
import org.example.AuditLogService;
import org.example.RecordType;
import org.example.UserAccount;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Disabled("HW6 week 1 draft: enable when AppController is implemented in week 2")
@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    @Mock
    private AgencyDataService data;

    @Mock
    private AuditLogService auditLog;

    private AppController controller;

    private final UserAccount admin = new UserAccount("pat", true);
    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() {
        controller = new AppController(data, auditLog);
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new AppController(null, auditLog));
        assertThrows(IllegalArgumentException.class, () -> new AppController(data, null));
    }

    @Test
    void listBriefsShowsIdsAndTitles() {
        when(data.getBriefs()).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null),
                new Brief(2, "The Munich Lead", "1972-09-15", "Find the cell.", null)));

        String text = controller.listRecords(RecordType.BRIEF);

        assertTrue(text.contains("1. Operation Sandtrap"));
        assertTrue(text.contains("2. The Munich Lead"));
    }

    @Test
    void listFacilitiesShowsAbbreviations() {
        when(data.getFacilities()).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));

        assertTrue(controller.listRecords(RecordType.FACILITY).contains("1. Blackwood Annex (BA)"));
    }

    @Test
    void emptyListShowsMessage() {
        when(data.getAgents()).thenReturn(List.of());

        assertTrue(controller.listRecords(RecordType.AGENT).contains("No agents are available."));
    }

    @Test
    void readRecordShowsDetailsAndIsAudited() {
        when(data.getAgents()).thenReturn(List.of(
                new Agent(4, "Jackal", "1991-02-11", null, null, 1)));

        String text = controller.readRecord(RecordType.AGENT, 4, standard);

        assertTrue(text.contains("Name: Jackal"));
        assertTrue(text.contains("Date of death: N/A"));
        verify(auditLog).log("agent_7", "READ", "AGENT", "4");
    }

    @Test
    void readUnknownRecordIsNotAudited() {
        when(data.getBriefs()).thenReturn(List.of());

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
}
