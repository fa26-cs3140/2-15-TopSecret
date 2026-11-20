package org.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CountryTest {

    @Test
    void countryNamesCanBeEditedAndFacilitiesCanBeReplaced() {
        Country country = new Country("Afghanistan", 1, new Facility[0]);
        assertEquals(1, country.getID());
        assertEquals("Afghanistan", country.getName());
        country.setName("Updated country");
        Facility[] facilities = {new Facility(7, "HQ", "HQ", 1)};
        country.setFacilities(facilities);
        assertEquals("Updated country", country.getName());
        assertArrayEquals(facilities, country.getFacilities());
    }

    @Test
    void identifiersArePositiveAndNamesAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> new Country("Name", 0, new Facility[0]));
        assertThrows(IllegalArgumentException.class, () -> new Country(" ", 1, new Facility[0]));
        assertEquals(500, new Country("New country", 500, new Facility[0]).getID());
        Facility facility = new Facility(1, "HQ", "HQ", 1);
        facility.setCountry(2);
        assertEquals(2, facility.getCountryID());
        assertThrows(IllegalArgumentException.class, () -> facility.setCountry(0));
        assertEquals(0, new Facility(1, "Legacy", "LEG").getCountryID());
    }

    @Test
    void suppliedCountryNamesAreAvailable() {
        assertTrue(Countries.values().length > 0);
        for (Countries country : Countries.values()) {
            assertNotNull(country.getName());
            assertFalse(country.getName().isBlank());
        }
    }
}
