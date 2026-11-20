package org.view;

import org.junit.jupiter.api.Test;
import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.managers.UserAccountManager;
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
        when(accounts.getUser("admin")).thenReturn(admin);
        when(accounts.getUser("ghost")).thenReturn(target);

        ui("7\nadmin\nghost\n1\n0\n").run();

        assertTrue(output.toString().contains("User cannot be yourself"));
        verify(accounts).setUserAdmin("ghost", true);
        verify(audit).log("admin", "ACCOUNT", "PROMOTION", "ghost");
    }

    @Test
    void terminalImportCallsTheSelectedManagerAndAuditsTheOutcome() throws Exception {
        when(briefs.getTableName()).thenReturn("briefs");
        when(briefs.importTSV("briefs.tsv", "briefs", "data", "mission_database.db")).thenReturn(2);

        ui("1\n4\nbriefs.tsv\n0\n0\n").run();

        verify(briefs).importTSV("briefs.tsv", "briefs", "data", "mission_database.db");
        verify(audit).log("admin", "IMPORT", "Briefs", "2");
        assertTrue(output.toString().contains("TSV Successfully imported"));
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
        when(missions.addRecord(eq("Recon"), any(int[].class), any(int[].class), eq(1)))
                .thenReturn(new Mission(4, "Recon", new int[]{2}, new int[]{3}, 1));

        ui("4\n3\nRecon\nNotes\nGhost\nHQ\n0\n0\n").run();

        verify(missions).addRecord(eq("Recon"), org.mockito.AdditionalMatchers.aryEq(new int[]{2}),
                org.mockito.AdditionalMatchers.aryEq(new int[]{3}), eq(1));
        verify(audit).log("admin", "CREATE", "MISSION", "4");
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

    private AgencyInterface ui(String input) {
        return new AgencyInterface(agents, facilities, briefs, accounts, missions, audit, admin,
                new Scanner(input), new PrintStream(output));
    }
}
