package org.controller.messaging;

import org.model.messaging.MessageService;
import org.view.messaging.MessageView;

/** Messaging controller used by the authenticated terminal interface. The service must
 * already be bound to a verified session. There is deliberately no sender or
 * inbox-username parameter in any UI action. The application supplies the session-bound service and terminal view.
 */
public final class MessageController {
    private final MessageService service;
    private final MessageView view;

    public MessageController(MessageService service, MessageView view) {
        if (service == null || view == null) {
            throw new IllegalArgumentException("Message dependencies cannot be null.");
        }
        this.service = service;
        this.view = view;
    }

    public void listAgents() {
        view.showAgents(service.getAgents());
    }

    public void sendMessage(int recipientAgentId, String body) {
        try {
            view.showMessageSent(service.sendMessage(recipientAgentId, body));
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
        }
    }

    public void showInbox() {
        view.showInbox(service.getInbox());
    }
}
