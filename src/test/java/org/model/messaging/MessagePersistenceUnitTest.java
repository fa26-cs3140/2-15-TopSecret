package org.model.messaging;

import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tests binding and result mapping through mocked JDBC, with no SQLite database. */
class MessagePersistenceUnitTest {
    @Test
    void savingReadingAndMarkingReadBindIdentityAndMapEveryField() throws Exception {
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        var prepared = mock(PreparedStatement.class);
        var result = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(prepared);
        when(statement.executeQuery(anyString())).thenReturn(result);
        when(prepared.executeQuery()).thenReturn(result);
        when(prepared.executeUpdate()).thenReturn(1);
        when(result.getLong(1)).thenReturn(7L);
        when(result.getLong("id")).thenReturn(7L);
        when(result.getString("sender_username")).thenReturn("sender");
        when(result.getInt("recipient_agent_id")).thenReturn(2);
        when(result.getString("recipient_name")).thenReturn("Ghost");
        when(result.getString("body")).thenReturn("O'Brien");
        when(result.getString("sent_at")).thenReturn("2026-11-20T12:00");
        when(result.getInt("is_read")).thenReturn(1);
        try (var driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection("jdbc:sqlite:unit-messages")).thenReturn(connection);
            var store = new SqliteMessageStore("jdbc:sqlite:unit-messages");
            var draft = new Message(0, "sender", 2, "Ghost", "O'Brien", LocalDateTime.of(2026, 11, 20, 12, 0));
            assertEquals(7, store.save(draft).getId());
            verify(prepared).setString(1, "sender");
            verify(prepared).setInt(4, 2);
            verify(prepared).setString(5, "Ghost");
            when(result.next()).thenReturn(true, false);
            var inbox = store.findForRecipient("Ghost");
            assertEquals(1, inbox.size());
            assertEquals("O'Brien", inbox.getFirst().getBody());
            assertTrue(inbox.getFirst().isRead());
            assertThrows(UnsupportedOperationException.class, inbox::clear);
            when(result.next()).thenReturn(true);
            assertEquals(7, store.markAsRead(7, "Ghost").getId());
            verify(prepared, atLeastOnce()).setLong(1, 7);
            verify(prepared, times(2)).setString(2, "Ghost");
            when(prepared.executeUpdate()).thenReturn(0);
            assertThrows(IllegalArgumentException.class, () -> store.save(draft));
            assertThrows(IllegalArgumentException.class, () -> store.markAsRead(7, "Other"));
            SQLException unavailable = new SQLException("disk unavailable");
            when(prepared.executeQuery()).thenThrow(unavailable);
            assertThrows(IllegalStateException.class, () -> store.findForRecipient("Ghost"));
        }
    }
}
