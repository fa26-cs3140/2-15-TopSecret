package org.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.model.Brief;
import org.model.Facility;
import org.model.Mission;
import org.model.RecordType;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SqliteRecordStoreIntegrationTest {

    @TempDir
    Path directory;

    private String url;
    private SqliteRecordStore store;
    private BriefManager briefs;
    private MissionManager missions;
    private SqliteAuditLogStore audit;
    private int facilityId;
    private int briefId;

    @BeforeEach
    void setUp() throws Exception {
        url = "jdbc:sqlite:" + directory.resolve("records.db");
        FacilityManager facilities = new FacilityManager(url);
        briefs = new BriefManager(url);
        missions = new MissionManager(url);
        facilities.createTable();
        briefs.createTable();
        new AgentManager(url).createTable();
        missions.createTable();
        audit = new SqliteAuditLogStore(url);
        new SqliteUserAccountStore(url).addUser(new org.model.UserAccount("admin", true));
        facilityId = facilities.addRecord("HQ", "HQ").getId();
        briefId = briefs.addRecord("Original", "2020-01-01", "Original text").getId();
        store = new SqliteRecordStore(url);
    }

    @Test
    void completeEditsAndAuditPersistAcrossConnections() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 11, 19, 12, 0);
        var create = new RecordEditor.AuditEvent("admin", "CREATE", RecordType.MISSION, timestamp);
        int missionId = store.create(new Mission(1, "O'Brien", new int[]{briefId}, new int[0], facilityId), create);

        assertArrayEquals(new int[]{briefId}, missions.getRecords("id", String.valueOf(missionId)).getFirst().getBriefIds());
        var update = new RecordEditor.AuditEvent("admin", "UPDATE", RecordType.MISSION, timestamp);
        assertTrue(store.update(missionId, new Mission(missionId, "Changed", new int[0], new int[0], facilityId), update));
        assertArrayEquals(new int[0], missions.getRecords("id", String.valueOf(missionId)).getFirst().getBriefIds());
        assertEquals(2, new SqliteAuditLogStore(url).getAll().size());
        var entry = audit.getAll().getLast();
        assertEquals("admin", entry.getUsername());
        assertEquals("UPDATE", entry.getAction());
        assertEquals("MISSION", entry.getContentType());
        assertEquals(String.valueOf(missionId), entry.getContentIdentifier());
        assertEquals(timestamp, entry.getTimestamp());
    }

    @Test
    void auditFailureRollsBackCreateUpdateAndAssignments() throws Exception {
        int missionId = store.create(new Mission(1, "Original", new int[]{briefId}, new int[0], facilityId),
                event("CREATE", RecordType.MISSION));
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_audit BEFORE INSERT ON audit_log "
                    + "BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }

        assertThrows(IllegalArgumentException.class, () -> store.update(missionId,
                new Mission(missionId, "Changed", new int[0], new int[0], facilityId), event("UPDATE", RecordType.MISSION)));
        assertThrows(IllegalArgumentException.class, () -> store.create(
                new Facility(1, "Never committed", "NO"), event("CREATE", RecordType.FACILITY)));

        Mission original = missions.getRecords("id", String.valueOf(missionId)).getFirst();
        assertEquals("Original", original.getTitle());
        assertArrayEquals(new int[]{briefId}, original.getBriefIds());
        assertEquals(1, new FacilityManager(url).getRecords("id", "%").size());
        assertEquals(1, audit.getAll().size());
    }

    @Test
    void missingTargetsAndInvalidRelationsWriteNoAudit() {
        assertThrows(IllegalArgumentException.class, () -> store.update(0,
                new Brief(1, "Invalid", "2020-01-01", "text", null), event("UPDATE", RecordType.BRIEF)));
        assertFalse(store.update(999, new Brief(999, "Missing", "2020-01-01", "text", null),
                event("UPDATE", RecordType.BRIEF)));
        assertThrows(IllegalArgumentException.class, () -> store.create(
                new Mission(1, "Invalid", new int[]{999}, new int[0], facilityId), event("CREATE", RecordType.MISSION)));
        assertTrue(audit.getAll().isEmpty());
        assertTrue(missions.getRecords("id", "%").isEmpty());
    }

    @Test
    void nullOptionalFieldsAndLiteralMultilineTextSurviveUpdate() {
        String text = "  O'Brien\n100% complete\n";
        assertTrue(store.update(briefId, new Brief(briefId, "Updated", "2020-01-01", text, null),
                event("UPDATE", RecordType.BRIEF)));
        assertEquals(text, briefs.getRecords("id", String.valueOf(briefId)).getFirst().getText());
    }

    private RecordEditor.AuditEvent event(String action, RecordType type) {
        return new RecordEditor.AuditEvent("admin", action, type, LocalDateTime.now());
    }
}
