package org.view.messaging;

import org.model.Agent;
import org.model.messaging.Message;

import java.io.PrintStream;
import java.util.List;

/** Plain terminal rendering. Message bodies are kept out of public audit history. */
public final class TerminalMessageView implements MessageView {

    private final PrintStream output;

    public TerminalMessageView(PrintStream output) {
        if (output == null) {
            throw new IllegalArgumentException("Output required.");
        }
        this.output = output;
    }

    @Override
    public void showAgents(List<Agent> agents) {
        if (agents.isEmpty()) {
            output.println("No agents are available.");
        }
        for (Agent agent : agents) {
            output.println(agent.getId() + ". " + agent.getName());
        }
    }

    @Override
    public void showInbox(List<Message> messages) {
        output.println("\n=== My Messages ===");

        if (messages.isEmpty()) {
            output.println("You have no messages.");
            return;
        }

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);

            String status = message.isRead() ? "Read" : "Unread";

            output.printf(
                    "%d. [%s] From: %s | Sent: %s%n",
                    i + 1,
                    status,
                    message.getSenderUsername(),
                    message.getSentAt()
            );
        }
    }

    @Override
    public void showMessageDetail(Message message) {
        output.println("\n=== Message Details ===");
        output.println("From: " + message.getSenderUsername());
        output.println("To: " + message.getRecipientName());
        output.println("Sent: " + message.getSentAt());
        output.println("-----------------");
        output.println(message.getBody());
        output.println("-----------------");
        output.println("Status: Read");
    }

    @Override
    public void showMessageSent(Message message) {
        output.println("Message sent to " + message.getRecipientName() + ".");
    }

    @Override
    public void showError(String message) {
        output.println("Unable to send message: " + message);
    }
}
