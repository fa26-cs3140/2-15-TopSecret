package org.controller;

import org.junit.jupiter.api.Test;
import org.model.UserAccount;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionTimeoutTest {

    private static final Instant LOGIN_TIME = Instant.parse("2026-11-12T20:00:00Z");

    private final Clock clock = mock(Clock.class);
    private final SessionManager sessions = new SessionManager(clock);
    private final UserAccount admin = new UserAccount("admin", true);
    private final UserAccount agent = new UserAccount("agent", false);

    @Test
    void requestRefreshesIdleTimeoutAndExactBoundaryExpires() {
        atMinute(0);
        String token = sessions.createSession(agent);

        atMinute(29);
        assertSame(agent, sessions.getUser(token));
        assertEquals(List.of(new SessionManager.SessionSummary(
                "agent", token, LOGIN_TIME.plusSeconds(29 * 60))), sessions.listSessions(admin));

        atMinute(59);
        assertNull(sessions.getUser(token));
        assertEquals(List.of(), sessions.listSessions(admin));
    }

    @Test
    void listingFiltersExpiredTokensAndDoesNotRefreshActivity() {
        atMinute(0);
        String stale = sessions.createSession(agent);
        atMinute(15);
        String current = sessions.createSession(admin);

        atMinute(30);
        assertEquals(List.of(new SessionManager.SessionSummary(
                "admin", current, LOGIN_TIME.plusSeconds(15 * 60))), sessions.listSessions(admin));
        assertNull(sessions.getUser(stale));

        atMinute(45);
        assertNull(sessions.getUser(current));
    }

    @Test
    void revocationOnlyAffectsSelectedTokenAndRequiresAdmin() {
        atMinute(0);
        String selected = sessions.createSession(agent);
        String other = sessions.createSession(agent);

        assertThrows(SecurityException.class, () -> sessions.listSessions(agent));
        assertThrows(SecurityException.class, () -> sessions.revokeSession(agent, selected));
        assertThrows(SecurityException.class, () -> sessions.listSessions(null));
        assertSame(agent, sessions.getUser(selected));

        sessions.revokeSession(admin, selected);

        assertNull(sessions.getUser(selected));
        assertSame(agent, sessions.getUser(other));
    }

    @Test
    void currentPermissionsAndDeletedAccountsAreChecked() {
        org.managers.UserAccountManager accounts = mock(org.managers.UserAccountManager.class);
        SessionManager checked = new SessionManager(clock, accounts);
        atMinute(0);
        when(accounts.getUser("admin")).thenReturn(admin);
        String token = checked.createSession(admin);
        assertSame(admin, checked.getUser(token));

        UserAccount demoted = new UserAccount("admin", false);
        when(accounts.getUser("admin")).thenReturn(demoted);
        assertSame(demoted, checked.getUser(token));
        assertThrows(SecurityException.class, () -> checked.revokeSession(admin, token));

        when(accounts.getUser("admin")).thenReturn(null);
        assertNull(checked.getUser(token));
        assertNull(checked.getUser(token));
    }

    private void atMinute(long minutes) {
        when(clock.instant()).thenReturn(LOGIN_TIME.plusSeconds(minutes * 60));
    }
}
