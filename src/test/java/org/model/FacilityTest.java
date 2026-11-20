package org.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FacilityTest {
    @Test
    void testConstructorAndGetters() {
        Facility facility = new Facility(1, "Blackwood Annex", "BA", 1);

        assertEquals(1, facility.getId());
        assertEquals("Blackwood Annex", facility.getName());
        assertEquals("BA", facility.getAbbreviation());
    }

    @Test
    void testSetters() {
        Facility facility = new Facility(1, "Blackwood Annex", "BA", 1);

        facility.setName("Rose Garden");
        facility.setAbbreviation("RG");

        assertEquals("Rose Garden", facility.getName());
        assertEquals("RG", facility.getAbbreviation());
        assertEquals(1, facility.getId());
    }

    @Test
    void testInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Facility(0, "Blackwood Annex", "BA", 1));
        assertThrows(IllegalArgumentException.class, () -> new Facility(-1, "Blackwood Annex", "BA", 1));
    }

    @Test
    void testInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> new Facility(1, null, "BA", 1));
        assertThrows(IllegalArgumentException.class, () -> new Facility(1, "", "BA", 1));
        assertThrows(IllegalArgumentException.class, () -> new Facility(1, "   ", "BA", 1));
    }

    @Test
    void testInvalidAbbreviation() {
        assertThrows(IllegalArgumentException.class, () -> new Facility(1, "Blackwood Annex", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Facility(1, "Blackwood Annex", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new Facility(1, "Blackwood Annex", "   ", 1));
    }

    @Test
    void testInvalidNameSetter() {
        Facility facility = new Facility(1, "Blackwood Annex", "BA", 1);

        assertThrows(IllegalArgumentException.class, () -> facility.setName(null));
        assertThrows(IllegalArgumentException.class, () -> facility.setName(""));
        assertThrows(IllegalArgumentException.class, () -> facility.setName("   "));
        assertEquals("Blackwood Annex", facility.getName());
    }

    @Test
    void testInvalidAbbreviationSetter() {
        Facility facility = new Facility(1, "Blackwood Annex", "BA", 1);

        assertThrows(IllegalArgumentException.class, () -> facility.setAbbreviation(null));
        assertThrows(IllegalArgumentException.class, () -> facility.setAbbreviation(""));
        assertThrows(IllegalArgumentException.class, () -> facility.setAbbreviation("   "));
        assertEquals("BA", facility.getAbbreviation());
    }

    @Test
    void testMinimumValidValues() {
        Facility facility = new Facility(1, "A", "B", 1);

        assertEquals(1, facility.getId());
        assertEquals("A", facility.getName());
        assertEquals("B", facility.getAbbreviation());
    }
}
