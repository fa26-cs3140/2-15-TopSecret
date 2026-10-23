package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogManagerTest {

    @Mock
    private AuditLogStore store;

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuditLogManager((AuditLogStore) null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditLogManager((DatabaseManager) null));
    }

    @Test
    void logCreatesTimestampedAuditEntry() {
        AuditLogManager manager = new AuditLogManager(store);

        manager.log("agent_7", "READ", "BRIEF", "42");

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(store).add(captor.capture());
        AuditLogEntry entry = captor.getValue();

        assertEquals("agent_7", entry.getUsername());
        assertEquals("READ", entry.getAction());
        assertEquals("BRIEF", entry.getContentType());
        assertEquals("42", entry.getContentIdentifier());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void logRejectsBlankFields() {
        AuditLogManager manager = new AuditLogManager(store);

        assertThrows(IllegalArgumentException.class,
                () -> manager.log(null, "READ", "BRIEF", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.log(" ", "READ", "BRIEF", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.log("agent", "", "BRIEF", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.log("agent", "READ", null, "1"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.log("agent", "READ", "BRIEF", ""));

        verifyNoInteractions(store);
    }

    @Test
    void getAuditLogDelegatesToStore() {
        AuditLogEntry entry = new AuditLogEntry(1, "agent_7", "READ", "BRIEF", "1",
                java.time.LocalDateTime.now());
        when(store.getAll()).thenReturn(List.of(entry));

        AuditLogManager manager = new AuditLogManager(store);

        assertEquals(List.of(entry), manager.getAuditLog());
        verify(store).getAll();
    }
}
