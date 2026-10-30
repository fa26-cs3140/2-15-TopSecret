package org.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import org.model.Facility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacilityManagerTest {
    @TempDir
    Path tempDir;

    private String testUrl;

    @BeforeEach
    void setup() {
        testUrl = "jdbc:sqlite:" + tempDir.resolve("test.db");
    }

    // Helper method: Creates the schema so CRUD tests can be tested independently
    private FacilityManager createManagerWithValidSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE facilities (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    abbreviation TEXT NOT NULL,
                    mission_id INTEGER UNIQUE
                )
            """);
        }

        return new FacilityManager(testUrl);
    }

    // Helper method: Inserts test data manually to not rely on addRecord()
    private void insertTestFacility(int id, String name, String abbreviation) throws SQLException {

        try (Connection conn = DriverManager.getConnection(testUrl);
             PreparedStatement stmt = conn.prepareStatement("""
                 INSERT INTO facilities (id, name, abbreviation)
                 VALUES (?, ?, ?)
             """)) {

            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, abbreviation);
            stmt.executeUpdate();
        }
    }

    // Helper method: Counts rows directly instead of relying on getRecords()
    private int countFacilities() throws SQLException {
        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM facilities")) {

            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    // ----- CONSTRUCTORS -----
    @Test
    void testInvalidURL() {
        assertThrows(IllegalArgumentException.class, () -> new FacilityManager(null));
        assertThrows(IllegalArgumentException.class, () -> new FacilityManager(""));
        assertThrows(IllegalArgumentException.class, () -> new FacilityManager("          "));
    }

    @Test
    void testGetters() {
        FacilityManager manager = new FacilityManager(testUrl);

        assertEquals(testUrl, manager.getURL());
        assertEquals("facilities", manager.getTableName());
        assertArrayEquals(new String[]{"id", "name", "abbreviation", "mission_id"}, manager.getColumnNames());
    }

    @Test
    void testFacilityTableCreated() throws SQLException {
        new FacilityManager(testUrl);

        try (Connection conn = DriverManager.getConnection(testUrl);
             ResultSet rs = conn.getMetaData().getTables(null, null, "facilities", new String[]{"TABLE"})) {

            assertTrue(rs.next(), "facilities table should exist");
        }
    }

    @Test
    void testTableSchema() throws SQLException {
        new FacilityManager(testUrl);

        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(facilities)")) {

            while (rs.next()) {
                String column = rs.getString("name");

                columns.add(column);
                types.add(rs.getString("type"));

                if (column.equals("id")) {
                    assertEquals(1, rs.getInt("pk"));
                }

                if (column.equals("name") || column.equals("abbreviation")) {
                    assertEquals(1, rs.getInt("notnull"));
                }
            }
        }

        assertEquals(List.of("id", "name", "abbreviation", "mission_id"), columns);
        assertEquals(List.of("INTEGER", "TEXT", "TEXT", "INTEGER"), types);
    }

    @Test
    void testUniqueMissionId() throws SQLException {
        new FacilityManager(testUrl);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                INSERT INTO facilities
                (name, abbreviation, mission_id)
                VALUES ('Facility A', 'FA', 10)
            """);

            // The schema declares mission_id as UNIQUE
            assertThrows(SQLException.class, () ->
                stmt.execute("""
                    INSERT INTO facilities
                    (name, abbreviation, mission_id)
                    VALUES ('Facility B', 'FB', 10)
                """)
            );
        }
    }

    @Test
    void testConstructorPreservesExistingRecords() throws SQLException {
        createManagerWithValidSchema();
        insertTestFacility(1, "Headquarters", "HQ");

        // Reinitializing the manager must not erase existing data
        new FacilityManager(testUrl);

        assertEquals(1, countFacilities());
    }

    // ----- ADD RECORDS -----
    @Test
    void testAddRecordWithFacility() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        Facility original = new Facility(999, "Central Intelligence Agency", "CIA");
        Facility saved = manager.addRecord(original);

        // Should return a new object with the generated ID
        assertNotSame(original, saved);
        assertTrue(saved.getId() > 0);
        assertNotEquals(original.getId(), saved.getId());
        assertEquals("Central Intelligence Agency", saved.getName());
        assertEquals("CIA", saved.getAbbreviation());

        // Verify that the returned ID matches the saved database record
        try (Connection conn = DriverManager.getConnection(testUrl);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM facilities WHERE id = ?")) {

            stmt.setInt(1, saved.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Central Intelligence Agency", rs.getString("name"));
                assertEquals("CIA", rs.getString("abbreviation"));
            }
        }
    }

    @Test
    void testAddRecordWithFields() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        Facility saved = manager.addRecord("Federal Bureau of Investigation", "FBI");

        assertTrue(saved.getId() > 0);
        assertEquals("Federal Bureau of Investigation", saved.getName());
        assertEquals("FBI", saved.getAbbreviation());
        assertEquals(1, countFacilities());
    }

    @Test
    void testAddMultipleRecords() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        Facility first = manager.addRecord("Facility Alpha", "FA");
        Facility second = manager.addRecord("Facility Bravo", "FB");

        assertTrue(first.getId() > 0);
        assertTrue(second.getId() > 0);
        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, countFacilities());
    }

    @Test
    void testAddInvalidRecord() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        // empty name
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("", "CIA"));
        // null name
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord(null, "CIA"));
        // empty abbr
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("Headquarters", ""));
        // null abbr
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("Headquarters", null));
        // blank name
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("   ", "HQ"));
        // blank abbr
        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("Headquarters", "   "));

        // None of the invalid records should be persisted
        assertEquals(0, countFacilities());
    }

    // ----- GET RECORDS -----
    @Test
    void testGetRecordsById() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Headquarters", "HQ");
        insertTestFacility(2, "Research Center", "RC");

        List<Facility> results = manager.getRecords("id", "1");

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId());
        assertEquals("Headquarters", results.get(0).getName());
        assertEquals("HQ", results.get(0).getAbbreviation());
    }

    @Test
    void testGetRecordsByName() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(3, "Headquarters", "HQ3");
        insertTestFacility(1, "Headquarters", "HQ1");
        insertTestFacility(2, "Research Center", "RC");

        List<Facility> results = manager.getRecords("name", "Headquarters");

        assertEquals(2, results.size());

        // Results should be sorted by ID
        assertEquals(1, results.get(0).getId());
        assertEquals(3, results.get(1).getId());
        assertEquals("HQ1", results.get(0).getAbbreviation());
        assertEquals("HQ3", results.get(1).getAbbreviation());
    }

    @Test
    void testGetRecordsByAbbreviation() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Headquarters", "HQ");
        insertTestFacility(2, "Research Center", "RC");

        List<Facility> results = manager.getRecords("abbreviation", "RC");

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId());
        assertEquals("Research Center", results.get(0).getName());
    }

    @Test
    void testGetRecordsNoMatch() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Headquarters", "HQ");

        List<Facility> results = manager.getRecords("id", "999");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetRecordsWithApostrophe() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Director's Office", "DO");

        List<Facility> results = manager.getRecords("name", "Director's Office");

        assertEquals(1, results.size());
        assertEquals("Director's Office", results.get(0).getName());
    }

    // ----- UPDATE RECORDS -----
    @Test
    void testUpdateRecordName() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Old Headquarters", "HQ");
        insertTestFacility(2, "Research Center", "RC");

        Boolean updated = manager.updateRecord("id", "1", "name", "New Headquarters");

        assertTrue(updated);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM facilities ORDER BY id")) {

            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("New Headquarters", rs.getString("name"));

            assertTrue(rs.next());
            assertEquals(2, rs.getInt("id"));
            assertEquals("Research Center", rs.getString("name"));

            assertFalse(rs.next());
        }
    }

    @Test
    void testUpdateRecordAbbreviation() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Headquarters", "HQ");

        Boolean updated = manager.updateRecord("id", "1", "abbreviation", "MAIN");

        assertTrue(updated);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT abbreviation FROM facilities WHERE id = 1")) {

            assertTrue(rs.next());
            assertEquals("MAIN", rs.getString("abbreviation"));
        }
    }

    @Test
    void testUpdateRecordNoMatch() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Headquarters", "HQ");

        Boolean updated = manager.updateRecord("id", "999", "name", "New Headquarters");

        assertFalse(updated);

        // Existing data should be unchanged
        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM facilities WHERE id = 1")) {

            assertTrue(rs.next());
            assertEquals("Headquarters", rs.getString("name"));
        }
    }

    @Test
    void testUpdateRecordByName() throws SQLException {
        FacilityManager manager = createManagerWithValidSchema();

        insertTestFacility(1, "Headquarters", "HQ");

        Boolean updated = manager.updateRecord("name", "Headquarters", "abbreviation", "MAIN");

        assertTrue(updated);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT abbreviation FROM facilities WHERE id = 1")) {

            assertTrue(rs.next());
            assertEquals("MAIN", rs.getString("abbreviation"));
        }
    }

    // ----- IMPORT TSV -----
    @Test
    void testImportTSV() throws SQLException, IOException {
        FacilityManager manager = createManagerWithValidSchema();

        Path tsv = tempDir.resolve("facilities.tsv");

        Files.writeString(tsv,
                "name\tabbreviation\n"
                        + "Headquarters\tHQ\n"
                        + "Research Center\tRC\n");

        Boolean imported = manager.importTSV(tsv.toString(), "facilities", tempDir.toString(), "test.db");

        assertTrue(imported);
        assertEquals(2, countFacilities());

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, abbreviation FROM facilities ORDER BY id")) {

            assertTrue(rs.next());
            assertEquals("Headquarters", rs.getString("name"));
            assertEquals("HQ", rs.getString("abbreviation"));

            assertTrue(rs.next());
            assertEquals("Research Center", rs.getString("name"));
            assertEquals("RC", rs.getString("abbreviation"));

            assertFalse(rs.next());
        }
    }
}
