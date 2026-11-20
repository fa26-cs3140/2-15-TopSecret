package org.controller;

import com.sun.net.httpserver.HttpExchange;

// managers
import org.managers.AgentManager;
import org.managers.UserAccountStore;
// models
import org.model.UserAccount;
import org.model.messaging.AuthenticatedMessageService;
import org.model.messaging.Message;
import org.model.messaging.MessageService;
import org.model.messaging.MessageStore;
// views
import org.view.HtmlPage;
import org.view.messaging.MessageHtmlView;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Handles web requests for the messaging center.
 *
 * PageHandler authenticates the user before passing the request here.
 * A separate MessageService is created for each authenticated user.
 */
public final class MessageWebHandler {

    private final AgentManager agents;
    private final UserAccountStore users;
    private final MessageStore store;

    public MessageWebHandler(AgentManager agents, UserAccountStore users, MessageStore store) {
        this.agents = Objects.requireNonNull(agents);
        this.users = Objects.requireNonNull(users);
        this.store = Objects.requireNonNull(store);
    }

    /** Routes a request using the authenticated user's identity. */
    public void handle(HttpExchange exchange, UserAccount user) throws IOException {
        if (user == null) {
            WebServer.redirect(exchange, "/");
            return;
        }

        MessageService service;

        try {
            service = new AuthenticatedMessageService(agents, users, store, user);
        } catch (IllegalArgumentException e) {
            send(exchange, user, 403, "Access denied", "<p>Messaging account unavailable.</p>");
            return;
        }

        try {
            route(exchange, user, service);
        } catch (IllegalStateException e) {
            send(exchange, user, 500, "Messaging error", "<p>Messaging is temporarily unavailable.</p>");
        }
    }

    /** Determines which messaging operation was requested. */
    private void route(HttpExchange exchange, UserAccount user, MessageService service) throws IOException {

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/messages".equals(path)) {
            if ("GET".equals(method)) {
                showInbox(exchange, user, service);
            } else if ("POST".equals(method)) {
                sendMessage(exchange, user, service);
            } else {
                methodNotAllowed(exchange, user);
            }
            return;
        }

        if ("/messages/new".equals(path)) {
            if ("GET".equals(method)) {
                showComposeForm(exchange, user, service);
            } else {
                methodNotAllowed(exchange, user);
            }
            return;
        }

        if (!path.matches("/messages/[0-9]+")) {
            send(exchange, user, 404, "Not Found",
                    "<p>Message not found.</p>");
            return;
        }

        if (!"GET".equals(method)) {
            methodNotAllowed(exchange, user);
            return;
        }

        // Remaining valid path: /messages/{id}
        try {
            long messageId = Long.parseLong(path.substring("/messages/".length()));
            showMessage(exchange, user, service, messageId);
        } catch (IllegalArgumentException e) {
            send(exchange, user, 404, "Not Found", "<p>Message not found.</p>");
        }
    }

    /** Displays the user's inbox without modifying read status. */
    private void showInbox(HttpExchange exchange, UserAccount user, MessageService service) throws IOException {

        String notice = "sent=1".equals(exchange.getRequestURI().getRawQuery())
                ? "<p role=\"status\">Message sent.</p>"
                : "";

        String body = notice + MessageHtmlView.inbox(service.getInbox());

        send(exchange, user, 200, "My Messages", body);
    }

    /** Displays the form for composing a new message. */
    private void showComposeForm(HttpExchange exchange, UserAccount user, MessageService service) throws IOException {
        String body = MessageHtmlView.composeForm(service.getAgents());
        send(exchange, user, 200, "Compose Message", body);
    }

    /** Displays a message and marks it as read for its recipient. */
    private void showMessage(HttpExchange exchange, UserAccount user, MessageService service, long messageId) throws IOException {

        // Authorization and read-status update happen in the service/store.
        Message message = service.readMessage(messageId);

        String body = MessageHtmlView.messageDetail(message);

        send(exchange, user, 200, "Read Message", body);
    }

    /** Processes the compose form and persists a message. */
    private void sendMessage(HttpExchange exchange, UserAccount user, MessageService service) throws IOException {

        Map<String, String> form;

        try {
            form = WebServer.readForm(exchange);
        } catch (IllegalArgumentException e) {
            send(exchange, user, 400, "Compose Message", "<p>Invalid form submission.</p>");
            return;
        }

        String messageBody = form.getOrDefault("body", "");
        Integer agentId = null;

        try {
            agentId = Integer.valueOf(form.getOrDefault("recipientAgentId", ""));

            service.sendMessage(agentId, messageBody);

            // Redirect after POST to avoid duplicate submissions on refresh.
            WebServer.redirect(exchange, "/messages?sent=1");

        } catch (IllegalArgumentException e) {
            // Redisplay the form without losing the user's message.
            String body = MessageHtmlView.composeForm(
                    service.getAgents(),
                    e.getMessage(),
                    agentId,
                    messageBody
            );

            send(exchange, user, 400, "Compose Message", body);
        }
    }

    private void methodNotAllowed(HttpExchange exchange, UserAccount user) throws IOException {

        send(exchange, user, 405, "Method Not Allowed", "<p>Unsupported request method.</p>");
    }

    /** Wraps HTML fragments using the team's shared page template. */
    private void send(
            HttpExchange exchange,
            UserAccount user,
            int status,
            String title,
            String body) throws IOException {

        WebServer.send(
                exchange,
                status,
                HtmlPage.page(title, body, true, user.isAdmin())
        );
    }
}