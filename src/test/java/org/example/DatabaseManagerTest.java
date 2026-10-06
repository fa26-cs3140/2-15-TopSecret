package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.File;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

class DatabaseManagerTest
{
    private Path testDirectory;
    private final String testDirName = "testDir";
    private final String testDatabase = "testDatabase.db";
    private final String missionData = "data/mission_briefs.tsv";
    private DatabaseManager testManager;

    private Connection testConnection;
    private final String testURL = "jdbc:sqlite:" + testDirName + "/" + testDatabase;

    // Set up a dummy directory and databse to work with
    @BeforeEach
    void setUp() throws IOException
    {
        this.testDirectory = Path.of(testDirName);
        Files.createDirectory(testDirectory);

        this.testManager = new DatabaseManager(testURL);
    }

    // Remove dummy directory + all files
    @AfterEach
    void cleanUp() throws IOException
    {
        File[] allFiles = testDirectory.toFile().listFiles();

        if (allFiles != null)
        {
            for (File file : allFiles)
                Files.delete(file.toPath());
        }

        Files.delete(testDirectory);
    }

    @Test
    void createDatabaseTest()
    {
        DebugHelper.debugPrintln("Starting test...");
        this.testManager = new DatabaseManager(this.testURL);
        Boolean success = false;
        try
        {
            success = this.testManager.createMissionDatabaseFromTSV(this.missionData, 
                      this.testDirectory.toString(), this.testDatabase);
        }
        catch (IOException e)
        {
            System.err.print(e.getMessage());
        }
        
        // Sleep for 30s so i can check the database myself
        assertTrue(success, "Table not successfuly made.");

        DebugHelper.debugPrintln("Test passed!");

    }

    @Test
    void insertRecordTest()
    {
        
    }

    @Test
    void removeRecordTest()
    {
    }

    @Test
    void insertColumnTest()
    {
    }

    @Test
    void removeColumnTest()
    {
    }

    @Test
    void setRecordTest()
    {
    }

    @Test 
    void setColumnTest()
    {
    }

    @Test
    void getRecordTest()
    {
        DebugHelper.debugPrintln("Starting test...");
        this.testManager = new DatabaseManager(this.testURL);
        // Create temporary database
        this.createTestDatabase(this.testManager);
        DebugHelper.debugPrintln("Database created.");

        // Store all information (including headers) into an array
        FileHandler fhandler = new FileHandler();
        String[] data_raw = fhandler.readFile("mission_briefs.tsv").split("\n");
        // Test 1 -- Pull exactly one record by title
        // Create expeceted output
        String[] expOut_1 = new String[1];
        expOut_1[0] = "Operation Sandtrap\t1970-11-03\tBug the diplomatic pouch of the Libyan attaché during the layover in Rome.";

        DebugHelper.debugPrintln("Getting records...");
        String[] out_1 = testManager.getRecords(this.testManager.getURL(), "Title", "Operation Sandtrap");

        if (out_1 == null) 
        {
            DebugHelper.debugPrintln("out_1 is null.");
            throw new AssertionError("out_1 is null");
        }

        // Format the output here to make sure that
        // the behaviour for splitting around tabs is also
        // tested.
        String[] record_out_1 = out_1[0].split("\t");
        String[] record_expOut_1 = expOut_1[0].split("\t");
        
        assertEquals(record_out_1[0], record_expOut_1[0], "1. Records do not match");

        // Test 2 -- Pull entire table
        String[] expOut_2 = Arrays.copyOfRange(data_raw, 1, data_raw.length);
        String[] out_2 = testManager.getRecords(this.testManager.getURL(), "Title", "%");

        DebugHelper.debugPrintln(out_2.length);
        assertTrue(out_2.length == expOut_2.length, "2. Records do not match");
        for (int i = 0; i < out_2.length; i++)
        {
            assertEquals(out_2[i], expOut_2[i], "2. Records do not match");
        }

        // Test 3 -- Pull three random items by date
        
    }

    @Test
    void getColumnTest()
    {
    }

    // Helper function to create temporary databases without 
    // invoking testing issues
    // Must be run within testing bodies, setUp() and cleanUp()
    // can handle removing test files
    private void createTestDatabase(DatabaseManager manager)
    {
        // Create temporary database
        try
        {
            manager.createMissionDatabaseFromTSV(this.missionData, 
                 this.testDirectory.toString(), this.testDatabase);
        }
        catch (IOException e)
        {
            System.err.print(e.getMessage());
        }
    }
}

