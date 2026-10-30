package org.example.model.messaging;

import java.time.LocalDateTime;

/** Immutable message snapshot. ID zero denotes a message not yet persisted. */
public final class Message {
    private final long id;
    private final String senderUsername;
    private final int recipientAgentId;
    private final String recipientName;
    private final String body;
    private final LocalDateTime sentAt;

    public Message(long id, String senderUsername, int recipientAgentId,
                   String recipientName, String body, LocalDateTime sentAt) {
        if (id < 0 || recipientAgentId <= 0) {
            throw new IllegalArgumentException("Invalid message or agent ID.");
        }
        if (senderUsername == null || senderUsername.isBlank()
                || recipientName == null || recipientName.isBlank()
                || body == null || body.isBlank() || sentAt == null) {
            throw new IllegalArgumentException("Message fields cannot be missing or blank.");
        }
        this.id = id;
        this.senderUsername = senderUsername;
        this.recipientAgentId = recipientAgentId;
        this.recipientName = recipientName;
        this.body = body;
        this.sentAt = sentAt;
    }

    public long getId() { return id; }
    public String getSenderUsername() { return senderUsername; }
    public int getRecipientAgentId() { return recipientAgentId; }
    public String getRecipientName() { return recipientName; }
    public String getBody() { return body; }
    public LocalDateTime getSentAt() { return sentAt; }
}
