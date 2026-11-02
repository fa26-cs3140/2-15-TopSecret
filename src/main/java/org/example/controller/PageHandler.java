package org.example.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

// Serves /menu, /briefs, /agents, /facilities, /audit and /logout for logged-in users.
// Requests without a valid session cookie are redirected to the login page.
public class PageHandler implements HttpHandler {

    private final AppController controller;
    private final SessionManager sessions;

    public PageHandler(AppController controller, SessionManager sessions) {
        if (controller == null || sessions == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.controller = controller;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }
}
