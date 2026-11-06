package org.controller;

import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.model.AuditLogEntry;
import org.model.RecordType;
import org.model.UserAccount;

import java.util.List;

// Navigation logic shared by the terminal and web interfaces.
// Methods return plain text; the terminal prints it and the web view wraps it in HTML.
public class AppController {

    private final BriefManager briefManager;
    private final AgentManager agentManager;
    private final MissionManager missionManager;
    private final FacilityManager facilityManager;
    private final AuditLogService auditLog;

    public AppController(BriefManager briefManager, AgentManager agentManager, MissionManager missionManager,
                         FacilityManager facilityManager, AuditLogService auditLog) {
        if (briefManager == null || agentManager == null || facilityManager == null || auditLog == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.briefManager = briefManager;
        this.agentManager = agentManager;
        this.missionManager = missionManager;
        this.facilityManager = facilityManager;
        this.auditLog = auditLog;
    }

    // One line per record ("id. name"), or a "No ... are available." message.
    public String listRecords(RecordType type) {
        List<String> lines = switch (type) {
            case BRIEF -> briefManager.getRecords("id", "%").stream()
                    .map(b -> b.getId() + ". " + b.getTitle()).toList();
            case AGENT -> agentManager.getRecords("id", "%").stream()
                    .map(a -> a.getId() + ". " + a.getName()).toList();
            case FACILITY -> facilityManager.getRecords("id", "%").stream()
                    .map(f -> f.getId() + ". " + f.getName() + " (" + f.getAbbreviation() + ")").toList();
            case MISSION -> missionManager.getRecords("id", "%").stream()
                    .map(m -> m.getId() + ". " + m.getTitle()).toList();
        };
        if (lines.isEmpty()) {
            return "No " + type.getTitle().toLowerCase() + " are available.";
        }
        return String.join("\n", lines);
    }

    // Record details, one property per line. Records a READ audit entry when found.
    public String readRecord(RecordType type, int id, UserAccount user) {
        List<String> details = switch (type) {
            case BRIEF -> briefManager.getRecords("id", "%").stream().filter(b -> b.getId() == id).findFirst()
                    .map(b -> List.of("Title: " + b.getTitle(), "Date: " + b.getDate(), "Text: " + b.getText()))
                    .orElse(null);
            case AGENT -> agentManager.getRecords("id", "%").stream().filter(a -> a.getId() == id).findFirst()
                    .map(a -> List.of("Name: " + a.getName(), "Date of birth: " + a.getDateOfBirth(),
                            "Date of death: " + orNA(a.getDateOfDeath()), "Notes: " + orNA(a.getNotes()),
                            "Facility ID: " + a.getFacilityId()))
                    .orElse(null);
            case FACILITY -> facilityManager.getRecords("id", "%").stream().filter(f -> f.getId() == id).findFirst()
                    .map(f -> List.of("Name: " + f.getName(), "Abbreviation: " + f.getAbbreviation()))
                    .orElse(null);
            case MISSION -> missionManager.getRecords("id", "%").stream().filter(m -> m.getId() == id).findFirst()
                    .map(m -> List.of("Title: " + m.getTitle(), "Brief Ids: " + m.getBriefIdsAsString(), 
                                      "Agent Ids: " + m.getAgentIdsAsString(), "Facility Id: " + m.getFacilityId()))
                    .orElse(null);
        };
        if (details == null) {
            return "No " + type.getSingular() + " found with ID " + id + ".";
        }
        auditLog.log(user.getUsername(), "READ", type.name(), String.valueOf(id));
        return String.join("\n", details);
    }

    // One line per audit entry for admins; an access-denied message for everyone else.
    public String auditLog(UserAccount user) {
        if (!user.isAdmin()) {
            return "Access denied. Only administrators can review the audit log.";
        }
        List<AuditLogEntry> entries = auditLog.getAuditLog();
        if (entries == null || entries.isEmpty()) {
            return "No audit entries have been recorded.";
        }
        return String.join("\n", entries.stream()
                .map(e -> e.getTimestamp() + " | " + e.getUsername() + " | " + e.getAction()
                        + " | " + e.getContentType() + " " + e.getContentIdentifier())
                .toList());
    }

    private static String orNA(String value) {
        return value == null ? "N/A" : value;
    }
}
