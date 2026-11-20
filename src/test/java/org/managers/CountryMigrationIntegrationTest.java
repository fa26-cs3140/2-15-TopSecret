package org.managers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.model.Countries;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class CountryMigrationIntegrationTest {

    @TempDir
    Path directory;

    @Test
    void oldFacilitiesSurviveMigrationAndCountriesSeedOnce() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("legacy.db");
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE facilities (id INTEGER PRIMARY KEY, name TEXT NOT NULL, "
                    + "abbreviation TEXT NOT NULL, mission_id INTEGER)");
            statement.execute("INSERT INTO facilities VALUES (41, 'Legacy HQ', 'HQ', NULL)");
        }
        FacilityManager manager = new FacilityManager(url);
        manager.createTable();
        new FacilityManager(url).createTable();

        assertEquals(Countries.values().length, manager.getCountries().size());
        var legacy = manager.getRecords("id", "41").getFirst();
        assertEquals("Legacy HQ", legacy.getName());
        assertEquals(0, legacy.getCountryID());
        assertTrue(manager.updateRecord("id", "41", "country_id", "1"));
        assertEquals(41, manager.getCountry(1).getFacilities()[0].getId());
        assertEquals(1, manager.getCountry(Countries.values()[0]).getID());
        assertEquals(1, manager.getCountry(Countries.values()[0].getName().substring(0, 3)).getID());
        assertNull(manager.getCountry(-1));
        assertNull(manager.getCountry("No such country"));
        assertNull(manager.getCountry((String) null));
    }

    @Test
    void renamedAndNewCountriesSurviveSubsequentInitialization() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("countries.db");
        FacilityManager manager = new FacilityManager(url);
        var country = manager.addCountry("O'Brien Islands");
        assertTrue(country.getID() > Countries.values().length);
        manager.addRecord("Island HQ", "IHQ", country.getID());
        try (var connection = DriverManager.getConnection(url); var statement = connection.prepareStatement(
                "UPDATE countries SET name = ? WHERE id = ?")) {
            statement.setString(1, "Renamed country");
            statement.setInt(2, 1);
            statement.executeUpdate();
        }
        new FacilityManager(url).createTable();

        assertEquals("Renamed country", manager.getCountry(1).getName());
        assertEquals(1, manager.getCountry(country.getID()).getFacilities().length);
        assertEquals(Countries.values().length + 1, manager.getCountries().size());
        assertEquals(country.getID(), manager.getCountry("O'Brien").getID());
    }
}
