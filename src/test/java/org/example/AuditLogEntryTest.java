package org.example;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogEntryTest {

    @Test
    void constructorAndGettersStoreAuditData() {

        LocalDateTime time =
                LocalDateTime.of(
                        2026,
                        10,
                        16,
                        12,
                        30
                );

        AuditLogEntry entry =
                new AuditLogEntry(
                        1,
                        "agent_7",
                        "READ",
                        "BRIEF",
                        "42",
                        time
                );

        assertEquals(
                1,
                entry.getId()
        );

        assertEquals(
                "agent_7",
                entry.getUsername()
        );

        assertEquals(
                "READ",
                entry.getAction()
        );

        assertEquals(
                "BRIEF",
                entry.getContentType()
        );

        assertEquals(
                "42",
                entry.getContentIdentifier()
        );

        assertEquals(
                time,
                entry.getTimestamp()
        );
    }

    @Test
    void settersUpdateAuditData() {

        AuditLogEntry entry =
                new AuditLogEntry(
                        1,
                        "agent_7",
                        "READ",
                        "BRIEF",
                        "42",
                        LocalDateTime.of(
                                2026,
                                10,
                                16,
                                12,
                                30
                        )
                );

        LocalDateTime newTime =
                LocalDateTime.of(
                        2026,
                        10,
                        17,
                        9,
                        15
                );

        entry.setId(2);
        entry.setUsername("admin-user");
        entry.setAction("CREATE");
        entry.setContentType("FACILITY");
        entry.setContentIdentifier("7");
        entry.setTimestamp(newTime);

        assertEquals(
                2,
                entry.getId()
        );

        assertEquals(
                "admin-user",
                entry.getUsername()
        );

        assertEquals(
                "CREATE",
                entry.getAction()
        );

        assertEquals(
                "FACILITY",
                entry.getContentType()
        );

        assertEquals(
                "7",
                entry.getContentIdentifier()
        );

        assertEquals(
                newTime,
                entry.getTimestamp()
        );
    }
}