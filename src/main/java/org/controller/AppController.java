package org.controller;

import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.model.RecordType;
import org.model.UserAccount;

// Navigation logic shared by the terminal and web interfaces.
// Methods return plain text; the terminal prints it and the web view wraps it in HTML.
public class AppController {

    private final BriefManager briefManager;
    private final AgentManager agentManager;
    private final FacilityManager facilityManager;
    private final AuditLogService auditLog;

    public AppController(BriefManager briefManager, AgentManager agentManager,
                         FacilityManager facilityManager, AuditLogService auditLog) {
        if (briefManager == null || agentManager == null || facilityManager == null || auditLog == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.briefManager = briefManager;
        this.agentManager = agentManager;
        this.facilityManager = facilityManager;
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
