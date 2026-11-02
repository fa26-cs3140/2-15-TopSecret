package org.example.controller;

import org.example.AgencyDataService;
import org.example.AuditLogService;
import org.example.RecordType;
import org.example.UserAccount;

// Navigation logic shared by the terminal and web interfaces.
// Methods return plain text; the terminal prints it and the web view wraps it in HTML.
public class AppController {

    private final AgencyDataService data;
    private final AuditLogService auditLog;

    public AppController(AgencyDataService data, AuditLogService auditLog) {
        if (data == null || auditLog == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.data = data;
        this.auditLog = auditLog;
    }

    // One line per record ("id. name"), or a "No ... are available." message.
    public String listRecords(RecordType type) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    // Record details, one property per line. Records a READ audit entry when found.
    public String readRecord(RecordType type, int id, UserAccount user) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }

    // One line per audit entry for admins; an access-denied message for everyone else.
    public String auditLog(UserAccount user) {
        throw new UnsupportedOperationException("Not implemented yet (HW6 week 2).");
    }
}
