package org.example.view.messaging;

import org.example.model.Agent;
import org.example.model.messaging.Message;
import java.util.List;

/** Terminal rendering boundary. Implement the actual prompts/rendering in Week 2.
 * No web messaging routes or HTML message view are required by this homework.
 */
public interface MessageView {
    void showAgents(List<Agent> agents);
    void showInbox(List<Message> messages);
    void showMessageSent(Message message);
    void showError(String message);
}
