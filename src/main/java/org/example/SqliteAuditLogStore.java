package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** SQLite persistence for audit-log records. */
public class SqliteAuditLogStore implements AuditLogStore {

    private final String url;

    public SqliteAuditLogStore(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL cannot be blank.");
        }
        this.url = url;
        createTables();
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void add(AuditLogEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Audit entry cannot be null.");
        }

        String sql = "INSERT INTO audit_log "
                + "(username, action, content_type, content_identifier, accessed_at) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.getUsername());
            statement.setString(2, entry.getAction());
            statement.setString(3, entry.getContentType());
            statement.setString(4, entry.getContentIdentifier());
            statement.setString(5, entry.getTimestamp().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to store audit entry.", e);
        }
    }

    @Override
    public List<AuditLogEntry> getAll() {
        List<AuditLogEntry> entries = new ArrayList<>();
        String sql = "SELECT id, username, action, content_type, content_identifier, accessed_at "
                + "FROM audit_log ORDER BY id";

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                entries.add(new AuditLogEntry(
                        result.getInt("id"),
                        result.getString("username"),
                        result.getString("action"),
                        result.getString("content_type"),
                        result.getString("content_identifier"),
                        LocalDateTime.parse(result.getString("accessed_at"))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read persisted records.", e);
        }
        return entries;
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    private void createTables() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "username TEXT NOT NULL PRIMARY KEY, is_admin INTEGER NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS audit_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL REFERENCES users(username), "
                    + "action TEXT NOT NULL, "
                    + "content_type TEXT NOT NULL, "
                    + "content_identifier TEXT NOT NULL, "
                    + "accessed_at TEXT NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize audit log tables.", e);
        }
    }
}
