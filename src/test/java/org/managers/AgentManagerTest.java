package org.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import org.model.Agent;

import java.nio.file.Path;
import java.sql.*;
import java.util.List;

public class AgentManagerTest {
    @TempDir
    Path tempDir;

    private String testUrl;

    @BeforeEach
    void setup() throws SQLException {
        testUrl = "jdbc:sqlite:" + tempDir.resolve("test.db");

        // Create the facilities table (manually) first
        try (Connection conn = DriverManager.getConnection(testUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE facilities (id INTEGER PRIMARY KEY)");
            stmt.execute("INSERT INTO facilities (id) VALUES (1), (2)");
        }
    }

    // Helper method: creates the schema so CRUD tests can be tested independently
    private AgentManager createManagerWithValidSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(testUrl); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE agents (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    dateOfBirth DATE NOT NULL,
                    dateOfDeath DATE,
                    notes TEXT,
                    facilityId INTEGER,
                    FOREIGN KEY (facilityId) REFERENCES facilities(id)
                )
            """);
        }

        return new AgentManager(testUrl);
    }

    // Helper method: inserts test data directly into SQLite
    private void insertTestAgent(int id, String name) throws SQLException {
        try (Connection conn = DriverManager.getConnection(testUrl);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO agents (id, name, dateOfBirth, facilityId) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, "1995-01-01");
            stmt.setInt(4, 1);
            stmt.executeUpdate();
        }
    }

    // Constructor should automatically create agents table
    @Test
    void testAgentTableCreated() throws SQLException {
        AgentManager manager = new AgentManager(testUrl);

        assertTrue(manager.tableExists("agents"));
    }

    // Constructor should reject invalid database URLs
    @Test
    void testInvalidURL() {
        assertThrows(IllegalArgumentException.class, () -> new AgentManager(null));
        assertThrows(IllegalArgumentException.class, () -> new AgentManager(""));
    }

    // Test the getters
    @Test
    void testGetters() {
        AgentManager manager = new AgentManager(testUrl);

        assertEquals(testUrl, manager.getURL());
        assertEquals("agents", manager.getTableName());

        assertArrayEquals(new String[]{"id", "name", "dateOfBirth", "dateOfDeath", "notes", "facilityId"}, manager.getColumnNames());
    }

    // Table should contain all six columns id, name, dateOfBirth, dateOfDeath, notes, facilityId
    @Test
    void testTableSchema() throws SQLException {
        new AgentManager(testUrl);

        try (Connection conn = DriverManager.getConnection(testUrl);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(agents)")) {

            java.util.List<String> columns = new java.util.ArrayList<>();

            while (rs.next()) {
                columns.add(rs.getString("name"));
            }

            assertEquals(List.of("id", "name", "dateOfBirth", "dateOfDeath", "notes", "facilityId"), columns);
        }
    }

    // Foreign key should reference facilities (id)
    @Test
    void testFacilityForeignKey() throws SQLException {
        new AgentManager(testUrl);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_list(agents)")) {

            assertTrue(rs.next(), "Expected a foreign key on agents");
            assertEquals("facilities", rs.getString("table"));
            assertEquals("facilityId", rs.getString("from"));
            assertEquals("id", rs.getString("to"));
        }
    }

    // Note: From this test on, we use the helper
    // manager creator to isolate from createTable()
    // Adding an agent using an Agent object
    @Test
    void testAddRecordWithAgent() throws SQLException {
        AgentManager manager = createManagerWithValidSchema();

        Agent original = new Agent(999, "John Doe", "1990-05-15", null, "Active agent", 1);
        Agent saved = manager.addRecord(original);

        assertNotSame(saved, original); // the returned agent should be a copy of the original agent
        assertTrue(saved.getId() > 0);
        assertEquals("John Doe", saved.getName());
        assertEquals("1990-05-15", saved.getDateOfBirth());
        assertEquals("Active agent", saved.getNotes());
        assertEquals(1, saved.getFacilityId());

        // Check if the record was actually added to the SQLite database
        try (Connection conn = DriverManager.getConnection(testUrl);
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM agents WHERE id=?")) {

            stmt.setInt(1, saved.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("John Doe", rs.getString("name"));
                assertEquals("1990-05-15", rs.getString("dateOfBirth"));
                assertEquals("Active agent", rs.getString("notes"));
                assertEquals(1, rs.getInt("facilityId"));
            }
        }
    }

    // Adding an agent using individual fields
    @Test
    void testAddRecordWithFields() throws SQLException {
        AgentManager manager = createManagerWithValidSchema();

        Agent saved = manager.addRecord("Jane Doe", "1998-03-20", null, "Undercover", 1);

        assertTrue(saved.getId() > 0);
        assertEquals("Jane Doe", saved.getName());
        assertEquals("1998-03-20", saved.getDateOfBirth());
        assertNull(saved.getDateOfDeath());
        assertEquals("Undercover", saved.getNotes());

        try (Connection conn = DriverManager.getConnection(testUrl);
            PreparedStatement stmt = conn.prepareStatement("SELECT name FROM agents WHERE id = ?")) {

            stmt.setInt(1, saved.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Jane Doe", rs.getString("name"));
            }
        }
    }

    @Test
    void testGetRecords() throws SQLException {
        AgentManager manager = createManagerWithValidSchema();

        insertTestAgent(1, "John Doe");
        insertTestAgent(2, "Jane Doe");
        insertTestAgent(3, "John Doe");

        List<Agent> results = manager.getRecords("name", "John Doe");

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getId());
        assertEquals(3, results.get(1).getId());
        assertEquals("John Doe", results.getFirst().getName());
    }

    // No matching records should return an empty list
    @Test
    void testGetRecordsNoMatch() throws SQLException {
        AgentManager manager = createManagerWithValidSchema();

        insertTestAgent(1, "John Doe");

        List<Agent> results = manager.getRecords("id", "999");

        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    void testUpdateRecord() throws SQLException {
        AgentManager manager = createManagerWithValidSchema();

        insertTestAgent(1, "John Doe");
        insertTestAgent(2, "Jane Doe");

        boolean updated = manager.updateRecord("id", "1", "name", "James Doe");

        assertTrue(updated);

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM agents ORDER BY id")) {

            assertTrue(rs.next());
            assertEquals("James Doe", rs.getString("name"));

            assertTrue(rs.next());
            assertEquals("Jane Doe", rs.getString("name"));
        }
    }

    // updateRecord should return false if no record matches
    @Test
    void testUpdateRecordNoMatch() throws SQLException {
        AgentManager manager = createManagerWithValidSchema();

        insertTestAgent(1, "John Doe");

        boolean updated = manager.updateRecord("id", "999", "name", "James Doe");

        assertFalse(updated);
    }

    // Add invalid agent
    @Test
    void testAddInvalidAgent() throws SQLException {
        AgentManager manager = createManagerWithValidSchema();

        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("", "1990-01-01", null, "Notes", 1));

        assertThrows(IllegalArgumentException.class, () -> manager.addRecord("John", "invalid-date", null, "Notes", 1));

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM agents")) {

            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}
