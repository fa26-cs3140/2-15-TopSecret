package org.example.controller;

import org.example.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("HW6 week 1 draft: enable when SessionManager is implemented in week 2")
class SessionManagerTest {

    private SessionManager sessions;
    private final UserAccount pat = new UserAccount("pat", true);

    @BeforeEach
    void setUp() {
        sessions = new SessionManager();
    }

    @Test
    void createdSessionReturnsItsUser() {
        String token = sessions.createSession(pat);

        assertSame(pat, sessions.getUser(token));
    }

    @Test
    void tokensAreUniqueAndHardToGuess() {
        String first = sessions.createSession(pat);
        String second = sessions.createSession(pat);

        assertNotEquals(first, second);
        assertTrue(first.length() >= 20);
    }

    @Test
    void unknownOrNullTokenHasNoUser() {
        assertNull(sessions.getUser("not-a-token"));
        assertNull(sessions.getUser(null));
    }

    @Test
    void endedSessionHasNoUser() {
        String token = sessions.createSession(pat);

        sessions.endSession(token);

        assertNull(sessions.getUser(token));
    }

    @Test
    void createSessionRejectsNullUser() {
        assertThrows(IllegalArgumentException.class, () -> sessions.createSession(null));
    }

    @Test
    void cookieHeaderIsASessionCookie() {
        String header = SessionManager.cookieHeader("abc123");

        assertTrue(header.startsWith("session=abc123"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("Path=/"));
        assertFalse(header.contains("Expires"));
        assertFalse(header.contains("Max-Age"));
    }

    @Test
    void tokenIsFoundInCookieHeader() {
        assertEquals("abc123", SessionManager.tokenFromCookieHeader("session=abc123"));
        assertEquals("abc123", SessionManager.tokenFromCookieHeader("theme=dark; session=abc123; x=y"));
    }

    @Test
    void missingSessionCookieGivesNull() {
        assertNull(SessionManager.tokenFromCookieHeader(null));
        assertNull(SessionManager.tokenFromCookieHeader(""));
        assertNull(SessionManager.tokenFromCookieHeader("theme=dark"));
    }
}
