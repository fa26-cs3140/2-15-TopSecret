package org.example.model.messaging;

import org.example.SqliteAgencyDataService;
import org.example.SqliteUserAccountStore;
import org.example.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageSchemaIntegrationTest {
    @TempDir Path directory;
    private String url;
    private String schema;

    @BeforeEach
    void setUp() throws Exception {
        url = "jdbc:sqlite:" + directory.resolve("messages.db");
        try (var resource = getClass().getResourceAsStream("/org/example/model/messaging/schema.sql")) {
            assertNotNull(resource);
            schema = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
        var users = new SqliteUserAccountStore(url);
        assertTrue(users.addUser(new UserAccount("sender", false)));
        var data = new SqliteAgencyDataService(url);
        int facility = data.addFacility("Test Agency", "TA").getId();
        assertEquals(1, data.addAgent("Ghost", "1990-01-01", null, null, facility).getId());
        try (var c = connect(); var statement = c.createStatement()) {
            applySchema(statement);
        }
    }

    private Connection connect() throws SQLException {
        var c = DriverManager.getConnection(url);
        try (var statement = c.createStatement()) { statement.execute("PRAGMA foreign_keys=ON"); }
        return c;
    }

    private void applySchema(Statement statement) throws SQLException {
        for (String sql : schema.split(";")) if (!sql.isBlank()) statement.execute(sql);
    }

    private void insert(String sender, int agent, String recipient, String body) throws SQLException {
        try (var c = connect(); var p = c.prepareStatement("INSERT INTO agent_messages "
                + "(sender_username, recipient_agent_id, recipient_name, body, sent_at) VALUES(?, ?, ?, ?, ?)")) {
            p.setString(1, sender); p.setInt(2, agent); p.setString(3, recipient);
            p.setString(4, body); p.setString(5, "2026-10-29T12:30:00"); p.executeUpdate();
        }
    }

    @Test
    void noRecipientAccountIsRequiredAndEveryFieldSurvivesReopen() throws Exception {
        insert("sender", 1, "Ghost", "Meet at dawn.\nBring O'Brien's file.");
        try (var c = connect(); var s = c.createStatement();
             var rows = s.executeQuery("SELECT * FROM agent_messages")) {
            assertTrue(rows.next());
            assertTrue(rows.getLong("id") > 0);
            assertEquals("sender", rows.getString("sender_username"));
            assertEquals(1, rows.getInt("recipient_agent_id"));
            assertEquals("Ghost", rows.getString("recipient_name"));
            assertEquals("Meet at dawn.\nBring O'Brien's file.", rows.getString("body"));
            assertEquals("2026-10-29T12:30:00", rows.getString("sent_at"));
            assertFalse(rows.next());
        }
    }

    @Test
    void proposedRecipientQueryIsCaseInsensitiveAndDoesNotReturnOtherInboxes() throws Exception {
        insert("sender", 1, "Ghost", "first");
        insert("sender", 1, "Ghost", "second");
        try (var c = connect(); var p = c.prepareStatement(
                "SELECT body FROM agent_messages WHERE recipient_name = ? COLLATE NOCASE ORDER BY id")) {
            p.setString(1, "gHoSt");
            try (var rows = p.executeQuery()) {
                assertTrue(rows.next()); assertEquals("first", rows.getString(1));
                assertTrue(rows.next()); assertEquals("second", rows.getString(1)); assertFalse(rows.next());
            }
            p.setString(1, "other");
            try (var rows = p.executeQuery()) { assertFalse(rows.next()); }
            p.setString(1, "' OR 1=1 --");
            try (var rows = p.executeQuery()) { assertFalse(rows.next()); }
        }
    }

    @Test
    void foreignKeysAndBlankBodyConstraintsAreEnforced() {
        assertThrows(SQLException.class, () -> insert("unknown", 1, "Ghost", "hello"));
        assertThrows(SQLException.class, () -> insert("sender", 99, "Ghost", "hello"));
        for (String body : new String[]{null, "", "  ", "\t\n"}) {
            assertThrows(SQLException.class, () -> insert("sender", 1, "Ghost", body));
        }
        assertThrows(SQLException.class, () -> insert("sender", 1, " ", "hello"));
    }

    @Test
    void additiveSchemaCanBeAppliedTwiceWithoutChangingExistingData() throws Exception {
        insert("sender", 1, "Ghost", "keep");
        try (var c = connect(); var s = c.createStatement()) {
            applySchema(s);
            for (String table : new String[]{"users", "agents", "agent_messages"}) {
                try (var rows = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    assertTrue(rows.next()); assertEquals(1, rows.getInt(1));
                }
            }
            try (var rows = s.executeQuery("SELECT name FROM sqlite_master WHERE type='index' "
                    + "AND name='idx_agent_messages_recipient'")) { assertTrue(rows.next()); }
        }
    }
}
