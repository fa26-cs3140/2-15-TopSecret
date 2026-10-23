package org.example;

import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Isolates JDBC failures and metadata mapping; real SQLite tests run separately. */
class AccountAuditStoreUnitTest {
    @Test
    void accountStoreMapsMetadataAndNeverBindsPasswords() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PreparedStatement prepared = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(prepared);
        when(statement.executeQuery(anyString())).thenReturn(result);
        when(prepared.executeQuery()).thenReturn(result);
        when(prepared.executeUpdate()).thenReturn(1);
        try (var driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection("jdbc:sqlite:unit")).thenReturn(connection);
            var store = new SqliteUserAccountStore("jdbc:sqlite:unit");
            assertEquals("jdbc:sqlite:unit", store.getUrl());
            when(result.next()).thenReturn(true);
            assertTrue(store.hasUsers());
            assertTrue(store.addUser(new UserAccount("agent", false)));
            when(result.getString("username")).thenReturn("agent");
            when(result.getInt("is_admin")).thenReturn(1);
            assertTrue(store.getUser("agent").isAdmin());
            when(result.next()).thenReturn(true, false);
            assertEquals("agent", store.getUsers().get(0).getUsername());
            assertTrue(store.setAdmin("agent", false));
            verify(connection).prepareStatement("INSERT INTO users (username, is_admin) VALUES (?, ?)");
            verify(prepared, atLeastOnce()).setString(1, "agent");
            verify(prepared, never()).setString(eq(3), anyString());
        }
    }

    @Test
    void auditStoreMapsEveryRequiredFieldAndReportsWriteFailure() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PreparedStatement prepared = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(prepared);
        when(statement.executeQuery(anyString())).thenReturn(result);
        try (var driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection("jdbc:sqlite:unit")).thenReturn(connection);
            var store = new SqliteAuditLogStore("jdbc:sqlite:unit");
            assertEquals("jdbc:sqlite:unit", store.getUrl());
            var time = LocalDateTime.of(2026, 10, 22, 12, 30);
            store.add(new AuditLogEntry(0, "agent", "READ", "BRIEF", "7", time));
            verify(prepared).setString(1, "agent");
            verify(prepared).setString(2, "READ");
            verify(prepared).setString(3, "BRIEF");
            verify(prepared).setString(4, "7");
            verify(prepared).setString(5, time.toString());
            when(result.next()).thenReturn(true, false);
            when(result.getInt("id")).thenReturn(4);
            when(result.getString("username")).thenReturn("agent");
            when(result.getString("action")).thenReturn("READ");
            when(result.getString("content_type")).thenReturn("BRIEF");
            when(result.getString("content_identifier")).thenReturn("7");
            when(result.getString("accessed_at")).thenReturn(time.toString());
            var entry = store.getAll().get(0);
            assertEquals(4, entry.getId());
            assertEquals("agent", entry.getUsername());
            assertEquals("READ", entry.getAction());
            assertEquals("BRIEF", entry.getContentType());
            assertEquals("7", entry.getContentIdentifier());
            assertEquals(time, entry.getTimestamp());
            SQLException failure = new SQLException("disk full");
            when(prepared.executeUpdate()).thenThrow(failure);
            assertThrows(IllegalStateException.class, () -> store.add(entry));
        }
    }

    @Test
    void managerConvenienceConstructorsUseTheActualDatabaseUrl() {
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.getURL()).thenReturn("jdbc:sqlite:configured");
        try (var accounts = mockConstruction(SqliteUserAccountStore.class,
                (mock, context) -> assertEquals("jdbc:sqlite:configured", context.arguments().get(0)));
             var audits = mockConstruction(SqliteAuditLogStore.class,
                (mock, context) -> assertEquals("jdbc:sqlite:configured", context.arguments().get(0)))) {
            var users = new UserAccountManager(database, mock(FileHandler.class), mock(Cipher.class));
            assertFalse(users.hasUsers());
            var logs = new AuditLogManager(database);
            assertTrue(logs.getAuditLog().isEmpty());
            assertEquals(1, accounts.constructed().size());
            assertEquals(1, audits.constructed().size());
            verify(accounts.constructed().get(0)).hasUsers();
            verify(audits.constructed().get(0)).getAll();
        }
    }
}
