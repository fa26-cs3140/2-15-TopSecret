package org.model.messaging;

import java.time.LocalDateTime;

/** Immutable message snapshot. ID zero denotes a message not yet persisted. */
public final class Message {
    private final long id;
    private final String senderUsername;
    private final int recipientAgentId;
    private final String recipientName;
    private final String body;
    private final LocalDateTime sentAt;
    private final boolean read;

    /** New messages are unread by default; preserves the original constructor. */
    public Message(long id, String senderUsername, int recipientAgentId,
                   String recipientName, String body, LocalDateTime sentAt) {
        this(id, senderUsername, recipientAgentId, recipientName, body, sentAt, false);
    }

    public Message(long id, String senderUsername, int recipientAgentId,
                   String recipientName, String body, LocalDateTime sentAt, boolean read) {
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
        this.read = read;
    }

    public long getId() { return id; }
    public String getSenderUsername() { return senderUsername; }
    public int getRecipientAgentId() { return recipientAgentId; }
    public String getRecipientName() { return recipientName; }
    public String getBody() { return body; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isRead() { return read; }
}
