// Mike Ly (mch3jy)

package org.example.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AgentTest {
    @Test
    void testConstructorAndGetters() {
        Agent agent = new Agent(1, "James Bond", "1985-06-14", null, "Secret Agent", 2);

        assertEquals(1, agent.getId());
        assertEquals("James Bond", agent.getName());
        assertEquals("1985-06-14", agent.getDateOfBirth());
        assertNull(agent.getDateOfDeath());
        assertEquals("Secret Agent", agent.getNotes());
        assertEquals(2, agent.getFacilityId());
    }

    @Test
    void testSetters() {
        Agent agent = new Agent(1, "James Bond", "1985-06-14", null, "Secret Agent", 2);

        agent.setName("Ethan Hunt");
        agent.setDateOfBirth("1990-05-10");
        agent.setDateOfDeath("2025-01-15");
        agent.setNotes("IMF Agent");
        agent.setFacilityId(3);

        assertEquals("Ethan Hunt", agent.getName());
        assertEquals("1990-05-10", agent.getDateOfBirth());
        assertEquals("2025-01-15", agent.getDateOfDeath());
        assertEquals("IMF Agent", agent.getNotes());
        assertEquals(3, agent.getFacilityId());

        // Optional attributes
        agent.setDateOfDeath(null);
        agent.setNotes(null);

        assertNull(agent.getDateOfDeath());
        assertNull(agent.getNotes());
    }

    @Test
    void testInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Agent(0, "James Bond", "1985-06-14", null, null, 2));
        assertThrows(IllegalArgumentException.class, () -> new Agent(-1, "James Bond", "1985-06-14", null, null, 2));
    }

    @Test
    void testInvalidName() {
        // IllegalArgumentException should be thrown
        assertThrows(IllegalArgumentException.class, () -> new Agent(1, null, "1985-06-14", null, null, 2));
        assertThrows(IllegalArgumentException.class, () -> new Agent(1, "   ", "1985-06-14", null, null, 2));

        Agent agent = new Agent(1, "James Bond", "1985-06-14", null, null, 2);

        assertThrows(IllegalArgumentException.class, () -> agent.setName(""));
        assertThrows(IllegalArgumentException.class, () -> agent.setName(null));
        assertEquals("James Bond", agent.getName());
    }

    @Test
    void testInvalidDateOfBirth() {
        Agent agent = new Agent(1, "James Bond", "1985-06-14", null, null, 2);

        String[] invalidDates = {"1985-02-30", "1985-13-01", "1985-6-14", "June 14, 1985", "banana", ""};
        for (String date : invalidDates) {
            assertThrows(IllegalArgumentException.class, () -> agent.setDateOfBirth(date));
        }

        assertThrows(IllegalArgumentException.class, () -> agent.setDateOfBirth(null));
        assertEquals("1985-06-14", agent.getDateOfBirth());
    }

    @Test
    void testInvalidDateOfDeath() {
        Agent agent = new Agent(1, "James Bond", "1985-06-14", null, null, 2);

        assertThrows(IllegalArgumentException.class, () -> agent.setDateOfDeath("1980-01-01"));
        assertThrows(IllegalArgumentException.class, () -> agent.setDateOfDeath("2025-02-30"));
        assertThrows(IllegalArgumentException.class, () -> agent.setDateOfDeath("Jan 1, 2025"));
        assertNull(agent.getDateOfDeath());
    }

    @Test
    void testDateOrdering() {
        Agent agent = new Agent(1, "James Bond", "1985-06-14", "2025-01-01", null, 2);

        assertThrows(IllegalArgumentException.class, () -> agent.setDateOfBirth("2030-01-01"));
        assertEquals("1985-06-14", agent.getDateOfBirth());

        assertThrows(IllegalArgumentException.class, () -> agent.setDateOfDeath("1980-01-01"));
        assertEquals("2025-01-01", agent.getDateOfDeath());

        agent.setDateOfDeath("1985-06-14");
        assertEquals("1985-06-14", agent.getDateOfDeath());
    }

    @Test
    void testConstructorRejectsInvalidDates() {
        assertThrows(IllegalArgumentException.class, () -> new Agent(1, "James Bond", null, null, null, 2));
        assertThrows(IllegalArgumentException.class, () -> new Agent(1, "James Bond", "1985-06-14", "1980-01-01", null, 2));
        assertThrows(IllegalArgumentException.class, () -> new Agent(1, "James Bond", "1985-02-30", null, null, 2));
    }
}
