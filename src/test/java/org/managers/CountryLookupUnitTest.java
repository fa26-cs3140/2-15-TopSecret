package org.managers;

import org.junit.jupiter.api.Test;
import org.model.Countries;
import org.model.Facility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Country lookup and assignment mapping isolated from SQLite initialization. */
class CountryLookupUnitTest {
    @Test
    void enumLookupUsesPersistedIdAndCountryListsIncludeOnlyAssignedFacilities() throws Exception {
        var manager = mock(FacilityManager.class, CALLS_REAL_METHODS);
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        var result = mock(ResultSet.class);
        doReturn(connection).when(manager).connect();
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.getString("name")).thenReturn("First country");
        when(result.getInt("id")).thenReturn(1);
        var assigned = new Facility(7, "HQ", "HQ", 1);
        var other = new Facility(8, "Other", "OTH", 2);
        doReturn(List.of(assigned)).when(manager).getRecords("country_id", "1");
        doReturn(List.of(assigned, other)).when(manager).getRecords("id", "%");
        when(result.next()).thenReturn(true);
        assertEquals(1, manager.getCountry(Countries.values()[0]).getID());
        verify(statement).setInt(1, 1);
        when(result.next()).thenReturn(true, false);
        assertArrayEquals(new Facility[]{assigned}, manager.getCountries().getFirst().getFacilities());
    }

    @Test
    void addingCountryValidatesBeforeBindingItsName() {
        var manager = mock(FacilityManager.class, CALLS_REAL_METHODS);
        doReturn(197).when(manager).insert("INSERT INTO countries (name) VALUES (?)", "Newland");
        var country = manager.addCountry("Newland");
        assertEquals(197, country.getID());
        assertEquals("Newland", country.getName());
        assertThrows(IllegalArgumentException.class, () -> manager.addCountry(" "));
        verify(manager).insert("INSERT INTO countries (name) VALUES (?)", "Newland");
    }
}
