package org.example;

import java.time.LocalDateTime;

/**
 * Represents one audit-log record.
 */
public class AuditLogEntry {

    private int id;
    private String username;
    private String action;
    private String contentType;
    private String contentIdentifier;
    private LocalDateTime timestamp;

    public AuditLogEntry(
            int id,
            String username,
            String action,
            String contentType,
            String contentIdentifier,
            LocalDateTime timestamp
    ) {
        this.id = id;
        this.username = username;
        this.action = action;
        this.contentType = contentType;
        this.contentIdentifier = contentIdentifier;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentIdentifier() {
        return contentIdentifier;
    }

    public void setContentIdentifier(String contentIdentifier) {
        this.contentIdentifier = contentIdentifier;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}