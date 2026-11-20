package org.managers;

import org.junit.jupiter.api.Test;
import org.model.Brief;
import org.model.Facility;
import org.model.Mission;
import org.model.RecordType;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Isolates the JDBC transaction protocol from the separate SQLite integration tests. */
class RecordPersistenceUnitTest {

    @Test
    void bindsValuesAndCommitsTheContentAndAuditOnOneConnection() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement prepared = mock(PreparedStatement.class);
        Statement statement = mock(Statement.class);
        ResultSet generated = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(prepared);
        when(connection.createStatement()).thenReturn(statement);
        when(prepared.executeUpdate()).thenReturn(1);
        when(statement.executeQuery("SELECT last_insert_rowid()")).thenReturn(generated);
        when(generated.getInt(1)).thenReturn(7);
        var event = new RecordEditor.AuditEvent("admin", "CREATE", RecordType.BRIEF, LocalDateTime.now());

        try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection("jdbc:sqlite:test")).thenReturn(connection);
            SqliteRecordStore store = new SqliteRecordStore("jdbc:sqlite:test");
            assertEquals(7, store.create(new Brief(1, "O'Brien", "2020-01-01", "text", null), event));
            assertTrue(store.update(7, new Facility(7, "HQ", "HQ"), event));
            assertEquals(7, store.create(new Mission(1, "Recon", new int[]{2}, new int[]{3}, 1), event));

            assertEquals(7, store.create(new org.model.Country("Country", 1, new Facility[0]), event));

            assertEquals(java.util.List.of(), store.createAll(java.util.List.of(), event));
            assertEquals(java.util.List.of(7, 7), store.createAll(java.util.List.of(
                    new Brief(1, "First", "2020-01-01", "text", null),
                    new Brief(2, "Second", "2020-01-01", "text", null)), event));
            verify(prepared).setObject(1, "O'Brien");
            verify(connection, times(5)).setAutoCommit(false);
            verify(connection, times(5)).commit();
            verify(connection, never()).rollback();
            verify(connection, times(7)).prepareStatement(startsWith("INSERT INTO audit_log"));
            verify(connection, never()).prepareStatement(contains("O'Brien"));
        }
    }
}
