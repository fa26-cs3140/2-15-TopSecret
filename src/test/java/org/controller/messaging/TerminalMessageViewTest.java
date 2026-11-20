package org.controller.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.model.Agent;
import org.model.messaging.Message;
import org.view.messaging.TerminalMessageView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TerminalMessageViewTest {

    private ByteArrayOutputStream output;
    private TerminalMessageView view;

    private final LocalDateTime timestamp = LocalDateTime.of(2026, 11, 20, 10, 30);

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        view = new TerminalMessageView(new PrintStream(output));
    }

    @Test
    void constructorRejectsNullOutput() {
        assertThrows(IllegalArgumentException.class, () -> new TerminalMessageView(null));
    }

    @Test
    void displaysAvailableAgents() {
        Agent ghost = new Agent(
                1, "Ghost", "1990-01-01", null, null, 1
        );

        Agent smith = new Agent(
                2, "Smith", "1990-01-01", null, null, 1
        );

        view.showAgents(List.of(ghost, smith));

        String text = output.toString();

        assertTrue(text.contains("1. Ghost"));
        assertTrue(text.contains("2. Smith"));
    }

    @Test
    void displaysEmptyAgentList() {
        view.showAgents(List.of());

        assertTrue(output.toString().contains("No agents are available."));
    }

    @Test
    void displaysEmptyInbox() {
        view.showInbox(List.of());

        String text = output.toString();

        assertTrue(text.contains("My Messages"));
        assertTrue(text.contains("You have no messages."));
    }

    @Test
    void displaysReadAndUnreadIndicators() {
        Message unread = new Message(
                5, "Ghost", 1, "sender",
                "Secret mission", timestamp, false
        );

        Message read = new Message(
                6, "Admin", 1, "sender",
                "Mission complete", timestamp, true
        );

        view.showInbox(List.of(unread, read));

        String text = output.toString();

        assertTrue(text.contains("1. [Unread] From: Ghost"));
        assertTrue(text.contains("2. [Read] From: Admin"));

        // Inbox displays metadata, not message contents.
        assertFalse(text.contains("Secret mission"));
        assertFalse(text.contains("Mission complete"));
    }

    @Test
    void displaysSelectedMessageDetails() {
        Message message = new Message(
                5,
                "Ghost",
                1,
                "sender",
                "Meet at HQ tomorrow",
                timestamp,
                true
        );

        view.showMessageDetail(message);

        String text = output.toString();

        assertTrue(text.contains("Message Details"));
        assertTrue(text.contains("From: Ghost"));
        assertTrue(text.contains("To: sender"));
        assertTrue(text.contains("Meet at HQ tomorrow"));
        assertTrue(text.contains("Status: Read"));
    }

    @Test
    void confirmsSuccessfullySentMessage() {
        Message message = new Message(
                5,
                "sender",
                1,
                "Ghost",
                "Hello",
                timestamp
        );

        view.showMessageSent(message);

        assertTrue(output.toString().contains(
                "Message sent to Ghost."
        ));
    }

    @Test
    void displaysMessagingErrors() {
        view.showError("No such agent.");

        String text = output.toString();

        assertTrue(text.contains("Unable to send message:"));
        assertTrue(text.contains("No such agent."));
    }
}