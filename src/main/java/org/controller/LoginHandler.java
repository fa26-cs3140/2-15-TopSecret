package org.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.managers.UserAccountManager;

import java.io.IOException;

// GET / shows the login form; POST /login checks credentials and sets the session cookie.
public class LoginHandler implements HttpHandler {

    private final UserAccountManager accounts;
    private final SessionManager sessions;

    public LoginHandler(UserAccountManager accounts, SessionManager sessions) {
        if (accounts == null || sessions == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.accounts = accounts;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }
}
