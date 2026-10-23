package org.example;

import java.util.List;

/** Persistence boundary for audit-log records. */
public interface AuditLogStore {

    void add(AuditLogEntry entry);

    List<AuditLogEntry> getAll();
}
