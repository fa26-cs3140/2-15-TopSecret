package org.view.messaging;

import org.model.Agent;
import org.model.messaging.Message;
import java.util.List;

/** Terminal rendering boundary. TerminalMessageView supplies rendering; AgencyInterface owns prompts.
 * No web messaging routes or HTML message view are required by this homework.
 */
public interface MessageView {
    void showAgents(List<Agent> agents);
    void showInbox(List<Message> messages);
    void showMessageSent(Message message);
    void showError(String message);
}
