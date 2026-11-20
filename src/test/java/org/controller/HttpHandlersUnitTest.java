package org.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.managers.*;
import org.model.*;
import org.model.messaging.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises HTTP routing without a listening socket or database. */
class HttpHandlersUnitTest {
    private final UserAccount admin = new UserAccount("pat", true);

    private HttpExchange request(String method, String path, String form) {
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestURI()).thenReturn(URI.create(path));
        when(exchange.getRequestMethod()).thenReturn(method);
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(form.getBytes(StandardCharsets.UTF_8)));
        when(exchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        return exchange;
    }

    private String body(HttpExchange exchange) throws java.io.IOException {
        return exchange.getResponseBody().toString();
    }

    @Test
    void loginRejectsBadCredentialsAndBindsSuccessfulCookieToVerifiedUser() throws Exception {
        var accounts = mock(UserAccountManager.class);
        var sessions = mock(SessionManager.class);
        var handler = new LoginHandler(accounts, sessions);
        var anonymous = request("GET", "/", "");
        handler.handle(anonymous);
        assertTrue(body(anonymous).contains("type=\"password\""));
        var invalid = request("POST", "/login", "username=pat&password=bad");
        handler.handle(invalid);
        assertTrue(body(invalid).contains("Incorrect username or password"));
        assertNull(invalid.getResponseHeaders().getFirst("Set-Cookie"));
        verify(sessions, never()).createSession(any());

        when(accounts.verifyLogin("pat", "Secret1")).thenReturn(true);
        when(accounts.getUser("pat")).thenReturn(admin);
        when(sessions.createSession(admin)).thenReturn("verified-token");
        var valid = request("POST", "/login", "username=+pat+&password=Secret1");
        handler.handle(valid);
        assertTrue(valid.getResponseHeaders().getFirst("Set-Cookie").startsWith("session=verified-token;"));
        assertEquals("/menu", valid.getResponseHeaders().getFirst("Location"));
        verify(valid).sendResponseHeaders(303, -1);
    }

    @Test
    void navigationUsesSessionIdentityAndAuditsOnlyExistingDetails() throws Exception {
        var controller = mock(AppController.class);
        var sessions = mock(SessionManager.class);
        when(sessions.getUser("token")).thenReturn(admin);
        when(controller.canEdit(admin)).thenReturn(true);
        when(controller.listRecords(RecordType.BRIEF)).thenReturn("7. O'Brien\nNo identifier");
        when(controller.formValues(RecordType.BRIEF, 7)).thenReturn(Map.of("title", "O'Brien"));
        when(controller.readRecord(RecordType.BRIEF, 7, admin)).thenReturn("Title: O'Brien");
        when(controller.relatedRecords(RecordType.BRIEF, 7)).thenReturn(List.of());
        when(controller.formValues(RecordType.BRIEF, 99)).thenReturn(null);
        var handler = new PageHandler(controller, sessions, null);
        for (String path : List.of("/menu", "/briefs", "/briefs/7", "/briefs/99", "/briefs/abc", "/unknown")) {
            var exchange = request("GET", path, "");
            exchange.getRequestHeaders().set("Cookie", "session=token");
            handler.handle(exchange);
            if (path.equals("/briefs")) assertTrue(body(exchange).contains("href=\"/briefs/7\""));
            if (path.equals("/briefs/7")) assertTrue(body(exchange).contains("Edit brief"));
            if (path.equals("/briefs/99")) verify(exchange).sendResponseHeaders(eq(404), anyLong());
        }
        verify(controller).readRecord(RecordType.BRIEF, 7, admin);
        verify(controller, never()).readRecord(RecordType.BRIEF, 99, admin);
        var logout = request("GET", "/logout", "");
        logout.getRequestHeaders().set("Cookie", "session=token");
        handler.handle(logout);
        verify(sessions).endSession("token");
        assertEquals("/", logout.getResponseHeaders().getFirst("Location"));
        var anonymous = request("GET", "/briefs", "");
        handler.handle(anonymous);
        verify(anonymous).sendResponseHeaders(303, -1);
    }

    @Test
    void editWorkflowPrefillsValuesRedirectsOnSaveAndPreservesInvalidInput() throws Exception {
        var controller = mock(AppController.class);
        when(controller.canEdit(admin)).thenReturn(true);
        when(controller.formValues(RecordType.BRIEF, 7)).thenReturn(Map.of("title", "Current"));
        when(controller.createRecord(eq(RecordType.BRIEF), anyMap(), eq(admin))).thenReturn(8);
        var handler = new EditHandler(controller);
        var edit = request("GET", "/briefs/7/edit", "");
        handler.handle(edit, admin);
        assertTrue(body(edit).contains("Current"));
        var create = request("POST", "/briefs/new", "title=New&date=2020-01-01&text=Body");
        handler.handle(create, admin);
        assertEquals("/briefs/8", create.getResponseHeaders().getFirst("Location"));
        var update = request("POST", "/briefs/7/edit", "title=Updated");
        handler.handle(update, admin);
        verify(controller).updateRecord(RecordType.BRIEF, 7, Map.of("title", "Updated"), admin);
        when(controller.createRecord(eq(RecordType.BRIEF), anyMap(), eq(admin)))
                .thenThrow(new IllegalArgumentException("Invalid date"));
        var invalid = request("POST", "/briefs/new", "title=Keep+this");
        handler.handle(invalid, admin);
        assertTrue(body(invalid).contains("Keep this"));
        assertTrue(body(invalid).contains("Invalid date"));
    }

    @Test
    void messagingRoutesUseRecipientIdentityAndReturnSafeErrors() throws Exception {
        var users = mock(UserAccountStore.class);
        var agents = mock(AgentManager.class);
        var store = mock(MessageStore.class);
        when(users.getUser("pat")).thenReturn(admin);
        when(users.getUsers()).thenReturn(List.of(admin));
        when(agents.getRecords("id", "%")).thenReturn(List.of(new Agent(2, "Ghost", "1990-01-01", null, null, 1)));
        Message message = new Message(7, "sender", 2, "pat", "<secret>", LocalDateTime.of(2026, 11, 20, 12, 0), true);
        when(store.findForRecipient("pat")).thenReturn(List.of(message));
        when(store.markAsRead(7, "pat")).thenReturn(message);
        var handler = new MessageWebHandler(agents, users, store);
        for (String path : List.of("/messages?sent=1", "/messages/new", "/messages/7", "/messages/bad")) {
            var exchange = request("GET", path, "");
            handler.handle(exchange, admin);
            if (path.equals("/messages/7")) {
                assertTrue(body(exchange).contains("&lt;secret&gt;"));
                verify(store).markAsRead(7, "pat");
            }
        }
        var send = request("POST", "/messages", "recipientAgentId=2&body=Hello&sender_username=forged");
        handler.handle(send, admin);
        verify(store).save(argThat(m -> m.getSenderUsername().equals("pat") && m.getBody().equals("Hello")));
        assertEquals("/messages?sent=1", send.getResponseHeaders().getFirst("Location"));
        var invalid = request("POST", "/messages", "recipientAgentId=999&body=Keep+this");
        handler.handle(invalid, admin);
        verify(invalid).sendResponseHeaders(eq(400), anyLong());
        assertTrue(body(invalid).contains("Keep this"));
        var unsupported = request("DELETE", "/messages", "");
        handler.handle(unsupported, admin);
        verify(unsupported).sendResponseHeaders(eq(405), anyLong());
        when(store.findForRecipient("pat")).thenThrow(new IllegalStateException("private database path"));
        var unavailable = request("GET", "/messages", "");
        handler.handle(unavailable, admin);
        verify(unavailable).sendResponseHeaders(eq(500), anyLong());
        assertFalse(body(unavailable).contains("private database path"));
    }
    @Test
    void serverRegistersSafeRoutesWithoutOpeningSockets() throws Exception {
        var accounts = mock(UserAccountManager.class);
        when(accounts.hasUsers()).thenReturn(true);
        var controller = mock(AppController.class);
        var sessions = mock(SessionManager.class);
        when(sessions.getUser("token")).thenReturn(admin);
        var transport = mock(com.sun.net.httpserver.HttpServer.class);
        var routes = new java.util.HashMap<String, com.sun.net.httpserver.HttpHandler>();
        when(transport.createContext(anyString(), any())).thenAnswer(invocation -> {
            routes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        });
        try (var factory = mockStatic(com.sun.net.httpserver.HttpServer.class)) {
            factory.when(() -> com.sun.net.httpserver.HttpServer.create(any(java.net.InetSocketAddress.class), eq(0)))
                    .thenReturn(transport);
            var server = new WebServer(controller, accounts, sessions, 0);
            try {
                server.start();
                verify(transport).setExecutor(any(java.util.concurrent.Executor.class));
                verify(transport).start();
                var login = request("GET", "/login", "");
                routes.get("/login").handle(login);
                assertTrue(body(login).contains("password"));
                var root = request("GET", "/", "");
                routes.get("/").handle(root);
                verify(root).sendResponseHeaders(eq(200), anyLong());
                var css = request("GET", "/css/style.css", "");
                routes.get("/css/").handle(css);
                assertTrue(css.getResponseHeaders().getFirst("Content-Type").startsWith("text/css"));
                assertFalse(body(css).isEmpty());
                var menu = request("GET", "/menu", "");
                menu.getRequestHeaders().set("Cookie", "session=token");
                routes.get("/").handle(menu);
                assertTrue(body(menu).contains("Main Menu"));
                var malformed = request("POST", "/login", "username=%GG");
                routes.get("/login").handle(malformed);
                verify(malformed).sendResponseHeaders(eq(400), anyLong());
                var oversized = request("POST", "/login", "x".repeat(65_537));
                routes.get("/login").handle(oversized);
                verify(oversized).sendResponseHeaders(eq(400), anyLong());
                var foreign = request("POST", "/login", "");
                foreign.getRequestHeaders().set("Origin", "http://foreign.example");
                routes.get("/login").handle(foreign);
                verify(foreign).sendResponseHeaders(eq(403), anyLong());
                when(controller.listRecords(RecordType.BRIEF)).thenThrow(new IllegalStateException("private database path"));
                var failed = request("GET", "/briefs", "");
                failed.getRequestHeaders().set("Cookie", "session=token");
                routes.get("/").handle(failed);
                verify(failed).sendResponseHeaders(eq(503), anyLong());
                assertFalse(body(failed).contains("private database path"));
            } finally {
                server.stop();
            }
            verify(transport).stop(0);
        }
    }

}
