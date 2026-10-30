package org.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import org.model.Brief;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BriefManagerTest {
    @TempDir
    Path tempDir;

    private String testUrl;

    @BeforeEach
    void setUp() {
        testUrl = "jdbc:sqlite:" + tempDir.resolve("test.db");
    }

    // Helper method: Creates the schema so CRUD tests can be tested independently
    private BriefManager createManagerWithValidSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(testUrl);
            Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE briefs (
                    id INTEGER PRIMARY KEY,
                    title TEXT NOT NULL,
                    date DATE NOT NULL,
                    text TEXT NOT NULL,
                    mission_id INTEGER UNIQUE
                )
            """);
        }

        return new BriefManager(testUrl);
    }

    // Helper method: Inserts test data manually to not rely on addRecord()
    private void insertTestBrief(int id, String title, Integer missionId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(testUrl);
             PreparedStatement stmt = conn.prepareStatement("""
                 INSERT INTO briefs
                 (id, title, date, text, mission_id)
                 VALUES (?, ?, ?, ?, ?)
             """)) {

            stmt.setInt(1, id);
            stmt.setString(2, title);
            stmt.setString(3, "2026-10-01");
            stmt.setString(4, "Classified mission details");
            stmt.setObject(5, missionId);
            stmt.executeUpdate();
        }
    }

    // Helper method: Counts rows directly instead of relying on getRecords()
    private int countBriefs() throws SQLException {
        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM briefs")) {

            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    // ----- CONSTRUCTORS ------
    @Test
    void testInvalidURL() {
        assertThrows(IllegalArgumentException.class, () -> new BriefManager(null));
        assertThrows(IllegalArgumentException.class, () -> new BriefManager(""));
        assertThrows(IllegalArgumentException.class, () -> new BriefManager("          "));
    }

    @Test
    void testBriefTableCreated() throws SQLException {
        new BriefManager(testUrl);

        try (Connection conn = DriverManager.getConnection(testUrl);
             ResultSet rs = conn.getMetaData().getTables(null, null, "briefs", new String[]{"TABLE"})) {

            assertTrue(rs.next(), "briefs table should exist");
        }
    }

    @Test
    void testGetters() {
        BriefManager manager = new BriefManager(testUrl);

        assertEquals(testUrl, manager.getURL());
        assertEquals("briefs", manager.getTableName());
        assertArrayEquals(new String[]{"id", "title", "date", "text", "mission_id"}, manager.getColumnNames());
    }

    @Test
    void testTableSchema() throws SQLException {
        new BriefManager(testUrl);

        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(briefs)")) {

            while (rs.next()) {
                String name = rs.getString("name");
                columns.add(name);
                types.add(rs.getString("type"));

                if (name.equals("id")) {
                    assertEquals(1, rs.getInt("pk"));
                }

                if (name.equals("title") || name.equals("date") || name.equals("text")) {
                    assertEquals(1, rs.getInt("notnull"));
                }
            }
        }

        assertEquals(List.of("id", "title", "date", "text", "mission_id"), columns);
        assertEquals(List.of("INTEGER", "TEXT", "DATE", "TEXT", "INTEGER"), types);
    }

    @Test
    void testConstructorPreservesExistingRecords() throws SQLException {
        createManagerWithValidSchema();
        insertTestBrief(1, "Operation Alpha", null);

        // Creating another manager should not delete existing data.
        new BriefManager(testUrl);

        assertEquals(1, countBriefs());
    }

    // ----- ADD RECORD ------
    @Test
    void testAddRecordWithBrief() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        Brief original = new Brief(999, "Operation Alpha", "2026-10-15", "Classified information", 1);

        Brief saved = manager.addRecord(original);

        assertTrue(saved.getId() > 0);
        assertNotEquals(original.getId(), saved.getId());

        // Should preserve all other fields.
        assertEquals("Operation Alpha", saved.getTitle());
        assertEquals("2026-10-15", saved.getDate());
        assertEquals("Classified information", saved.getText());
        assertEquals(1, saved.getMissionId());

        // Check actual database persistence.
        try (Connection conn = DriverManager.getConnection(testUrl);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM briefs WHERE id = ?")) {

            stmt.setInt(1, saved.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Operation Alpha", rs.getString("title"));
                assertEquals("2026-10-15", rs.getString("date"));
                assertEquals("Classified information", rs.getString("text"));
                assertEquals(1, rs.getInt("mission_id"));
                assertFalse(rs.wasNull());
            }
        }
    }

    @Test
    void testAddRecordWithFields() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        Brief saved = manager.addRecord("Operation Bravo", "2026-10-20", "Mission briefing");

        assertTrue(saved.getId() > 0);
        assertEquals("Operation Bravo", saved.getTitle());
        assertEquals("2026-10-20", saved.getDate());
        assertEquals("Mission briefing", saved.getText());
        assertNull(saved.getMissionId());
        assertEquals(1, countBriefs());
    }

    @Test
    void testAddMultipleRecords() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        Brief first = manager.addRecord("Operation Alpha", "2026-10-01", "First brief");

        Brief second = manager.addRecord("Operation Bravo", "2026-10-02", "Second brief");

        assertTrue(first.getId() > 0);
        assertTrue(second.getId() > 0);
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, countBriefs());
    }

    @Test
    void testAddInvalidRecord() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        // empty title
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("", "2026-10-01", "Text"));
        // null title
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord(null, "2026-10-01", "Text"));
        // malformatted date
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("Alpha", "not-a-date", "Text"));
        // not a real date
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("Alpha", "2026-02-30", "Text"));
        // null text
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("Alpha", "2026-10-01", null));

        // Invalid records should never be saved
        assertEquals(0, countBriefs());
    }

    // ----- GETTERS -----
    @Test
    void testGetRecordsById() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        insertTestBrief(1, "Operation Alpha", null);
        insertTestBrief(2, "Operation Bravo", null);

        List<Brief> results = manager.getRecords("id", "1");

        assertEquals(1, results.size());

        Brief retrieved = results.get(0);

        assertEquals(1, retrieved.getId());
        assertEquals("Operation Alpha", retrieved.getTitle());
        assertEquals("2026-10-01", retrieved.getDate());
        assertEquals("Classified mission details", retrieved.getText());
        assertNull(retrieved.getMissionId());
    }

    @Test
    void testGetRecordsByTitle() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        insertTestBrief(3, "Operation Alpha", null);
        insertTestBrief(1, "Operation Alpha", null);
        insertTestBrief(2, "Operation Bravo", null);

        List<Brief> results = manager.getRecords("title", "Operation Alpha");

        assertEquals(2, results.size());

        assertEquals(1, results.get(0).getId());
        assertEquals(3, results.get(1).getId());
    }

    @Test
    void testGetRecordsByMissionId() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        insertTestBrief(1, "Operation Alpha", 10);
        insertTestBrief(2, "Operation Bravo", 20);

        List<Brief> results = manager.getRecords("mission_id", "20");

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId());
        assertEquals(20, results.get(0).getMissionId());
    }

    @Test
    void testGetRecordsNoMatch() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        insertTestBrief(1, "Operation Alpha", null);

        List<Brief> results = manager.getRecords("id", "999");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ----- UPDATE RECORDS -----
    @Test
    void testUpdateRecord() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        insertTestBrief(1, "Old Title", null);
        insertTestBrief(2, "Unchanged Title", null);

        Boolean updated = manager.updateRecord("id", "1", "title", "New Title");

        assertTrue(updated);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, title FROM briefs ORDER BY id")) {

            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("New Title", rs.getString("title"));

            assertTrue(rs.next());
            assertEquals(2, rs.getInt("id"));
            assertEquals("Unchanged Title", rs.getString("title"));

            assertFalse(rs.next());
        }
    }

    @Test
    void testUpdateRecordText() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        insertTestBrief(1, "Operation Alpha", null);

        Boolean updated = manager.updateRecord("id", "1", "text", "Updated classified information");

        assertTrue(updated);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT text FROM briefs WHERE id = 1")) {

            assertTrue(rs.next());
            assertEquals("Updated classified information", rs.getString("text"));
        }
    }

    // Unmatched record should result in no update
    @Test
    void testUpdateRecordNoMatch() throws SQLException {
        BriefManager manager = createManagerWithValidSchema();

        insertTestBrief(1, "Operation Alpha", null);

        Boolean updated = manager.updateRecord("id", "999", "title", "New Title");

        assertFalse(updated);

        // Existing data should be unchanged.
        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT title FROM briefs WHERE id = 1")) {

            assertTrue(rs.next());
            assertEquals("Operation Alpha", rs.getString("title"));
        }
    }

    // ----- IMPORT TSV -----
    @Test
    void testImportTSV() throws SQLException, IOException {
        BriefManager manager = createManagerWithValidSchema();

        Path tsv = tempDir.resolve("briefs.tsv");

        // TSV format documented by BriefManager: title, date, text (with a header to skip)
        Files.writeString(tsv, "title\tdate\ttext\n"
                        + "Operation Alpha\t2026-10-01\tFirst brief\n"
                        + "Operation Bravo\t2026-10-02\tSecond brief\n");

        Boolean imported = manager.importTSV(tsv.toString(), "briefs", tempDir.toString(), "test.db");

        assertTrue(imported);
        assertEquals(2, countBriefs());

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT title, date, text FROM briefs ORDER BY id")) {

            assertTrue(rs.next());
            assertEquals("Operation Alpha", rs.getString("title"));
            assertEquals("2026-10-01", rs.getString("date"));
            assertEquals("First brief", rs.getString("text"));

            assertTrue(rs.next());
            assertEquals("Operation Bravo", rs.getString("title"));
            assertEquals("2026-10-02", rs.getString("date"));
            assertEquals("Second brief", rs.getString("text"));

            assertFalse(rs.next());
        }
    }
}
