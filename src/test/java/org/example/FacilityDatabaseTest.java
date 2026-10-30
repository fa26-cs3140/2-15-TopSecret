package org.example;

import org.example.model.Facility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FacilityDatabaseTest {
    // Unlike in MissionSearchIntegrationTest, I think it's better if we go with TempDir
    // instead of manually creating a new test database and then deleting it after
    // Maybe I'll circle back to MissionSearchIntegrationTest to change that test
    // to do the same too
    @TempDir
    Path tempDir;

    private DatabaseManager databaseManager;
    private String testUrl;

    @BeforeEach
    void setup() {
        // Every test uses a temporary SQLite database
        Path dbPath = tempDir.resolve("test.db");
        testUrl = "jdbc:sqlite:" + dbPath;

        databaseManager = new DatabaseManager(testUrl);
        databaseManager.initializeSchema();
    }

    @Test
    void testFacilityTableCreated() throws Exception {
        try (Connection conn = DriverManager.getConnection(testUrl)) {
            // Look for the facilities table
            ResultSet rs = conn.getMetaData().getTables(null, null, "facilities", new String[]{"TABLE"});

            try (rs) {
                assertTrue(rs.next());
            }
        }
    }

    @Test
    void testAddFacility() {
        Facility facility = databaseManager.addFacility("Blackwood Annex", "BA");

        assertTrue(facility.getId() > 0);
        assertEquals("Blackwood Annex", facility.getName());
        assertEquals("BA", facility.getAbbreviation());

        Optional<Facility> retrieved = databaseManager.getFacilityById(facility.getId()); // Just learned about Optional, pretty cool

        assertTrue(retrieved.isPresent());
        assertEquals("Blackwood Annex", retrieved.get().getName());
        assertEquals("BA", retrieved.get().getAbbreviation());
    }

    @Test
    void testGetAllFacilities() {
        databaseManager.addFacility("Blackwood Annex", "BA");
        databaseManager.addFacility("Rose Garden", "RG");

        List<Facility> facilities = databaseManager.getAllFacilities();

        assertEquals(2, facilities.size());
        assertTrue(facilities.stream().anyMatch(f -> f.getName().equals("Blackwood Annex")));
        assertTrue(facilities.stream().anyMatch(f -> f.getName().equals("Rose Garden")));
    }

    @Test
    void testEmptyFacilities() {
        List<Facility> facilities = databaseManager.getAllFacilities();

        assertNotNull(facilities);
        assertTrue(facilities.isEmpty());
    }

    @Test
    void testGetNonexistentFacility() {
        Optional<Facility> result = databaseManager.getFacilityById(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateFacility() {
        Facility facility = databaseManager.addFacility("Blackwood Annex", "BA");

        facility.setName("Updated Annex");
        facility.setAbbreviation("UA");

        assertTrue(databaseManager.updateFacility(facility));

        Facility retrieved = databaseManager.getFacilityById(facility.getId()).orElseThrow();

        assertEquals("Updated Annex", retrieved.getName());
        assertEquals("UA", retrieved.getAbbreviation());
        assertEquals(facility.getId(), retrieved.getId());
    }

    @Test
    void testUpdateNonexistentFacility() {
        Facility facility = new Facility(999, "Unknown Facility", "UF");

        assertFalse(databaseManager.updateFacility(facility));
    }

    @Test
    void testDeleteFacility() {
        Facility facility = databaseManager.addFacility("Blackwood Annex", "BA");

        assertTrue(databaseManager.deleteFacility(facility.getId()));
        assertTrue(databaseManager.getFacilityById(facility.getId()).isEmpty());

        // Can't delete the same record twice
        assertFalse(databaseManager.deleteFacility(facility.getId()));
    }

    @Test
    void testRepeatedInitializationPreservesData() {
        Facility facility = databaseManager.addFacility("Blackwood Annex", "BA");

        databaseManager.initializeSchema();
        databaseManager.initializeSchema();

        List<Facility> facilities = databaseManager.getAllFacilities();

        assertEquals(1, facilities.size());
        assertEquals(facility.getId(), facilities.get(0).getId());
    }
}
