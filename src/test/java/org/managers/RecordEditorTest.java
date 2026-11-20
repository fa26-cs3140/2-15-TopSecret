package org.managers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.model.AuditLogEntry;
import org.model.Brief;
import org.model.RecordType;
import org.model.UserAccount;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordEditorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T20:00:00Z"), ZoneOffset.UTC);
    private static final UserAccount ADMIN = new UserAccount("admin", true);
    private static final Brief BRIEF = new Brief(7, "O'Brien's mission", "2000-01-01", "Updated text", null);

    @Mock
    private RecordEditor.Store<Brief> store;

    @Test
    void rejectsUnauthenticatedAndStandardUsersBeforeCallingPersistence() {
        RecordEditor<Brief> editor = editor();

        assertThrows(SecurityException.class, () -> editor.create(null, BRIEF));
        assertThrows(SecurityException.class, () -> editor.update(new UserAccount("agent", false), BRIEF));
        verifyNoInteractions(store);
    }

    @Test
    void createUsesGeneratedIdentifierInTheAuditEntry() {
        when(store.create(eq(BRIEF), any())).thenAnswer(invocation -> {
            RecordEditor.AuditEvent event = invocation.getArgument(1);
            AuditLogEntry entry = event.forRecord(42);
            assertEquals("admin", entry.getUsername());
            assertEquals("CREATE", entry.getAction());
            assertEquals("BRIEF", entry.getContentType());
            assertEquals("42", entry.getContentIdentifier());
            assertEquals(LocalDateTime.now(CLOCK), entry.getTimestamp());
            return 42;
        });

        assertEquals(42, editor().create(ADMIN, BRIEF));
    }

    @Test
    void updateUsesExistingIdAndReportsMissingRecord() {
        RecordEditor.AuditEvent event = new RecordEditor.AuditEvent(
                "admin", "UPDATE", RecordType.BRIEF, LocalDateTime.now(CLOCK));
        when(store.update(7, BRIEF, event)).thenReturn(false);

        assertFalse(editor().update(ADMIN, BRIEF));
    }

    @Test
    void updateRejectsInvalidIdBeforeCallingPersistence() {
        RecordEditor<Brief> editor = new RecordEditor<>(RecordType.BRIEF, store, brief -> 0, CLOCK);

        assertThrows(IllegalArgumentException.class, () -> editor.update(ADMIN, BRIEF));
        verifyNoInteractions(store);
    }

    @Test
    void importsRequireAnAdministratorAndUseTheTransactionalBatchStore() {
        var records = java.util.List.of(BRIEF);
        assertThrows(SecurityException.class, () -> editor().importRecords(null, records));
        assertThrows(SecurityException.class, () -> editor().importRecords(new UserAccount("agent", false), records));
        verifyNoInteractions(store);
        when(store.createAll(eq(records), any())).thenReturn(java.util.List.of(42));
        assertEquals(java.util.List.of(42), editor().importRecords(ADMIN, records));
    }

    private RecordEditor<Brief> editor() {
        return new RecordEditor<>(RecordType.BRIEF, store, Brief::getId, CLOCK);
    }
}
