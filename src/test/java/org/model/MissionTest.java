package org.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MissionTest {
    @Test
    void testConstructorAndGetters() {
        Mission mission = new Mission(1, 2);

        assertEquals(1, mission.getId());
        assertEquals(2, mission.getFacilityId());
    }

    @Test
    void testSetFacilityId() {
        Mission mission = new Mission(1, 2);
        mission.setFacilityId(3);

        assertEquals(3, mission.getFacilityId());
        // Mission ID should remain unchanged
        assertEquals(1, mission.getId());
    }

    @Test
    void testInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Mission(0, 2));
        assertThrows(IllegalArgumentException.class, () -> new Mission(-1, 2));
    }

    @Test
    void testInvalidFacilityId() {
        assertThrows(IllegalArgumentException.class, () -> new Mission(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Mission(1, -1));
    }

    @Test
    void testInvalidFacilityIdSetter() {
        Mission mission = new Mission(1, 2);

        assertThrows(IllegalArgumentException.class, () -> mission.setFacilityId(0));
        assertThrows(IllegalArgumentException.class, () -> mission.setFacilityId(-1));

        // Invalid updates shouold leave the object unchanged
        assertEquals(2, mission.getFacilityId());
    }

    @Test
    void testMinimumValidValues() {
        Mission mission = new Mission(1, 1);

        assertEquals(1, mission.getId());
        assertEquals(1, mission.getFacilityId());
    }
}
