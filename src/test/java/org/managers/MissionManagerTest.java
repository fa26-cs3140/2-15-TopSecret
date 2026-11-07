package org.managers;

import org.controller.ProgramControl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import org.model.Mission;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MissionManagerTest
{
    private String testUrl;

    @BeforeEach
    void setup() throws IOException {
        Files.createDirectory(Path.of("mission_test"));
        Files.createFile(Path.of("mission_test/test.db"));
        testUrl = "jdbc:sqlite:" + "mission_test/test.db";
    }

    @AfterEach
    void cleanup() throws IOException
    {
        File[] allFiles = Path.of("mission_test").toFile().listFiles();
        
        if (allFiles != null) {
            for (File file : allFiles)
                Files.delete(file.toPath());
        }

        Files.delete(Path.of("mission_test"));
        
    }

    // Tests
    @Test 
    void testInvalidURL()
    {
        assertThrows(IllegalArgumentException.class, () -> new MissionManager(null));
        assertThrows(IllegalArgumentException.class, () -> new MissionManager(""));
        assertThrows(IllegalArgumentException.class, () -> new MissionManager("          "));
    }

    @Test 
    void testBriefTableCreated() throws SQLException 
    {
        new MissionManager(testUrl).createTable();

        try (Connection conn = DriverManager.getConnection(testUrl);
             ResultSet rs = conn.getMetaData().getTables(null, null, "missions", new String[]{"TABLE"})) {
                 
                 assertTrue(rs.next(), "missions table should exist");
             }
    }

    @Test 
    void testConstructorPreservesExisitingRecords() throws SQLException 
    {
        MissionManager missionMan = new MissionManager(testUrl);
        missionMan.createTable();

        assertThrows(IllegalArgumentException.class, () -> missionMan.addRecord("Test Title", new int[] { 1 }, new int[] { 1 }, 1));

        // creating a new manager should not delete existing data.
        new MissionManager(testUrl);
    }

    @Test
    void testCreateMissionTables() throws SQLException {
        MissionManager manager = new MissionManager(testUrl);

        manager.createTable();

        try (Connection conn = DriverManager.getConnection(testUrl)) {
            DatabaseMetaData metadata = conn.getMetaData();

            for (String table : List.of(
                        "missions",
                        "j_mission_briefs",
                        "j_mission_agents")) {

                try (ResultSet rs = metadata.getTables(
                            null, null, table, new String[]{"TABLE"})) {

                    assertTrue(rs.next(),
                            table + " should exist");
                            }
                        }
        }
    }

    // @Test
    // void testAddMultipleRecords() throws SQLException {
    //     MissionManager manager = new MissionManager(testUrl);
    //     manager.createTable();

    //     // Create the required facility table and record.
    //     FacilityManager facilities = new FacilityManager(testUrl);
    //     facilities.createTable();
    //     facilities.addRecord("Headquarters", "HQ");

    //     Mission first = manager.addRecord(
    //             "Mission Alpha",
    //             new int[]{},
    //             new int[]{},
    //             1
    //             );

    //     Mission second = manager.addRecord(
    //             "Mission Bravo",
    //             new int[]{},
    //             new int[]{},
    //             1
    //             );

    //     // assertTrue(first.getId() > 0);
    //     // assertTrue(second.getId() > 0);
    //     // assertNotEquals(first.getId(), second.getId());
    // }

    @Test
    void testGetRecordsNoMatch() {
        MissionManager manager = new MissionManager(testUrl);
        manager.createTable();

        List<Mission> results =
            manager.getRecords("id", "999");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
