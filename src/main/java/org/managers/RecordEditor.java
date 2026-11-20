package org.managers;

import org.model.AuditLogEntry;
import org.model.RecordType;
import org.model.UserAccount;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.ToIntFunction;

/** Authorizes record changes requested by an authenticated controller. */
public final class RecordEditor<T> {

    /**
     * Transactional persistence boundary. Each operation must save the record and its
     * audit event in the same transaction. The store supplies the generated ID
     * for a create and reports a missing ID without writing an audit event.
     */
    public interface Store<T> {
        int create(T record, AuditEvent auditEvent);

        boolean update(int id, T record, AuditEvent auditEvent);
    }

    public record AuditEvent(String username, String action, RecordType type, LocalDateTime timestamp) {

        public AuditLogEntry forRecord(int recordId) {
            return new AuditLogEntry(0, username, action, type.name(), String.valueOf(recordId), timestamp);
        }
    }

    private final RecordType type;
    private final Store<T> store;
    private final ToIntFunction<T> recordId;
    private final Clock clock;

    public RecordEditor(RecordType type, Store<T> store, ToIntFunction<T> recordId, Clock clock) {
        this.type = Objects.requireNonNull(type);
        this.store = Objects.requireNonNull(store);
        this.recordId = Objects.requireNonNull(recordId);
        this.clock = Objects.requireNonNull(clock);
    }

    public int create(UserAccount actor, T record) {
        AuditEvent event = authorize(actor, "CREATE");
        return store.create(Objects.requireNonNull(record), event);
    }

    public boolean update(UserAccount actor, T record) {
        AuditEvent event = authorize(actor, "UPDATE");
        int id = recordId.applyAsInt(Objects.requireNonNull(record));
        if (id <= 0) {
            throw new IllegalArgumentException("Record ID must be positive.");
        }
        return store.update(id, record, event);
    }

    private AuditEvent authorize(UserAccount actor, String action) {
        if (actor == null || !actor.isAdmin()) {
            throw new SecurityException("Only administrators can change records.");
        }
        return new AuditEvent(actor.getUsername(), action, type, LocalDateTime.now(clock));
    }
}
