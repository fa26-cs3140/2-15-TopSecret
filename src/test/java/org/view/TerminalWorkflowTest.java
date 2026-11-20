package org.view;

import org.junit.jupiter.api.Test;
import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.managers.UserAccountManager;
import org.managers.RecordEditor;
import org.model.Agent;
import org.model.Brief;
import org.model.Facility;
import org.model.Mission;
import org.model.UserAccount;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TerminalWorkflowTest {

    private final AgentManager agents = mock(AgentManager.class);
    private final FacilityManager facilities = mock(FacilityManager.class);
    private final BriefManager briefs = mock(BriefManager.class);
    private final MissionManager missions = mock(MissionManager.class);
    private final UserAccountManager accounts = mock(UserAccountManager.class);
    private final RecordEditor.Store<Object> changes = mock(RecordEditor.Store.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final UserAccount admin = new UserAccount("admin", true);

    @Test
    void selectionPromptAndUserChangeUseTheCurrentIdentity() {
        AgencyInterface ui = ui("ghost\nPassword1\n");
        assertArrayEquals(new String[]{"ghost", "Password1"}, ui.userSelectionPrompt());
        UserAccount replacement = new UserAccount("ghost", false);
        ui.setUser(replacement);
        assertSame(replacement, ui.getCurrentUser());
        ui.message("Welcome");
        assertTrue(output.toString().contains("Welcome"));
        assertNull(ui("\n").userSelectionPrompt());
    }

    @Test
    void promotionMenuRejectsSelfAndPersistsAnotherUsersRole() {
        UserAccount target = new UserAccount("ghost", false);
        when(accounts.getUsers()).thenReturn(List.of(admin, target));
        when(accounts.getUser("ghost")).thenReturn(target);

        ui("7\nadmin\nghost\n1\n0\n").run();

        assertTrue(output.toString().contains("User cannot be yourself"));
        verify(accounts).setUserAdmin("ghost", true);
        verify(audit).log("admin", "ACCOUNT", "PROMOTION", "ghost");
    }

    @Test
    void terminalMissionCreationUsesTheSelectedRelatedRecordIds() {
        when(facilities.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "HQ", "HQ")));
        when(facilities.getColumnNames()).thenReturn(new String[]{"id", "name"});
        when(briefs.getColumnNames()).thenReturn(new String[]{"id", "title"});
        when(agents.getColumnNames()).thenReturn(new String[]{"id", "name"});
        when(briefs.getRecords("title", "Notes")).thenReturn(List.of(new Brief(2, "Notes", "2020-01-01", "text", null)));
        when(agents.getRecords("name", "Ghost")).thenReturn(List.of(new Agent(3, "Ghost", "1990-01-01", null, null, 1)));
        when(facilities.getRecords("name", "HQ")).thenReturn(List.of(new Facility(1, "HQ", "HQ")));
        when(briefs.getRecords("id", "%")).thenReturn(List.of(new Brief(2, "Notes", "2020-01-01", "text", null)));
        when(agents.getRecords("id", "%")).thenReturn(List.of(new Agent(3, "Ghost", "1990-01-01", null, null, 1)));
        when(changes.create(any(Mission.class), any())).thenReturn(4);

        ui("4\n3\nRecon\nNotes\nGhost\nHQ\n0\n0\n").run();

        var record = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(changes).create(record.capture(), argThat(event -> event.type() == org.model.RecordType.MISSION
                && event.username().equals("admin")));
        Mission saved = (Mission) record.getValue();
        assertEquals("Recon", saved.getTitle());
        assertArrayEquals(new int[]{2}, saved.getBriefIds());
        assertArrayEquals(new int[]{3}, saved.getAgentIds());
        assertEquals(1, saved.getFacilityId());
        verify(audit, never()).log(any(), eq("CREATE"), any(), any());
    }

    @Test
    void aNonDefaultFacilityIdStillAllowsMissionNavigation() {
        when(facilities.getRecords("id", "%")).thenReturn(List.of(new Facility(41, "Legacy HQ", "HQ")));
        when(missions.getRecords("id", "%")).thenReturn(List.of(
                new Mission(7, "Legacy mission", new int[0], new int[0], 41)));

        ui("4\n1\n0\n0\n").run();

        assertTrue(output.toString().contains("Legacy mission"));
    }

    @Test
    void aMissingFacilityNameDoesNotCrashMissionCreation() {
        when(facilities.getRecords("id", "%")).thenReturn(List.of(new Facility(41, "HQ", "HQ")));
        when(facilities.getColumnNames()).thenReturn(new String[]{"id", "name"});
        when(briefs.getColumnNames()).thenReturn(new String[]{"id", "title"});
        when(agents.getColumnNames()).thenReturn(new String[]{"id", "name"});

        assertDoesNotThrow(() -> ui("4\n3\nRecon\n\n\nMissing HQ\n0\n0\n").run());

        assertTrue(output.toString().contains("Unable to add mission"));
        verify(missions, never()).addRecord(anyString(), any(int[].class), any(int[].class), anyInt());
    }

    @Test
    void sessionMenuListsAndRevokesUsingTheCurrentAdministrator() {
        var sessions = mock(org.controller.SessionManager.class);
        when(sessions.listSessions(admin)).thenReturn(List.of(
                new org.controller.SessionManager.SessionSummary("ghost", "token", java.time.Instant.EPOCH)));
        AgencyInterface terminal = ui("12\ntoken\n\n0\n");
        terminal.enableSessionManagement(sessions);
        terminal.run();
        assertTrue(output.toString().contains("ghost | token | 1970-01-01T00:00:00Z"));
        verify(sessions).revokeSession(admin, "token");
        verify(accounts, atLeastOnce()).getUser("admin");
    }

    @Test
    void searchMenuShowsMatchesFromMockedManagers() {
        when(briefs.getRecords("id", "%")).thenReturn(List.of(
                new Brief(7, "Needle", "2020-01-01", "Body", null)));
        ui("11\nneedle\n0\n").run();
        assertTrue(output.toString().contains("brief 7. Needle"));
    }

    @Test
    void openingATerminalMessageUsesTheActualIdAndAuthenticatedService() {
        var service = mock(org.model.messaging.MessageService.class);
        var message = new org.model.messaging.Message(42, "sender", 2, "admin", "Body",
                java.time.LocalDateTime.of(2026, 11, 20, 12, 0));
        when(service.getInbox()).thenReturn(List.of(message));
        when(service.readMessage(42)).thenReturn(message);
        var terminal = ui("10\n1\n0\n");
        terminal.enableMessaging(service);
        terminal.run();
        verify(service).readMessage(42);
        verify(service, never()).readMessage(1);
        assertTrue(output.toString().contains("Body"));
    }

    @Test
    void terminalImportUsesTheAuditedBatchBoundaryWithFreshPermissions() throws Exception {
        var record = new Brief(1, "Imported", "2020-01-01", "Body", null);
        try (var parser = mockConstruction(org.managers.RecordImport.class, (mock, context) ->
                when(mock.read(org.model.RecordType.BRIEF, java.nio.file.Path.of("briefs.tsv")))
                        .thenReturn(List.of(record)))) {
            when(changes.createAll(eq(List.of(record)), any())).thenReturn(List.of(17));
            ui("1\n4\nbriefs.tsv\n0\n0\n").run();
            verify(changes).createAll(eq(List.of(record)), argThat(event -> event.username().equals("admin")
                    && event.type() == org.model.RecordType.BRIEF && event.action().equals("CREATE")));
            assertTrue(output.toString().contains("TSV Successfully imported"));
            verify(audit, never()).log(any(), eq("IMPORT"), any(), any());
            assertEquals(1, parser.constructed().size());
        }
    }

    private AgencyInterface ui(String input) {
        when(accounts.getUser("admin")).thenReturn(admin);
        return new AgencyInterface(agents, facilities, briefs, accounts, missions, audit, admin,
                new Scanner(input), new PrintStream(output), changes);
    }
}
