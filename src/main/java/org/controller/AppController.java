package org.controller;

import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.RecordRelationships;
import org.model.RelatedRecord;
import org.managers.RecordEditor;
import org.managers.SqliteRecordStore;
import org.managers.MissionManager;
import org.model.Agent;
import org.model.AuditLogEntry;
import org.model.Brief;
import org.model.Country;
import org.model.Facility;
import org.model.Mission;
import org.model.RecordType;
import org.model.UserAccount;
import org.view.FormField;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Control logic shared by the terminal and web interfaces.
// Read methods return plain text; the terminal prints it and the web view wraps it in HTML.
// Create/update methods take form values keyed by RecordForms field names.
public class AppController {

    private final BriefManager briefManager;
    private final AgentManager agentManager;
    private final MissionManager missionManager;
    private final FacilityManager facilityManager;
    private final AuditLogService auditLog;
    private final RecordEditor.Store<Object> changes;

    public AppController(BriefManager briefManager, AgentManager agentManager, MissionManager missionManager,
                         FacilityManager facilityManager, AuditLogService auditLog) {
        this(briefManager, agentManager, missionManager, facilityManager, auditLog,
                new SqliteRecordStore(briefManager == null ? null : briefManager.getURL()));
    }

    public AppController(BriefManager briefManager, AgentManager agentManager, MissionManager missionManager,
                         FacilityManager facilityManager, AuditLogService auditLog,
                         RecordEditor.Store<Object> changes) {
        if (briefManager == null || agentManager == null || missionManager == null || facilityManager == null
                || auditLog == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.briefManager = briefManager;
        this.agentManager = agentManager;
        this.missionManager = missionManager;
        this.facilityManager = facilityManager;
        this.auditLog = auditLog;
        this.changes = java.util.Objects.requireNonNull(changes);
    }

    // One line per record ("id. name"), or a "No ... are available." message.
    public String listRecords(RecordType type) {
        List<String> lines = switch (type) {
            case BRIEF -> briefManager.getRecords("id", "%").stream()
                    .map(b -> b.getId() + ". " + b.getTitle()).toList();
            case AGENT -> agentManager.getRecords("id", "%").stream()
                    .map(a -> a.getId() + ". " + a.getName()).toList();
            case COUNTRY -> facilityManager.getCountries().stream()
                    .map(country -> country.getID() + ". " + country.getName()).toList();
            case FACILITY -> facilityManager.getRecords("id", "%").stream()
                    .map(f -> f.getId() + ". " + f.getName() + " (" + f.getAbbreviation() + ")"
                            + countryName(f.getCountryID()).map(name -> " [" + name + "]").orElse("")).toList();
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
        Optional<List<String>> details = switch (type) {
            case BRIEF -> brief(id).map(b -> List.of("Title: " + b.getTitle(), "Date: " + b.getDate(),
                    "Text: " + b.getText()));
            case AGENT -> agent(id).map(a -> List.of("Name: " + a.getName(), "Date of birth: " + a.getDateOfBirth(),
                    "Date of death: " + orNA(a.getDateOfDeath()), "Notes: " + orNA(a.getNotes()),
                    "Facility ID: " + a.getFacilityId()));
            case FACILITY -> facility(id).map(f -> List.of("Name: " + f.getName(),
                    "Abbreviation: " + f.getAbbreviation()));
            case COUNTRY -> country(id).map(c -> List.of("Name: " + c.getName()));
            case MISSION -> mission(id).map(m -> List.of("Title: " + m.getTitle(),
                    "Brief Ids: " + m.getBriefIdsAsString(), "Agent Ids: " + m.getAgentIdsAsString(),
                    "Facility Id: " + m.getFacilityId()));
        };
        if (details.isEmpty()) {
            return "No " + type.getSingular() + " found with ID " + id + ".";
        }
        auditLog.log(user.getUsername(), "READ", type.name(), String.valueOf(id));
        return String.join("\n", details.get());
    }

    public List<RelatedRecord> relatedRecords(RecordType type, int id) {
        return new RecordRelationships(briefManager, agentManager, facilityManager, missionManager).find(type, id);
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

    // Only administrators may create or edit records.
    public boolean canEdit(UserAccount user) {
        return user != null && user.isAdmin();
    }

    // Current values of a record keyed by RecordForms field name, used to fill the edit form.
    // Returns null when no record has that ID. Missing optional values are empty strings.
    public Map<String, String> formValues(RecordType type, int id) {
        Optional<Map<String, String>> values = switch (type) {
            case BRIEF -> brief(id).map(b -> values("title", b.getTitle(), "date", b.getDate(), "text", b.getText()));
            case AGENT -> agent(id).map(a -> values("name", a.getName(), "dateOfBirth", a.getDateOfBirth(),
                    "dateOfDeath", a.getDateOfDeath(), "notes", a.getNotes(),
                    "facilityId", String.valueOf(a.getFacilityId())));
            case FACILITY -> facility(id).map(f -> values("name", f.getName(), "abbreviation", f.getAbbreviation(),
                    "countryId", String.valueOf(f.getCountryID())));
            case MISSION -> mission(id).map(m -> values("title", m.getTitle(),
                    "facilityId", String.valueOf(m.getFacilityId()),
                    "briefIds", joinIds(m.getBriefIds()), "agentIds", joinIds(m.getAgentIds())));
        };
        return values.orElse(null);
    }

    // Creates a record from submitted form values, logs CREATE, and returns the new record's ID.
    // Throws SecurityException for non-admins and IllegalArgumentException for invalid values.
    public int createRecord(RecordType type, Map<String, String> values, UserAccount user) {
        requireEditor(user);
        Map<String, String> v = cleaned(type, values);
        // The model constructors validate (dates, blanks); ID 1 is a placeholder until the database assigns one.
        int id = switch (type) {
            case BRIEF -> briefManager.addRecord(new Brief(1, v.get("title"), v.get("date"), v.get("text"), null))
                    .getId();
            case AGENT -> agentManager.addRecord(new Agent(1, v.get("name"), v.get("dateOfBirth"),
                    v.get("dateOfDeath"), v.get("notes"), facilityId(v))).getId();
            case FACILITY -> facilityManager.addRecord(new Facility(1, v.get("name"), v.get("abbreviation"),
                    countryId(v))).getId();
            case MISSION -> missionManager.addRecord(new Mission(1, v.get("title"), briefIds(v), agentIds(v),
                    facilityId(v))).getId();
        };
        auditLog.log(user.getUsername(), "CREATE", type.name(), String.valueOf(id));
        return id;
    }

    public void updateRecord(RecordType type, int id, Map<String, String> values, UserAccount user) {
        requireEditor(user);
        if (formValues(type, id) == null) {
            throw new IllegalArgumentException("No " + type.getSingular() + " found with ID " + id + ".");
        }
        Map<String, String> v = cleaned(type, values);
        // Build the model object first so invalid values are rejected before anything is saved.
        switch (type) {
            case BRIEF -> {
                new Brief(id, v.get("title"), v.get("date"), v.get("text"), null);
                save(briefManager, id, v, "title", "date", "text");
            }
            case AGENT -> {
                new Agent(id, v.get("name"), v.get("dateOfBirth"), v.get("dateOfDeath"), v.get("notes"), facilityId(v));
                save(agentManager, id, v, "name", "dateOfBirth", "dateOfDeath", "notes", "facilityId");
            }
            case FACILITY -> {
                int countryId = countryId(v);
                new Facility(id, v.get("name"), v.get("abbreviation"), countryId);
                save(facilityManager, id, v, "name", "abbreviation");
                // the form field is countryId; the facilities column is country_id
                facilityManager.updateRecord("id", String.valueOf(id), "country_id", String.valueOf(countryId));
            }
            case MISSION -> {
                Mission mission = new Mission(id, v.get("title"), briefIds(v), agentIds(v), facilityId(v));
                save(missionManager, id, v, "title", "facilityId");
                missionManager.replaceAssignments(id, mission.getBriefIds(), mission.getAgentIds());
            }
        }
    }

    private RecordEditor<Object> editor(RecordType type) {
        return new RecordEditor<>(type, changes, record -> switch (record) {
            case Brief brief -> brief.getId();
            case Agent agent -> agent.getId();
            case Facility facility -> facility.getId();
            case Country country -> country.getID();
            case Mission mission -> mission.getId();
            default -> throw new IllegalArgumentException("Unsupported content record.");
        }, Clock.systemUTC());
    }

    private Object model(RecordType type, int id, Map<String, String> values) {
        return switch (type) {
            case BRIEF -> new Brief(id, values.get("title"), values.get("date"), values.get("text"), null);
            case AGENT -> new Agent(id, values.get("name"), values.get("dateOfBirth"),
                    values.get("dateOfDeath"), values.get("notes"), facilityId(values));
            case FACILITY -> new Facility(id, values.get("name"), values.get("abbreviation"), countryId(values));
            case COUNTRY -> new Country(values.get("name"), id, new Facility[0]);
            case MISSION -> new Mission(id, values.get("title"), briefIds(values), agentIds(values), facilityId(values));
        };
    }

    private void requireEditor(UserAccount user) {
        if (!canEdit(user)) {
            throw new SecurityException("Access denied. Only administrators can create or edit records.");
        }
    }

    // ---- lookups ----

    private Optional<Brief> brief(int id) {
        return briefManager.getRecords("id", "%").stream().filter(b -> b.getId() == id).findFirst();
    }

    private Optional<Agent> agent(int id) {
        return agentManager.getRecords("id", "%").stream().filter(a -> a.getId() == id).findFirst();
    }

    private Optional<Facility> facility(int id) {
        return facilityManager.getRecords("id", "%").stream().filter(f -> f.getId() == id).findFirst();
    }

    private Optional<Mission> mission(int id) {
        return missionManager.getRecords("id", "%").stream().filter(m -> m.getId() == id).findFirst();
    }

    // ---- form value helpers ----

    // Trimmed values for the type's fields, blanks as null. Throws when a required field is blank.
    private static Map<String, String> cleaned(RecordType type, Map<String, String> values) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        for (FormField field : RecordForms.fieldsFor(type)) {
            String raw = values == null ? null : values.get(field.name());
            String value = raw == null || raw.isBlank() ? null : raw;
            if (value == null && field.required()) {
                throw new IllegalArgumentException(field.label() + " is required.");
            }
            if (value != null && field.kind() != FormField.Kind.TEXTAREA) {
                value = value.trim();
            }
            cleaned.put(field.name(), value);
        }
        return cleaned;
    }

    // Facility ID from the form; must name an existing facility.
    private int facilityId(Map<String, String> v) {
        int id = parseId(v.get("facilityId"), "Facility ID");
        if (facility(id).isEmpty()) {
            throw new IllegalArgumentException("No facility found with ID " + id + ".");
        }
        return id;
    }

    // Country ID from the form; must name an existing country.
    private int countryId(Map<String, String> v) {
        int id = parseId(v.get("countryId"), "Country ID");
        if (countryName(id).isEmpty()) {
            throw new IllegalArgumentException("No country found with ID " + id + ".");
        }
        return id;
    }

    // Country name for an ID, or empty when no such country exists.
    private Optional<String> countryName(int countryId) {
        return Optional.ofNullable(facilityManager.getCountry(countryId)).map(Country::getName);
    }

    private int[] briefIds(Map<String, String> v) {
        int[] ids = parseIds(v.get("briefIds"), "Brief IDs");
        for (int id : ids) {
            if (brief(id).isEmpty()) {
                throw new IllegalArgumentException("No brief found with ID " + id + ".");
            }
        }
        return ids;
    }

    private int[] agentIds(Map<String, String> v) {
        int[] ids = parseIds(v.get("agentIds"), "Agent IDs");
        for (int id : ids) {
            if (agent(id).isEmpty()) {
                throw new IllegalArgumentException("No agent found with ID " + id + ".");
            }
        }
        return ids;
    }

    private static int parseId(String text, String label) {
        try {
            int id = Integer.parseInt(text.trim());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw new IllegalArgumentException(label + " must be a positive whole number.");
    }

    // "1, 2,3" -> {1, 2, 3}; blank -> {}. Duplicates are kept once.
    private static int[] parseIds(String text, String label) {
        if (text == null) {
            return new int[0];
        }
        List<Integer> ids = new ArrayList<>();
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                int id = parseId(part, label);
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    private static String joinIds(int[] ids) {
        return String.join(", ", Arrays.stream(ids).mapToObj(String::valueOf).toList());
    }

    // Ordered map from alternating name/value pairs; null values become "".
    private static Map<String, String> values(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return map;
    }

    private static String orNA(String value) {
        return value == null ? "N/A" : value;
    }
}
