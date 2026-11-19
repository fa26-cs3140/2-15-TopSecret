package org.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.managers.UserAccountManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

    // Serves the login form at / and /login and every other page through PageHandler.
    // Throws IllegalStateException when no user accounts exist.
    public void start() throws IOException {
        if (!accounts.hasUsers()) {
            throw new IllegalStateException("No user accounts exist. Create the first account in the terminal.");
        }
        LoginHandler login = new LoginHandler(accounts, sessions);
        PageHandler pages = new PageHandler(controller, sessions);

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/login", login);
        server.createContext("/", exchange -> {
            if (exchange.getRequestURI().getPath().equals("/")) {
                login.handle(exchange);
            } else {
                pages.handle(exchange);
            }
        });
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return port;
    }

    static void send(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    // 303 See Other, so the browser follows with a GET.
    static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    // Reads and decodes an application/x-www-form-urlencoded request body.
    static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        return parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    // "a=1&b=x+y" -> {a=1, b=x y}; pairs without '=' are ignored.
    static Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return values;
    }
}
