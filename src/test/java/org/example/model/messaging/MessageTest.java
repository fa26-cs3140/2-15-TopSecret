package org.example.model.messaging;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    private final LocalDateTime time = LocalDateTime.of(2026, 10, 29, 12, 30);

    @Test
    void constructorAndEveryGetterPreserveTheSnapshot() {
        Message message = new Message(8, "sender", 4, "Ghost", "  Meet at dawn.\nBring the file.  ", time);
        assertEquals(8, message.getId());
        assertEquals("sender", message.getSenderUsername());
        assertEquals(4, message.getRecipientAgentId());
        assertEquals("Ghost", message.getRecipientName());
        assertEquals("  Meet at dawn.\nBring the file.  ", message.getBody());
        assertEquals(time, message.getSentAt());
    }

    @Test
    void zeroIdIsAnUnsavedMessage() {
        assertEquals(0, new Message(0, "sender", 1, "Ghost", "Hello", time).getId());
    }

    @Test
    void invalidIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Message(-1, "sender", 1, "Ghost", "Hello", time));
        assertThrows(IllegalArgumentException.class, () -> new Message(0, "sender", 0, "Ghost", "Hello", time));
        assertThrows(IllegalArgumentException.class, () -> new Message(0, "sender", -1, "Ghost", "Hello", time));
    }

    @Test
    void missingOrBlankFieldsAreRejected() {
        for (String invalid : new String[]{null, "", " ", "\t\n"}) {
            assertThrows(IllegalArgumentException.class, () -> new Message(0, invalid, 1, "Ghost", "Hello", time));
            assertThrows(IllegalArgumentException.class, () -> new Message(0, "sender", 1, invalid, "Hello", time));
            assertThrows(IllegalArgumentException.class, () -> new Message(0, "sender", 1, "Ghost", invalid, time));
        }
        assertThrows(IllegalArgumentException.class, () -> new Message(0, "sender", 1, "Ghost", "Hello", null));
    }
}
