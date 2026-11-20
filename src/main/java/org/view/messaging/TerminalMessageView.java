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
        if (messages.isEmpty()) {
            output.println("You have no messages.");
        }
        for (Message message : messages) {
            output.println("From: " + message.getSenderUsername()
                    + " | To: " + message.getRecipientName()
                    + " | Sent: " + message.getSentAt()
                    + "\n" + message.getBody() + "\n---");
        }
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
