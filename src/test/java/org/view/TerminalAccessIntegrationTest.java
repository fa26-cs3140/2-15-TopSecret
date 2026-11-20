package org.view;

import org.controller.SessionManager;
import org.junit.jupiter.api.Test;
import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.managers.UserAccountManager;
import org.model.Brief;
import org.model.UserAccount;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TerminalAccessIntegrationTest {

    @Test
    void terminalListsAndRevokesTheWebServersToken() {
        SessionManager sessions = new SessionManager();
        UserAccount admin = new UserAccount("admin", true);
        String token = sessions.createSession(new UserAccount("ghost", false));
        UserAccountManager accounts = mock(UserAccountManager.class);
        when(accounts.getUser("admin")).thenReturn(admin);

        String output = run("12\n" + token + "\n\n0\n", admin, accounts, sessions);

        assertTrue(output.contains("ghost | " + token));
        assertTrue(output.contains("Last activity (UTC)"));
        assertTrue(output.contains("Session revoked."));
        assertNull(sessions.getUser(token));
    }

    @Test
    void terminalDeniesStandardUserEvenWhenOptionIsTypedDirectly() {
        SessionManager sessions = new SessionManager();
        UserAccount standard = new UserAccount("ghost", false);
        String token = sessions.createSession(standard);
        UserAccountManager accounts = mock(UserAccountManager.class);
        when(accounts.getUser("ghost")).thenReturn(standard);

        String output = run("12\n0\n", standard, accounts, sessions);

        assertFalse(output.contains(token));
        assertTrue(output.contains("Only administrators"));
        assertNotNull(sessions.getUser(token));
    }

    @Test
    void terminalSearchFindsBodyTextThroughTheMenu() {
        BriefManager briefs = mock(BriefManager.class);
        when(briefs.getRecords("id", "%")).thenReturn(List.of(
                new Brief(7, "Report", "2020-01-01", "Hidden nightbird clue", null)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AgencyInterface ui = new AgencyInterface(mock(AgentManager.class), mock(FacilityManager.class), briefs,
                mock(UserAccountManager.class), mock(MissionManager.class), mock(AuditLogService.class),
                new UserAccount("ghost", false), new Scanner("11\nnightbird\n0\n"), new PrintStream(output));
        ui.run();

        assertTrue(output.toString().contains("brief 7. Report"));
    }

    private String run(String input, UserAccount user, UserAccountManager accounts, SessionManager sessions) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AgencyInterface ui = new AgencyInterface(mock(AgentManager.class), mock(FacilityManager.class),
                mock(BriefManager.class), accounts, mock(MissionManager.class), mock(AuditLogService.class),
                user, new Scanner(input), new PrintStream(output));
        ui.enableSessionManagement(sessions);
        ui.run();
        return output.toString();
    }
}
