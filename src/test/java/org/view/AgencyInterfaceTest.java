package org.view;

import org.model.*;
import org.managers.UserAccountManager;
import org.managers.RecordEditor;
import org.managers.AuditLogService;
import org.managers.AgentManager;
import org.managers.FacilityManager;
import org.managers.BriefManager;
import org.managers.MissionManager;
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
 * AgencyInterface depends on two interfaces so it can be tested
 * without SQLite:
 *   AgencyDataService - lists and adds briefs, agents, and facilities
 *   AuditLogService   - implemented by Team D's AuditLogManager
 *
 * Main menu:  1 Briefs, 2 Agents, 3 Facilities,
 *             4 Review audit log (admin only), 0 Exit
 * Type menu:  1 List, 2 Read, 3 Add (admin only), 0 Back
 */
@ExtendWith(MockitoExtension.class)
class AgencyInterfaceTest {

    @Mock
    private AgentManager agentManager;

    @Mock
    private FacilityManager facilityManager;

    @Mock
    private BriefManager briefManager;

    @Mock
    private UserAccountManager userManager;

    @Mock
    private AuditLogService auditLog;

    @Mock
    private MissionManager missionManager;

    @Mock
    private RecordEditor.Store<Object> changes;

    private ByteArrayOutputStream output;

