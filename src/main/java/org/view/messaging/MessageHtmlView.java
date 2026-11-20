package org.view.messaging;

import org.model.Agent;
import org.model.messaging.Message;
import org.view.HtmlPage;

import java.util.List;
import java.util.Objects;

/**
 * Renders the messaging pages for the web interface.
 *
 * This class only creates HTML. Authentication, message retrieval,
 * sending and marking messages as read belong to the controller/service.
 * The controller wraps the returned HTML using HtmlPage.page().
 */
public final class MessageHtmlView {

    private MessageHtmlView() {
        // Utility class: use the static rendering methods
    }

    /** Renders the authenticated user's inbox without changing read status. */
    public static String inbox(List<Message> messages) {
        Objects.requireNonNull(messages, "Messages cannot be null.");

        StringBuilder html = new StringBuilder();

        html.append("<section class=\"message-center\">\n")
            .append("<p><a href=\"/messages/new\">Compose message</a></p>\n");

        if (messages.isEmpty()) {
            html.append("<p>You have no messages.</p>\n")
                .append("</section>\n");

            return html.toString();
        }

        html.append("<ul class=\"message-list\">\n");

        for (Message message : messages) {
            String status = message.isRead() ? "Read" : "Unread";

            String cssClass = message.isRead()
                    ? "message-item--read"
                    : "message-item--unread";

            html.append("<li class=\"message-item ")
                .append(cssClass)
                .append("\">\n")
                .append("<a href=\"/messages/")
                .append(message.getId())
                .append("\">")
                .append("From: ")
                .append(HtmlPage.escape(message.getSenderUsername()))
                .append("</a> ")
                .append("<span class=\"message-status\">")
                .append(status)
                .append("</span>\n")
                .append("<small>Sent: ")
                .append(HtmlPage.escape(message.getSentAt().toString()))
                .append("</small>\n")
                .append("</li>\n");
        }

        html.append("</ul>\n</section>\n");

        return html.toString();
    }

    /** Renders the details of a message the controller has authorized. */
    public static String messageDetail(Message message) {
        Objects.requireNonNull(message, "Message cannot be null.");

        return new StringBuilder()
                .append("<article class=\"message-detail\">\n")
                .append("<p><strong>From:</strong> ")
                .append(HtmlPage.escape(message.getSenderUsername()))
                .append("</p>\n")
                .append("<p><strong>To:</strong> ")
                .append(HtmlPage.escape(message.getRecipientName()))
                .append("</p>\n")
                .append("<p><strong>Sent:</strong> ")
                .append(HtmlPage.escape(message.getSentAt().toString()))
                .append("</p>\n")
                .append("<pre class=\"message-body\">")
                .append(HtmlPage.escape(message.getBody()))
                .append("</pre>\n")
                .append("<p><a href=\"/messages\">Back to inbox</a></p>\n")
                .append("</article>\n")
                .toString();
    }

    /** Renders a blank form for composing a new message. */
    public static String composeForm(List<Agent> agents) {
        return composeForm(agents, null, null, "");
    }

    /**
     * Renders the compose form after validation fails, preserving the draft.
     * The controller still validates the submitted agent ID and message body.
     */
    public static String composeForm(
            List<Agent> agents,
            String error,
            Integer selectedAgentId,
            String draftBody) {

        Objects.requireNonNull(agents, "Agents cannot be null.");

        StringBuilder html = new StringBuilder(
                "<section class=\"message-compose\">\n"
        );

        // Display an error message if provided.
        if (error != null && !error.isBlank()) {
            html.append("<p role=\"alert\">")
                .append(HtmlPage.escape(error))
                .append("</p>\n");
        }

        // Cannot compose messages when no agents exist.
        if (agents.isEmpty()) {
            return html.append("<p>No agents are available to message.</p>\n")
                    .append("<p><a href=\"/messages\">Back to inbox</a></p>\n")
                    .append("</section>\n")
                    .toString();
        }

        html.append("<form method=\"post\" action=\"/messages\">\n")
            .append("<p><label for=\"recipientAgentId\">Recipient:</label>\n")
            .append("<select id=\"recipientAgentId\" ")
            .append("name=\"recipientAgentId\" required>\n")
            .append("<option value=\"\">Select an agent</option>\n");

        // Populate the recipient dropdown.
        for (Agent agent : agents) {
            html.append("<option value=\"")
                .append(agent.getId())
                .append("\"");

            if (selectedAgentId != null && selectedAgentId == agent.getId()) {
                html.append(" selected");
            }

            html.append(">")
                .append(HtmlPage.escape(agent.getName()))
                .append("</option>\n");
        }

        html.append("</select></p>\n")
            .append("<p><label for=\"body\">Message:</label>\n")
            .append("<textarea id=\"body\" name=\"body\" ")
            .append("rows=\"8\" required>")
            .append(HtmlPage.escape(draftBody))
            .append("</textarea></p>\n")
            .append("<p><button type=\"submit\">Send message</button></p>\n")
            .append("</form>\n")
            .append("<p><a href=\"/messages\">Back to inbox</a></p>\n")
            .append("</section>\n");

        return html.toString();
    }
}