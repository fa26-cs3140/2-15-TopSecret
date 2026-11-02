package org.example.controller;

import com.sun.net.httpserver.HttpServer;
import org.example.UserAccountManager;

import java.io.IOException;

// HTTP server for the read-only web view, started with the -web switch.
public class WebServer {

    public static final int DEFAULT_PORT = 3002;

    private final AppController controller;
    private final UserAccountManager accounts;
    private final SessionManager sessions;
    private final int port;
    private HttpServer server;

    public WebServer(AppController controller, UserAccountManager accounts, SessionManager sessions, int port) {
        if (controller == null || accounts == null || sessions == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.controller = controller;
        this.accounts = accounts;
        this.sessions = sessions;
        this.port = port;
    }

    // Registers LoginHandler at / and /login and PageHandler for the other pages.
    // Throws IllegalStateException when no user accounts exist.
    public void start() throws IOException {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    public void stop() {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    public int getPort() {
        return port;
    }
}