    private final UserAccount admin = new UserAccount("pat", true);
    private final UserAccount standard = new UserAccount("agent_7", false);

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        lenient().when(facilityManager.getRecords("id", "%")).thenReturn(List.of(new Facility(2, "HQ", "HQ")));
    }

    private AgencyInterface createInterface(UserAccount user, String input) {
        lenient().when(userManager.getUser(user.getUsername())).thenReturn(user);
        return new AgencyInterface(
                agentManager,
                facilityManager,
                briefManager,
                userManager,
                missionManager,
                auditLog,
                user,
                new Scanner(input),
                new PrintStream(output),
                changes
        );
    }

    private String text() {
        return output.toString();
    }

    // ---------- Constructor and getters ----------

    @Test
    void constructorStoresCurrentUser() {
        AgencyInterface ui = createInterface(admin, "");

        assertSame(admin, ui.getCurrentUser());
    }

    @Test
    void constructorRejectsNullAgentManager() {
        assertThrows(IllegalArgumentException.class,
                () -> new AgencyInterface(
                        null,
                        facilityManager,
                        briefManager,
                        userManager,
                        missionManager,
                        auditLog,
                        admin,
                        new Scanner(""),
                        new PrintStream(output)
                ));
    }

    @Test
    void constructorRejectsNullBriefManager() {
        assertThrows(IllegalArgumentException.class,
                () -> new AgencyInterface(
                        agentManager,
                        facilityManager,
                        null,
                        userManager,
                        missionManager,
                        auditLog,
                        admin,
                        new Scanner(""),
                        new PrintStream(output)
                ));
    }

    @Test
    void constructorRejectsNullFacilityManager() {
        assertThrows(IllegalArgumentException.class,
                () -> new AgencyInterface(
                        agentManager,
                        null,
                        briefManager,
                        userManager,
                        missionManager,
                        auditLog,
                        admin,
                        new Scanner(""),
                        new PrintStream(output)
                ));
    }

    @Test
    void constructorRejectsNullAuditLog() {
        assertThrows(IllegalArgumentException.class, () -> new AgencyInterface(
                agentManager,
                facilityManager,
                briefManager,
                userManager,
                missionManager,
                null,
                admin,
                new Scanner(""),
                new PrintStream(output)
            )
        );
    }

    @Test
    void constructorRejectsNullUser() {
        assertThrows(IllegalArgumentException.class, () -> new AgencyInterface(
                agentManager,
                facilityManager,
                briefManager,
                userManager,
                missionManager,
                auditLog,
                null,
                new Scanner(""),
                new PrintStream(output)
            )
        );
    }

    @Test
    void constructorRejectsNullScanner() {
        assertThrows(IllegalArgumentException.class, () -> new AgencyInterface(
                agentManager,
                facilityManager,
                briefManager,
                userManager,
                missionManager,
                auditLog,
                admin,
                null,
                new PrintStream(output)
            )
        );
    }

    @Test
    void constructorRejectsNullOutput() {
        assertThrows(IllegalArgumentException.class, () -> new AgencyInterface(
                agentManager,
                facilityManager,
                briefManager,
                userManager,
                missionManager,
                auditLog,
                admin,
                new Scanner(""),
                null
            )
        );
    }

    // ---------- Main menu ----------

    @Test
    void mainMenuShowsAllOptionsForAdmin() {
        createInterface(admin, "").displayMainMenu();

        assertTrue(text().contains("1. Briefs"));
        assertTrue(text().contains("2. Agents"));
        assertTrue(text().contains("3. Facilities"));
        assertTrue(text().contains("4. Missions"));
        assertTrue(text().contains("5. Create User"));
        assertTrue(text().contains("6. Review audit log"));
        assertTrue(text().contains("7. Manage User Permissions"));
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
        createInterface(standard, "6\n0\n").run();

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
        verify(briefManager, never()).addRecord(any(), any(), any());
    }

    @Test
    void invalidTypeMenuSelectionIsRejected() {
        createInterface(admin, "3\nx\n0\n0\n").run();

        assertTrue(text().contains("Invalid selection"));
    }

    // ---------- Briefs ----------

    @Test
    void listBriefsShowsIdsAndTitles() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null),
                new Brief(2, "The Munich Lead", "1972-09-15", "Identify the cell.", null)));

        createInterface(standard, "1\n1\n0\n0\n").run();

        assertTrue(text().contains("1. Operation Sandtrap"));
        assertTrue(text().contains("2. The Munich Lead"));
    }

    @Test
    void listBriefsShowsMessageWhenEmpty() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of());

        createInterface(standard, "1\n1\n0\n0\n").run();

        assertTrue(text().contains("No briefs"));
    }

    @Test
    void readBriefShowsAllPropertiesAndIsAudited() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(
                new Brief(2, "The Munich Lead", "1972-09-15", "Identify the cell.", null)));

        createInterface(standard, "1\n2\n2\n0\n0\n").run();

        assertTrue(text().contains("Title: The Munich Lead"));
        assertTrue(text().contains("Date: 1972-09-15"));
        assertTrue(text().contains("Text: Identify the cell."));
        verify(auditLog).log("agent_7", "READ", "BRIEF", "2");
    }

    @Test
    void readBriefWithUnknownIdIsRejectedAndNotAudited() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null)));

        createInterface(standard, "1\n2\n99\n0\n0\n").run();

        assertTrue(text().contains("No brief found"));
        verify(auditLog, never()).log(any(), org.mockito.AdditionalMatchers.not(eq("LOGOUT")), any(), any());
    }

    @Test
    void readBriefWithNonNumericIdIsRejected() {
        createInterface(standard, "1\n2\nabc\n0\n0\n").run();

        assertTrue(text().contains("Invalid ID"));
        verify(auditLog, never()).log(any(), org.mockito.AdditionalMatchers.not(eq("LOGOUT")), any(), any());
    }

    @Test
    void addBriefPromptsForPropertiesAndIsAudited() {
        when(changes.create(any(Brief.class), any())).thenReturn(101);

        createInterface(admin, "1\n3\nOperation Nightfall\n1980-05-15\nNew report.\n0\n0\n").run();

        assertTrue(text().contains("Title:"));
        assertTrue(text().contains("Date (yyyy-mm-dd):"));
        assertTrue(text().contains("Text:"));
        assertTrue(text().contains("Added"));
        verify(changes).create(argThat(record -> record instanceof Brief brief
                && brief.getTitle().equals("Operation Nightfall") && brief.getText().equals("New report.")),
                argThat(event -> event.username().equals("pat") && event.action().equals("CREATE")
                        && event.type() == RecordType.BRIEF));
        verify(auditLog, never()).log(any(), eq("CREATE"), any(), any());
    }

    @Test
    void addedBriefIsImmediatelyVisibleInList() {
        List<Brief> briefs = new ArrayList<>();
        briefs.add(new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null));
        when(briefManager.getRecords("id", "%")).thenAnswer(invocation -> List.copyOf(briefs));
        when(changes.create(any(Brief.class), any())).thenAnswer(invocation -> {
            Brief record = invocation.getArgument(0);
            briefs.add(new Brief(2, record.getTitle(), record.getDate(), record.getText(), null));
            return 2;
        });

        createInterface(admin, "1\n3\nOperation Nightfall\n1980-05-15\nNew report.\n1\n0\n0\n").run();

        assertTrue(text().contains("2. Operation Nightfall"));
    }

    @Test
    void addBriefWithInvalidDataShowsErrorAndIsNotAudited() {
        createInterface(admin, "1\n3\nBad Brief\nNovember 3\nText\n0\n0\n").run();

        assertTrue(text().contains("Unable to add brief"));
        verify(auditLog, never()).log(any(), org.mockito.AdditionalMatchers.not(eq("LOGOUT")), any(), any());
    }

    // ---------- Agents ----------

    @Test
    void listAgentsShowsIdsAndNames() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of(
                new Agent(1, "Vanguard", "1985-06-14", null, null, 1),
                new Agent(2, "Ghost", "1992-11-22", null, null, 2)));

        createInterface(standard, "2\n1\n0\n0\n").run();

        assertTrue(text().contains("1. Vanguard"));
        assertTrue(text().contains("2. Ghost"));
    }

    @Test
    void listAgentsShowsMessageWhenEmpty() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of());

        createInterface(standard, "2\n1\n0\n0\n").run();

        assertTrue(text().contains("No agents"));
    }

    @Test
    void readAgentShowsAllPropertiesAndIsAudited() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of(
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
        when(agentManager.getRecords("id", "%")).thenReturn(List.of(
                new Agent(1, "Vanguard", "1985-06-14", null, null, 1)));

        createInterface(standard, "2\n2\n1\n0\n0\n").run();

        assertTrue(text().contains("Date of death: N/A"));
        assertTrue(text().contains("Notes: N/A"));
    }

    @Test
    void readAgentWithUnknownIdIsRejected() {
        when(agentManager.getRecords("id", "%")).thenReturn(List.of());

        createInterface(standard, "2\n2\n4\n0\n0\n").run();

        assertTrue(text().contains("No agent found"));
        verify(auditLog, never()).log(any(), org.mockito.AdditionalMatchers.not(eq("LOGOUT")), any(), any());
    }

    @Test
    void addAgentPromptsForPropertiesAndIsAudited() {
        when(changes.create(any(Agent.class), any())).thenReturn(11);

        createInterface(admin, "2\n3\nJackal\n1991-02-11\n2024-01-01\nDouble agent.\n2\n0\n0\n").run();

        assertTrue(text().contains("Name:"));
        assertTrue(text().contains("Date of birth (yyyy-mm-dd):"));
        assertTrue(text().contains("Date of death"));
        assertTrue(text().contains("Notes"));
        assertTrue(text().contains("Facility ID:"));
        verify(changes).create(any(Agent.class), argThat(event -> event.type() == RecordType.AGENT
                && event.username().equals("pat")));
    }

    @Test
    void addAgentTreatsBlankOptionalFieldsAsNull() {
        when(changes.create(any(Agent.class), any())).thenReturn(11);

        createInterface(admin, "2\n3\nJackal\n1991-02-11\n\n\n2\n0\n0\n").run();

        verify(changes).create(argThat(record -> record instanceof Agent agent
                && agent.getDateOfDeath() == null && agent.getNotes() == null && agent.getFacilityId() == 2), any());
    }

    @Test
    void addAgentWithNonNumericFacilityIdIsRejected() {
        createInterface(admin, "2\n3\nJackal\n1991-02-11\n\n\nBA\n0\n0\n").run();

        assertTrue(text().contains("Invalid ID"));
        verify(agentManager, never()).addRecord(any(), any(), any(), any(), anyInt());
        verify(auditLog, never()).log(any(), org.mockito.AdditionalMatchers.not(eq("LOGOUT")), any(), any());
    }

    @Test
    void addAgentRejectedByServiceShowsError() {
        createInterface(admin, "2\n3\nJackal\n1991-02-11\n\n\n99\n0\n0\n").run();

        assertTrue(text().contains("Unable to add agent"));
        verify(auditLog, never()).log(any(), org.mockito.AdditionalMatchers.not(eq("LOGOUT")), any(), any());
    }

    // ---------- Facilities ----------

    @Test
    void listFacilitiesShowsIdsNamesAndAbbreviations() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(
                new Facility(1, "Blackwood Annex", "BA", 1),
                new Facility(2, "Rose Garden", "RG", 1)));

        createInterface(standard, "3\n1\n0\n0\n").run();

        assertTrue(text().contains("1. Blackwood Annex (BA)"));
        assertTrue(text().contains("2. Rose Garden (RG)"));
    }

    @Test
    void listFacilitiesShowsMessageWhenEmpty() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of());

        createInterface(standard, "3\n1\n0\n0\n").run();

        assertTrue(text().contains("No facilities"));
    }

    @Test
    void readFacilityShowsAllPropertiesAndIsAudited() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(
                new Facility(3, "Meridian Observatory", "MO", 1)));

        createInterface(standard, "3\n2\n3\n0\n0\n").run();

        assertTrue(text().contains("Name: Meridian Observatory"));
        assertTrue(text().contains("Abbreviation: MO"));
        verify(auditLog).log("agent_7", "READ", "FACILITY", "3");
    }

    @Test
    void readFacilityWithUnknownIdIsRejected() {
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of());

        createInterface(standard, "3\n2\n8\n0\n0\n").run();

        assertTrue(text().contains("No facility found"));
    }

    @Test
    void addFacilityPromptsForPropertiesAndIsAudited() {
        when(facilityManager.getCountry(1)).thenReturn(new Country("Afghanistan", 1, new Facility[0]));
        when(changes.create(any(Facility.class), any())).thenReturn(6);

        createInterface(admin, "3\n3\nFairbanks Point\nFP\n1\n0\n0\n").run();

        assertTrue(text().contains("Name:"));
        assertTrue(text().contains("Abbreviation:"));
        verify(changes).create(argThat(record -> record instanceof Facility facility
                && facility.getName().equals("Fairbanks Point") && facility.getCountryID() == 1),
                argThat(event -> event.type() == RecordType.FACILITY));
    }

    @Test
    void addFacilityRejectedByServiceShowsError() {
        when(facilityManager.getCountry(1)).thenReturn(new Country("Afghanistan", 1, new Facility[0]));

        createInterface(admin, "3\n3\n\nFP\n1\n0\n0\n").run();

        assertTrue(text().contains("Unable to add facility"));
        verify(auditLog, never()).log(any(), org.mockito.AdditionalMatchers.not(eq("LOGOUT")), any(), any());
    }

    // ---------- Audit log ----------

    @Test
    void adminCanReviewAuditLog() {
        when(auditLog.getAuditLog()).thenReturn(List.of(
                new AuditLogEntry(1, "pat", "READ", "BRIEF", "2",
                        LocalDateTime.of(2026, 10, 22, 9, 30)),
                new AuditLogEntry(2, "agent_7", "CREATE", "AGENT", "11",
                        LocalDateTime.of(2026, 10, 22, 10, 15))));

        createInterface(admin, "6\n0\n").run();

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

        createInterface(admin, "6\n0\n").run();

        assertTrue(text().contains("No audit entries"));
    }

    // ---------- Session flow ----------

    @Test
    void multipleOperationsCanRunInOneSession() {
        when(briefManager.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "Operation Sandtrap", "1970-11-03", "Bug the pouch.", null)));
        when(facilityManager.getRecords("id", "%")).thenReturn(List.of(
                new Facility(1, "Blackwood Annex", "BA", 1)));

        createInterface(standard, "1\n1\n0\n3\n1\n0\n0\n").run();

        assertTrue(text().contains("Operation Sandtrap"));
        assertTrue(text().contains("Blackwood Annex"));
        assertTrue(text().contains("Exiting"));
    }

    // ---------- Create user ----------

    @Test
    void createUserRepromptsInvalidUsernameAndPassword() {
        when(userManager.isValidUsername(anyString())).thenAnswer(i -> i.getArgument(0).equals("ghost"));
        when(userManager.isValidPassword(anyString())).thenAnswer(i -> i.getArgument(0).equals("Secret1"));

        createInterface(admin, "5\nGhost\nghost\nweak\nSecret1\n0\n").run();

        assertTrue(text().contains("Usernames need"));
        assertTrue(text().contains("Passwords need"));
        verify(userManager).createUser("ghost", false);
        verify(userManager).createCredentials("ghost", "Secret1");
        verify(auditLog).log("pat", "CREATE", "USER", "ghost");
    }

    @Test
    void createUserWithTakenUsernameShowsErrorAndIsNotAudited() {
        when(userManager.isValidUsername("ghost")).thenReturn(true);
        when(userManager.isValidPassword("Secret1")).thenReturn(true);
        when(userManager.createUser("ghost", false)).thenThrow(new IllegalArgumentException("Username already exists."));

        createInterface(admin, "5\nghost\nSecret1\n0\n").run();

        assertTrue(text().contains("Unable to create user: Username already exists."));
        verify(userManager, never()).createCredentials(any(), any());
        verify(auditLog, never()).log(any(), eq("CREATE"), eq("USER"), any());
    }

    // ---------- Change password ----------

    @Test
    void changePasswordSavesNewPasswordAfterCorrectCurrentPassword() {
        when(userManager.verifyLogin("agent_7", "Secret1")).thenReturn(true);
        when(userManager.isValidPassword("NewPass1")).thenReturn(true);
        when(userManager.changePassword("agent_7", "NewPass1")).thenReturn(true);

        assertTrue(createInterface(standard, "Secret1\nNewPass1\n").changePasswordMenu());
        assertTrue(text().contains("Password changed."));
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        when(userManager.verifyLogin("agent_7", "wrong")).thenReturn(false);

        assertFalse(createInterface(standard, "wrong\n").changePasswordMenu());
        assertTrue(text().contains("Incorrect password"));
        verify(userManager, never()).changePassword(any(), any());
    }

    @Test
    void changePasswordRepromptsInvalidNewPassword() {
        when(userManager.verifyLogin("agent_7", "Secret1")).thenReturn(true);
        when(userManager.isValidPassword(anyString())).thenAnswer(i -> i.getArgument(0).equals("NewPass1"));
        when(userManager.changePassword("agent_7", "NewPass1")).thenReturn(true);

        assertTrue(createInterface(standard, "Secret1\nweak\nNewPass1\n").changePasswordMenu());
        assertTrue(text().contains("Passwords need"));
    }

    @Test
    void changePasswordReportsFailedSave() {
        when(userManager.verifyLogin("agent_7", "Secret1")).thenReturn(true);
        when(userManager.isValidPassword("NewPass1")).thenReturn(true);
        when(userManager.changePassword("agent_7", "NewPass1")).thenReturn(false);

        assertFalse(createInterface(standard, "Secret1\nNewPass1\n").changePasswordMenu());
        assertTrue(text().contains("Unable to change password."));
    }

    @Test
    void mainMenuOptionChangesPasswordForStandardUser() {
        when(userManager.verifyLogin("agent_7", "Secret1")).thenReturn(true);
        when(userManager.isValidPassword("NewPass1")).thenReturn(true);
        when(userManager.changePassword("agent_7", "NewPass1")).thenReturn(true);

        createInterface(standard, "8\nSecret1\nNewPass1\n0\n").run();

        assertTrue(text().contains("8. Change Password"));
        assertTrue(text().contains("Password changed."));
    }
}
