package org.controller;

import org.model.UserAccount;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// Keeps logged-in web sessions in memory, keyed by a random cookie token.
public class SessionManager {

    public static final String COOKIE_NAME = "session";

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    private record Session(UserAccount user, Instant lastActivity) {
        boolean isExpired(Instant now) {
            return !now.isBefore(lastActivity.plus(IDLE_TIMEOUT));
        }
    }

    public record SessionSummary(String username, String token, Instant lastActivity) {
    }

    public SessionManager() {
        this(Clock.systemUTC());
    }

    SessionManager(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    // Returns a new random token for the user.
    public String createSession(UserAccount user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(user, clock.instant()));
        return token;
    }

    // Refreshes active sessions and rejects sessions idle for at least 30 minutes.
    public UserAccount getUser(String token) {
        if (token == null) {
            return null;
        }
        Session active = sessions.computeIfPresent(token, (key, session) -> {
            Instant now = clock.instant();
            return session.isExpired(now) ? null : new Session(session.user(), now);
        });
        return active == null ? null : active.user();
    }

    public List<SessionSummary> listSessions(UserAccount administrator) {
        requireAdministrator(administrator);
        Instant now = clock.instant();
        sessions.forEach((token, session) -> {
            if (session.isExpired(now)) {
                sessions.remove(token, session);
            }
        });
        return sessions.entrySet().stream()
                .map(entry -> new SessionSummary(
                        entry.getValue().user().getUsername(), entry.getKey(), entry.getValue().lastActivity()))
                .sorted(Comparator.comparing(SessionSummary::username).thenComparing(SessionSummary::token))
                .toList();
    }

    public void revokeSession(UserAccount administrator, String token) {
        requireAdministrator(administrator);
        endSession(token);
    }

    private static void requireAdministrator(UserAccount user) {
        if (user == null || !user.isAdmin()) {
            throw new SecurityException("Only administrators can manage web sessions.");
        }
    }

    public void endSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    // Set-Cookie value with no Expires/Max-Age, so the browser drops it when closed.
    public static String cookieHeader(String token) {
        return COOKIE_NAME + "=" + token + "; HttpOnly; Path=/";
    }

    // Finds the session token in a Cookie request header, or returns null.
    public static String tokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null) {
            return null;
        }
        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && parts[0].equals(COOKIE_NAME)) {
                return parts[1];
            }
        }
        return null;
    }
}
