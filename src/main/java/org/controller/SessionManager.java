package org.controller;

import org.model.UserAccount;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Keeps logged-in web sessions in memory, keyed by a random cookie token.
public class SessionManager {

    public static final String COOKIE_NAME = "session";

    private final Map<String, UserAccount> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    // Returns a new random token for the user.
    public String createSession(UserAccount user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, user);
        return token;
    }

    // Returns the user for a token, or null if the token is unknown.
    public UserAccount getUser(String token) {
        return token == null ? null : sessions.get(token);
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
