package org.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MissionTest {
    @Test
    void testConstructorAndGetters() {
        Mission mission = new Mission(1, "Test Name", new int[] {1, 2, 3}, new int[] {3, 5, 6}, 2);

        assertEquals(1, mission.getId());
        assertEquals("Test Name", mission.getTitle());
        for (int i = 0; i < 3; i++)
        {
            assertEquals(new int[] {1, 2, 3}[i], mission.getBriefIds()[i]);
            assertEquals(new int[] {3, 5, 6}[i], mission.getAgentIds()[i]);

        }
        assertEquals("[ 1, 2, 3 ]", mission.getBriefIdsAsString());
        assertEquals("[ 3, 5, 6 ]", mission.getAgentIdsAsString());
        assertEquals(2, mission.getFacilityId());
    }

    @Test
    void testSetFacilityId() {
        Mission mission = new Mission(1, "Test Name", new int[] {1, 2, 3}, new int[] {3, 5, 6}, 2);
        mission.setFacilityId(3);

        assertEquals(3, mission.getFacilityId());
        // Mission ID should remain unchanged
        assertEquals(1, mission.getId());
    }

    @Test
    void testInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Mission(0, "Test Name", new int[] {1, 2, 3}, new int[] {3, 5, 6}, 2));
        assertThrows(IllegalArgumentException.class, () -> new Mission(-2, "Test Name", new int[] {1, 2, 3}, new int[] {3, 5, 6}, 2));
    }

    @Test
    void testInvalidFacilityId() {
        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Test Name", new int[] {1, 2, 3}, new int[] {3, 5, 6}, 0));
        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Test Name", new int[] {1, 2, 3}, new int[] {3, 5, 6}, -1));
    }

    @Test
    void testInvalidFacilityIdSetter() {
        Mission mission = new Mission(1, "Test Name", new int[] {1, 2, 3}, new int[] {3, 5, 6}, 2);

        assertThrows(IllegalArgumentException.class, () -> mission.setFacilityId(0));
        assertThrows(IllegalArgumentException.class, () -> mission.setFacilityId(-1));

        // Invalid updates shouold leave the object unchanged
        assertEquals(2, mission.getFacilityId());
    }
}
