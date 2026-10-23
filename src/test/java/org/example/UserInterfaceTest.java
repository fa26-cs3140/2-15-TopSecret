package org.example;

import org.model.Agent;
import org.model.Brief;
import org.model.Facility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Homework 5 user interface (Team Member B).
 *
 * UserInterface depends on two interfaces so it can be tested
 * without SQLite:
 *   AgencyDataService - lists and adds briefs, agents, and facilities
 *   AuditLogService   - implemented by Team D's AuditLogManager
 *
 * Main menu:  1 Briefs, 2 Agents, 3 Facilities,
 *             4 Review audit log (admin only), 0 Exit
 * Type menu:  1 List, 2 Read, 3 Add (admin only), 0 Back
 */
@ExtendWith(MockitoExtension.class)
class UserInterfaceTest {

    @Mock
    private AgencyDataService dataService;

    @Mock
    private AuditLogService auditLog;

    private ByteArrayOutputStream output;

    private final UserAccount admin = new UserAccount("pat", true);
    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
    }

    private UserInterface createInterface(UserAccount user, String input) {
        return new UserInterface(
                dataService,
                auditLog,
                user,
                new Scanner(input),
                new PrintStream(output)
        );
    }

    private String text() {
        return output.toString();
    }

    // ---------- Constructor and getters ----------

    @Test
    void constructorStoresCurrentUser() {
        UserInterface ui = createInterface(admin, "");

        assertSame(admin, ui.getCurrentUser());
    }

    @Test
    void constructorRejectsNullDataService() {
        assertThrows(IllegalArgumentException.class, () -> new UserInterface(
                null, auditLog, admin, new Scanner(""), new PrintStream(output)));
    }

    @Test
    void constructorRejectsNullAuditLog() {
        assertThrows(IllegalArgumentException.class, () -> new UserInterface(
                dataService, null, admin, new Scanner(""), new PrintStream(output)));
    }

    @Test
    void constructorRejectsNullUser() {
        assertThrows(IllegalArgumentException.class, () -> new UserInterface(
                dataService, auditLog, null, new Scanner(""), new PrintStream(output)));
    }

    @Test
    void constructorRejectsNullScanner() {
        assertThrows(IllegalArgumentException.class, () -> new UserInterface(
                dataService, auditLog, admin, null, new PrintStream(output)));
    }

    @Test
    void constructorRejectsNullOutput() {
        assertThrows(IllegalArgumentException.class, () -> new UserInterface(
                dataService, auditLog, admin, new Scanner(""), null));
    }

    // ---------- Main menu ----------

    @Test
    void mainMenuShowsAllOptionsForAdmin() {
        createInterface(admin, "").displayMainMenu();

        assertTrue(text().contains("1. Briefs"));
        assertTrue(text().contains("2. Agents"));
        assertTrue(text().contains("3. Facilities"));
        assertTrue(text().contains("4. Review audit log"));
        assertTrue(text().contains("0. Exit"));
    }

    @Test
    void mainMenuHidesAuditLogForStandardUser() {
        createInterface(standard, "").displayMainMenu();

        assertTrue(text().contains("1. Briefs"));
        assertFalse(text().contains("audit log"));
    }

    @Test
    void exitEndsInterface() {
        createInterface(admin, "0\n").run();

        assertTrue(text().contains("Exiting"));
    }

    @Test
    void runStopsWhenInputEnds() {
        assertDoesNotThrow(() -> createInterface(admin, "").run());
    }

    @Test
    void invalidMainMenuSelectionIsRejected() {
        createInterface(admin, "banana\n9\n0\n").run();

        assertTrue(text().contains("Invalid selection"));
    }

    @Test
    void standardUserCannotSelectAuditLog() {
        createInterface(standard, "4\n0\n").run();

        assertTrue(text().contains("Invalid selection"));
        verify(auditLog, never()).getAuditLog();
    }

    // ---------- Type menu ----------

    @Test
    void typeMenuShowsAddForAdmin() {
        createInterface(admin, "").displayTypeMenu(RecordType.AGENT);

        assertTrue(text().contains("Agents"));
        assertTrue(text().contains("1. List"));
        assertTrue(text().contains("2. Read"));
        assertTrue(text().contains("3. Add"));
        assertTrue(text().contains("0. Back"));
    }

    @Test
    void typeMenuHidesAddForStandardUser() {
        createInterface(standard, "").displayTypeMenu(RecordType.BRIEF);

        assertTrue(text().contains("1. List"));
        assertFalse(text().contains("3. Add"));
    }

    @Test
    void standardUserCannotSelectAdd() {
        createInterface(standard, "1\n3\n0\n0\n").run();

        assertTrue(text().contains("Invalid selection"));
        verify(dataService, never()).addBrief(any(), any(), any());
    }

    @Test
    void invalidTypeMenuSelectionIsRejected() {
        createInterface(admin, "3\nx\n0\n0\n").run();

        assertTrue(text().contains("Invalid selection"));
    }

    // ---------- Briefs ----------

    @Test
    void listBriefsShowsIdsAndTitles() {
        when(dataService.getBriefs()).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null),
                new Brief(2, "The Munich Lead", "1972-09-15", "Identify the cell.", null)));

        createInterface(standard, "1\n1\n0\n0\n").run();

        assertTrue(text().contains("1. Operation Sandtrap"));
        assertTrue(text().contains("2. The Munich Lead"));
    }

    @Test
    void listBriefsShowsMessageWhenEmpty() {
        when(dataService.getBriefs()).thenReturn(List.of());

        createInterface(standard, "1\n1\n0\n0\n").run();

        assertTrue(text().contains("No briefs"));
    }

    @Test
    void readBriefShowsAllPropertiesAndIsAudited() {
        when(dataService.getBriefs()).thenReturn(List.of(
                new Brief(2, "The Munich Lead", "1972-09-15", "Identify the cell.", null)));

        createInterface(standard, "1\n2\n2\n0\n0\n").run();

        assertTrue(text().contains("Title: The Munich Lead"));
        assertTrue(text().contains("Date: 1972-09-15"));
        assertTrue(text().contains("Text: Identify the cell."));
        verify(auditLog).log("agent_7", "READ", "BRIEF", "2");
    }

    @Test
    void readBriefWithUnknownIdIsRejectedAndNotAudited() {
        when(dataService.getBriefs()).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null)));

        createInterface(standard, "1\n2\n99\n0\n0\n").run();

        assertTrue(text().contains("No brief found"));
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    @Test
    void readBriefWithNonNumericIdIsRejected() {
        createInterface(standard, "1\n2\nabc\n0\n0\n").run();

        assertTrue(text().contains("Invalid ID"));
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    @Test
    void addBriefPromptsForPropertiesAndIsAudited() {
        when(dataService.addBrief("Operation Nightfall", "1980-05-15", "New report."))
                .thenReturn(new Brief(101, "Operation Nightfall", "1980-05-15", "New report.", null));

        createInterface(admin, "1\n3\nOperation Nightfall\n1980-05-15\nNew report.\n0\n0\n").run();

        assertTrue(text().contains("Title:"));
        assertTrue(text().contains("Date (yyyy-mm-dd):"));
        assertTrue(text().contains("Text:"));
        assertTrue(text().contains("Added"));
        verify(dataService).addBrief("Operation Nightfall", "1980-05-15", "New report.");
        verify(auditLog).log("pat", "CREATE", "BRIEF", "101");
    }

    @Test
    void addedBriefIsImmediatelyVisibleInList() {
        List<Brief> briefs = new ArrayList<>();
        briefs.add(new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null));
        when(dataService.getBriefs()).thenAnswer(invocation -> List.copyOf(briefs));
        when(dataService.addBrief("Operation Nightfall", "1980-05-15", "New report."))
                .thenAnswer(invocation -> {
                    Brief brief = new Brief(2, "Operation Nightfall", "1980-05-15", "New report.", null);
                    briefs.add(brief);
                    return brief;
                });

        createInterface(admin, "1\n3\nOperation Nightfall\n1980-05-15\nNew report.\n1\n0\n0\n").run();

        assertTrue(text().contains("2. Operation Nightfall"));
    }

    @Test
    void addBriefWithInvalidDataShowsErrorAndIsNotAudited() {
        when(dataService.addBrief(any(), eq("November 3"), any()))
                .thenThrow(new IllegalArgumentException("Invalid date."));

        createInterface(admin, "1\n3\nBad Brief\nNovember 3\nText\n0\n0\n").run();

        assertTrue(text().contains("Unable to add brief"));
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    // ---------- Agents ----------

    @Test
    void listAgentsShowsIdsAndNames() {
        when(dataService.getAgents()).thenReturn(List.of(
                new Agent(1, "Vanguard", "1985-06-14", null, null, 1),
                new Agent(2, "Ghost", "1992-11-22", null, null, 2)));

        createInterface(standard, "2\n1\n0\n0\n").run();

        assertTrue(text().contains("1. Vanguard"));
        assertTrue(text().contains("2. Ghost"));
    }

    @Test
    void listAgentsShowsMessageWhenEmpty() {
        when(dataService.getAgents()).thenReturn(List.of());

        createInterface(standard, "2\n1\n0\n0\n").run();

        assertTrue(text().contains("No agents"));
    }

    @Test
    void readAgentShowsAllPropertiesAndIsAudited() {
        when(dataService.getAgents()).thenReturn(List.of(
                new Agent(5, "Falcon", "1982-01-30", "2020-03-01", "Retired.", 3)));

        createInterface(standard, "2\n2\n5\n0\n0\n").run();

        assertTrue(text().contains("Name: Falcon"));
        assertTrue(text().contains("Date of birth: 1982-01-30"));
        assertTrue(text().contains("Date of death: 2020-03-01"));
        assertTrue(text().contains("Notes: Retired."));
        assertTrue(text().contains("Facility ID: 3"));
        verify(auditLog).log("agent_7", "READ", "AGENT", "5");
    }

    @Test
    void readAgentShowsPlaceholderForMissingOptionalFields() {
        when(dataService.getAgents()).thenReturn(List.of(
                new Agent(1, "Vanguard", "1985-06-14", null, null, 1)));

        createInterface(standard, "2\n2\n1\n0\n0\n").run();

        assertTrue(text().contains("Date of death: N/A"));
        assertTrue(text().contains("Notes: N/A"));
    }

    @Test
    void readAgentWithUnknownIdIsRejected() {
        when(dataService.getAgents()).thenReturn(List.of());

        createInterface(standard, "2\n2\n4\n0\n0\n").run();

        assertTrue(text().contains("No agent found"));
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    @Test
    void addAgentPromptsForPropertiesAndIsAudited() {
        when(dataService.addAgent("Jackal", "1991-02-11", "2024-01-01", "Double agent.", 2))
                .thenReturn(new Agent(11, "Jackal", "1991-02-11", "2024-01-01", "Double agent.", 2));

        createInterface(admin, "2\n3\nJackal\n1991-02-11\n2024-01-01\nDouble agent.\n2\n0\n0\n").run();

        assertTrue(text().contains("Name:"));
        assertTrue(text().contains("Date of birth (yyyy-mm-dd):"));
        assertTrue(text().contains("Date of death"));
        assertTrue(text().contains("Notes"));
        assertTrue(text().contains("Facility ID:"));
        verify(auditLog).log("pat", "CREATE", "AGENT", "11");
    }

    @Test
    void addAgentTreatsBlankOptionalFieldsAsNull() {
        when(dataService.addAgent("Jackal", "1991-02-11", null, null, 2))
                .thenReturn(new Agent(11, "Jackal", "1991-02-11", null, null, 2));

        createInterface(admin, "2\n3\nJackal\n1991-02-11\n\n\n2\n0\n0\n").run();

        verify(dataService).addAgent("Jackal", "1991-02-11", null, null, 2);
    }

    @Test
    void addAgentWithNonNumericFacilityIdIsRejected() {
        createInterface(admin, "2\n3\nJackal\n1991-02-11\n\n\nBA\n0\n0\n").run();

        assertTrue(text().contains("Invalid ID"));
        verify(dataService, never()).addAgent(any(), any(), any(), any(), anyInt());
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    @Test
    void addAgentRejectedByServiceShowsError() {
        when(dataService.addAgent(any(), any(), any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("Facility does not exist."));

        createInterface(admin, "2\n3\nJackal\n1991-02-11\n\n\n99\n0\n0\n").run();

        assertTrue(text().contains("Unable to add agent"));
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    // ---------- Facilities ----------

    @Test
    void listFacilitiesShowsIdsNamesAndAbbreviations() {
        when(dataService.getFacilities()).thenReturn(List.of(
                new Facility(1, "Blackwood Annex", "BA"),
                new Facility(2, "Rose Garden", "RG")));

        createInterface(standard, "3\n1\n0\n0\n").run();

        assertTrue(text().contains("1. Blackwood Annex (BA)"));
        assertTrue(text().contains("2. Rose Garden (RG)"));
    }

    @Test
    void listFacilitiesShowsMessageWhenEmpty() {
        when(dataService.getFacilities()).thenReturn(List.of());

        createInterface(standard, "3\n1\n0\n0\n").run();

        assertTrue(text().contains("No facilities"));
    }

    @Test
    void readFacilityShowsAllPropertiesAndIsAudited() {
        when(dataService.getFacilities()).thenReturn(List.of(
                new Facility(3, "Meridian Observatory", "MO")));

        createInterface(standard, "3\n2\n3\n0\n0\n").run();

        assertTrue(text().contains("Name: Meridian Observatory"));
        assertTrue(text().contains("Abbreviation: MO"));
        verify(auditLog).log("agent_7", "READ", "FACILITY", "3");
    }

    @Test
    void readFacilityWithUnknownIdIsRejected() {
        when(dataService.getFacilities()).thenReturn(List.of());

        createInterface(standard, "3\n2\n8\n0\n0\n").run();

        assertTrue(text().contains("No facility found"));
    }

    @Test
    void addFacilityPromptsForPropertiesAndIsAudited() {
        when(dataService.addFacility("Fairbanks Point", "FP"))
                .thenReturn(new Facility(6, "Fairbanks Point", "FP"));

        createInterface(admin, "3\n3\nFairbanks Point\nFP\n0\n0\n").run();

        assertTrue(text().contains("Name:"));
        assertTrue(text().contains("Abbreviation:"));
        verify(auditLog).log("pat", "CREATE", "FACILITY", "6");
    }

    @Test
    void addFacilityRejectedByServiceShowsError() {
        when(dataService.addFacility(any(), any()))
                .thenThrow(new IllegalArgumentException("Name cannot be blank."));

        createInterface(admin, "3\n3\n\nFP\n0\n0\n").run();

        assertTrue(text().contains("Unable to add facility"));
        verify(auditLog, never()).log(any(), any(), any(), any());
    }

    // ---------- Audit log ----------

    @Test
    void adminCanReviewAuditLog() {
        when(auditLog.getAuditLog()).thenReturn(List.of(
                new AuditLogEntry(1, "pat", "READ", "BRIEF", "2",
                        LocalDateTime.of(2026, 10, 22, 9, 30)),
                new AuditLogEntry(2, "agent_7", "CREATE", "AGENT", "11",
                        LocalDateTime.of(2026, 10, 22, 10, 15))));

        createInterface(admin, "4\n0\n").run();

        assertTrue(text().contains("pat"));
        assertTrue(text().contains("READ"));
        assertTrue(text().contains("BRIEF"));
        assertTrue(text().contains("2026-10-22T09:30"));
        assertTrue(text().contains("agent_7"));
        assertTrue(text().contains("CREATE"));
        assertTrue(text().contains("AGENT"));
        verify(auditLog).getAuditLog();
    }

    @Test
    void emptyAuditLogShowsMessage() {
        when(auditLog.getAuditLog()).thenReturn(List.of());

        createInterface(admin, "4\n0\n").run();

        assertTrue(text().contains("No audit entries"));
    }

    // ---------- Session flow ----------

    @Test
    void multipleOperationsCanRunInOneSession() {
        when(dataService.getBriefs()).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null)));
        when(dataService.getFacilities()).thenReturn(List.of(
                new Facility(1, "Blackwood Annex", "BA")));

        createInterface(standard, "1\n1\n0\n3\n1\n0\n0\n").run();

        assertTrue(text().contains("Operation Sandtrap"));
        assertTrue(text().contains("Blackwood Annex"));
        assertTrue(text().contains("Exiting"));
    }
}
