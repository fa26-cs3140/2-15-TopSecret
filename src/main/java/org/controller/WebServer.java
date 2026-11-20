package org.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.managers.UserAccountManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// HTTP server for authenticated record viewing and editing.
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
        server.createContext("/login", exchange -> handleSafely(exchange, login));
        server.createContext("/css/", exchange -> serveStaticResource(exchange));
        HttpHandler navigation = exchange -> {
            if (exchange.getRequestURI().getPath().equals("/")) {
                login.handle(exchange);
            } else {
                pages.handle(exchange);
            }
        };
        server.createContext("/", exchange -> handleSafely(exchange, navigation));
        server.start();
    }

    // Quick thing to load the css file; we gotta make a context for it.
    // Was really confused but it seems simple enough
    private static void serveStaticResource(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Only allow requests for CSS resources.
        if (!path.startsWith("/css/") || path.contains("..")) {
            send(exchange, 404, "<p>Resource not found.</p>");
            return;
        }

        String resourcePath = "static" + path;

        try (var input = WebServer.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (input == null) {
                send(exchange, 404, "<p>Resource not found.</p>");
                return;
            }
            byte[] bytes = input.readAllBytes();

            exchange.getResponseHeaders()
                .set( "Content-Type", "text/css; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }

    private static void handleSafely(HttpExchange exchange, HttpHandler handler) throws IOException {
        try {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String origin = exchange.getRequestHeaders().getFirst("Origin");
                String site = exchange.getRequestHeaders().getFirst("Sec-Fetch-Site");
                String expectedOrigin = "http://" + exchange.getRequestHeaders().getFirst("Host");
                if ("cross-site".equalsIgnoreCase(site)
                        || (origin != null && !origin.equalsIgnoreCase(expectedOrigin))) {
                    send(exchange, 403, "<p>Cross-origin form submissions are not allowed.</p>");
                    return;
                }
            }
            handler.handle(exchange);
        } catch (IllegalArgumentException malformed) {
            send(exchange, 400, "<p>Invalid request data.</p>");
        } catch (IllegalStateException unavailable) {
            send(exchange, 503, "<p>The requested data is temporarily unavailable.</p>");
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return server == null ? port : server.getAddress().getPort();
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
