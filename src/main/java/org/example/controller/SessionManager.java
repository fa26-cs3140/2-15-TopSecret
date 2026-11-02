package org.example.controller;

import org.example.UserAccount;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

// Keeps logged-in web sessions in memory, keyed by a random cookie token.
public class SessionManager {

    public static final String COOKIE_NAME = "session";

    private final Map<String, UserAccount> sessions = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    // Returns a new random token for the user.
    public String createSession(UserAccount user) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    // Returns the user for a token, or null if the token is unknown.
    public UserAccount getUser(String token) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    public void endSession(String token) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    // Set-Cookie value with no Expires/Max-Age, so the browser drops it when closed.
    public static String cookieHeader(String token) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    // Finds the session token in a Cookie request header, or returns null.
    public static String tokenFromCookieHeader(String cookieHeader) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }
}
