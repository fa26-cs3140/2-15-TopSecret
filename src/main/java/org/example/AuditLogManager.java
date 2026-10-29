package org.example;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records and retrieves audit events for content operations.
 */
public class AuditLogManager implements AuditLogService {

    private final AuditLogStore store;

    public AuditLogManager(AuditLogStore store) {
        if (store == null) {
            throw new IllegalArgumentException("AuditLogStore cannot be null.");
        }
        this.store = store;
    }

    public AuditLogManager(String url) {
        this(createStore(url));
    }

    @Override
    public void log(String username, String action, String contentType, String contentIdentifier) {
        if (isBlank(username) || isBlank(action) || isBlank(contentType) || isBlank(contentIdentifier)) {
            throw new IllegalArgumentException("Audit fields cannot be blank.");
        }

        store.add(new AuditLogEntry(
                0,
                username,
                action,
                contentType,
                contentIdentifier,
                LocalDateTime.now()
        ));
    }

    @Override
    public List<AuditLogEntry> getAuditLog() {
        return store.getAll();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static AuditLogStore createStore(String url) {
        return new SqliteAuditLogStore(url);
    }
}
