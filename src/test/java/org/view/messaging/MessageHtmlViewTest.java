
package org.view.messaging;

import org.junit.jupiter.api.Test;
import org.model.Agent;
import org.model.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageHtmlViewTest {

    private final LocalDateTime timestamp =
            LocalDateTime.of(2026, 11, 20, 9, 30);

    @Test
    void inboxDisplaysReadAndUnreadMessages() {
        Message unread = new Message(
                1,
                "sender",
                1,
                "Ghost",
                "Hello",
                timestamp,
                false
        );

        Message read = new Message(
                2,
                "admin",
                1,
                "Ghost",
                "Goodbye",
                timestamp,
                true
        );

        String html = MessageHtmlView.inbox(List.of(unread, read));

        assertTrue(html.contains("From: sender"));
        assertTrue(html.contains("From: admin"));
        assertTrue(html.contains("message-item--unread"));
        assertTrue(html.contains("message-item--read"));
        assertTrue(html.contains("href=\"/messages/1\""));
        assertTrue(html.contains("href=\"/messages/2\""));

        // Inbox should not reveal full message bodies
        assertFalse(html.contains("Hello"));
        assertFalse(html.contains("Goodbye"));
    }

    @Test
    void emptyInboxDisplaysHelpfulMessage() {
        String html = MessageHtmlView.inbox(List.of());

        assertTrue(html.contains("You have no messages."));
        assertTrue(html.contains("href=\"/messages/new\""));
    }

    @Test
    void messageDetailDisplaysAndEscapesContent() {
        Message message = new Message(
                1,
                "sender",
                1,
                "Ghost",
                "Line one\n<script>alert('hack')</script>",
                timestamp,
                true
        );

        String html = MessageHtmlView.messageDetail(message);

        assertTrue(html.contains("From:</strong> sender"));
        assertTrue(html.contains("To:</strong> Ghost"));
        assertTrue(html.contains("Line one\n"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("href=\"/messages\""));
    }

    @Test
    void composeFormDisplaysAgentDropdown() {
        Agent ghost = new Agent(
                1,
                "Ghost",
                "1990-01-01",
                null,
                null,
                1
        );

        Agent smith = new Agent(
                2,
                "Smith",
                "1990-01-01",
                null,
                null,
                1
        );

        String html = MessageHtmlView.composeForm(
                List.of(ghost, smith)
        );

        assertTrue(html.contains("<form method=\"post\" action=\"/messages\">"));
        assertTrue(html.contains("<option value=\"1\">Ghost</option>"));
        assertTrue(html.contains("<option value=\"2\">Smith</option>"));
        assertTrue(html.contains("name=\"body\""));
        assertTrue(html.contains("Send message"));
    }

    @Test
    void composeFormPreservesDraftAndEscapesInput() {
        Agent ghost = new Agent(
                1,
                "Ghost",
                "1990-01-01",
                null,
                null,
                1
        );

        String html = MessageHtmlView.composeForm(
                List.of(ghost),
                "Invalid <message>",
                1,
                "Hello <script>"
        );

        assertTrue(html.contains("Invalid &lt;message&gt;"));
        assertTrue(html.contains("value=\"1\" selected"));
        assertTrue(html.contains("Hello &lt;script&gt;"));
        assertFalse(html.contains("Hello <script>"));
    }

    @Test
    void noAgentsDisablesComposing() {
        String html = MessageHtmlView.composeForm(List.of());

        assertTrue(html.contains("No agents are available"));
        assertFalse(html.contains("<form"));
    }
}
