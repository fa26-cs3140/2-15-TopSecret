package org.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.managers.UserAccountManager;
import org.model.UserAccount;
import org.view.HtmlPage;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
        if (!"POST".equals(exchange.getRequestMethod())) {
            String token = SessionManager.tokenFromCookieHeader(exchange.getRequestHeaders().getFirst("Cookie"));
            if (sessions.getUser(token) != null) {
                WebServer.redirect(exchange, "/menu");
            } else {
                WebServer.send(exchange, 200, HtmlPage.page("Log in", HtmlPage.loginForm(null), false, false));
            }
            return;
        }

        Map<String, String> form = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String username = form.getOrDefault("username", "").trim();
        String password = form.getOrDefault("password", "");
        UserAccount user = accounts.verifyLogin(username, password) ? accounts.getUser(username) : null;

        if (user == null) {
            String body = HtmlPage.loginForm("Incorrect username or password.");
            WebServer.send(exchange, 200, HtmlPage.page("Log in", body, false, false));
            return;
        }
        exchange.getResponseHeaders().add("Set-Cookie", SessionManager.cookieHeader(sessions.createSession(user)));
        WebServer.redirect(exchange, "/menu");
    }

    // Parses an application/x-www-form-urlencoded body such as "username=pat&password=Secret1".
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
