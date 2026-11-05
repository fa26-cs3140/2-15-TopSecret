package org.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import org.model.Mission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MissionManagerTest
{
    @TempDir
    Path tempDir;

    private String testUrl;

    @BeforeEach
    void setup() {
        testUrl = "jdbc.sqlite:" + tempDir.resolve("test.db");
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
    void testTableSchema() throws SQLException
    {
        new BriefManager(testUrl).createTable();

        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(testUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(missions)")) {

            while (rs.next()) {
                String name = rs.getString("name");
                columns.add(name);
                types.add(rs.getString("type"));

                if (name.equals("id")) {
                    assertEquals(1, rs.getInt("pk"));
                }

                if (name.equals("id") || name.equals("") || name.equals("text")) {
                    assertEquals(1, rs.getInt("notnull"));
                }
            }
        }

        assertEquals(List.of("id", "title", "briefIds", "agentIds", "facilityId"), columns);
        assertEquals(List.of("INTEGER", "TEXT", "INTEGER", "INTEGER", "INTEGER"), types);
    }

    @Test 
    void testConstructorPreservesExisitingRecords() throws SQLException 
    {
        MissionManager missionMan = new MissionManager(testUrl);
        missionMan.createTable();

        missionMan.addRecord("Test Title", null, null, 1);

        // creating a new manager should not delete existing data.
        new MissionManager(testUrl);
    }

    // ----- ADD RECORD ------
    @Test 
    void testAddRecordWithMission() throws SQLException
    {
        // TODO: Stub
    }

    @Test 
    void testAddRecordWithFields() throws SQLException
    {
        // TODO: Stub
    }

    @Test 
    void testAddMultipleRecords() throws SQLException
    {
        // TODO: Stub
    }

    @Test 
    void testAddInvalidRecord() throws SQLException
    {
        // TODO: Stub
    }

    // ------ GET RECORDS ------
    @Test
    void testGetRecordsById() throws SQLException
    {
        // TODO: Stub
    }

    @Test 
    void testGetRecordsByMissionId() throws SQLException
    {
        // TODO: Stub
    }

    @Test 
    void testGetRecordsNoMatch() throws SQLException
    {
        // TODO: Stub
    }

    @Test
    void testUpdateRecord() throws SQLException
    {
        // TODO: Stub
    }

    @Test
    void testUpdateRecordText() throws SQLException
    {
        // TODO: Stub
    }
    
    @Test
    void testUpdateRecordNoMatch() throws SQLException
    {
        // TODO: Stub
    }

    @Test
    void testImportTSV() throws SQLException, IOException
    {
        // TODO: Stub
    }
}
