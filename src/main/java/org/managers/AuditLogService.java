package org.managers;

import org.model.AuditLogEntry;
import java.util.List;

public interface AuditLogService {

    void log(String username, String action, String contentType, String contentIdentifier);

    List<AuditLogEntry> getAuditLog();
}
