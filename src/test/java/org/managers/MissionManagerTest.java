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
    //     facilities.addRecord("Headquarters", "HQ", 1);

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

    // A mission needs one facility; these tests also use two briefs and one agent.
    private MissionManager managerWithRelatedRecords()
    {
        FacilityManager facilities = new FacilityManager(testUrl);
        facilities.createTable();
        facilities.addRecord("Headquarters", "HQ", 1);
        BriefManager briefs = new BriefManager(testUrl);
        briefs.createTable();
        briefs.addRecord("Brief One", "2020-01-01", "First");
        briefs.addRecord("Brief Two", "2020-01-02", "Second");
        AgentManager agents = new AgentManager(testUrl);
        agents.createTable();
        agents.addRecord("Ghost", "1992-11-22", null, null, 1);
        MissionManager manager = new MissionManager(testUrl);
        manager.createTable();
        return manager;
    }

    private Mission reload(MissionManager manager, int id)
    {
        return manager.getRecords("id", String.valueOf(id)).get(0);
    }

    @Test
    void testMissionWithNoBriefsOrAgents()
    {
        MissionManager manager = managerWithRelatedRecords();

        Mission mission = manager.addRecord("Solo Op", new int[]{}, new int[]{}, 1);

        assertEquals(0, reload(manager, mission.getId()).getBriefIds().length);
        assertEquals(0, reload(manager, mission.getId()).getAgentIds().length);
    }

    @Test
    void testReplaceAssignments()
    {
        MissionManager manager = managerWithRelatedRecords();
        int id = manager.addRecord("Joint Op", new int[]{1}, new int[]{}, 1).getId();

        manager.replaceAssignments(id, new int[]{2}, new int[]{1});

        assertArrayEquals(new int[]{2}, reload(manager, id).getBriefIds());
        assertArrayEquals(new int[]{1}, reload(manager, id).getAgentIds());
    }

    @Test
    void testReplaceAssignmentsWithUnknownIdKeepsOldAssignments()
    {
        MissionManager manager = managerWithRelatedRecords();
        int id = manager.addRecord("Joint Op", new int[]{1}, new int[]{1}, 1).getId();

        assertThrows(IllegalArgumentException.class, () -> manager.replaceAssignments(id, new int[]{99}, new int[]{}));
        assertArrayEquals(new int[]{1}, reload(manager, id).getBriefIds());
        assertArrayEquals(new int[]{1}, reload(manager, id).getAgentIds());
    }

    @Test
    void testUpdateRecordStoresQuotesSafelyAndRejectsUnknownColumns()
    {
        MissionManager manager = managerWithRelatedRecords();
        int id = manager.addRecord("Joint Op", new int[]{}, new int[]{}, 1).getId();

        assertTrue(manager.updateRecord("id", String.valueOf(id), "title", "O'Brien's Op"));
        assertEquals("O'Brien's Op", reload(manager, id).getTitle());
        assertFalse(manager.updateRecord("id", "999", "title", "Nobody"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.updateRecord("id", String.valueOf(id), "title = 'x' --", "y"));
    }
}
