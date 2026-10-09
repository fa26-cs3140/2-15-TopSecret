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
import java.util.Random;

class DatabaseManagerTest
{
    private Path testDirectory;
    private final String testDirName = "testDir";
    private final String testDatabase = "testDatabase.db";
    private final String missionData = "data/mission_briefs.tsv";
    private DatabaseManager testManager;

    private final String testURL = "jdbc:sqlite:" + testDirName + "/" + testDatabase;

    // Set up a dummy directory and databse to work with
    @BeforeEach
    void setUp() throws IOException
    {
        this.testDirectory = Path.of(testDirName);
        Files.createDirectory(testDirectory);

        this.testManager = new DatabaseManager(testURL);
        this.createTestDatabase(this.testManager);
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
        this.testManager = new DatabaseManager(this.testURL);
        Boolean success = false;
        try
        {
            success = this.testManager.importTSV(this.missionData, 
                      this.testDirectory.toString(), this.testDatabase);
        }
        catch (IOException e)
        {
            System.err.print(e.getMessage());
        }
        
        assertTrue(success, "Table not successfuly made.");
    }

    // TODO: Modifying the original database does not appear to be a part of the current criteria
    // for the project. As such, I have elected not to include them, not entirely out of
    // laziness, but also because it may well be a waste of time. Should the need arise, I will
    // be able to add them in rather easily.

    @Test
    void getRecordTest()
    {
        // Store all information (including headers) into an array
        FileHandler fhandler = new FileHandler();
        String[] data_raw = fhandler.readFile("mission_briefs.tsv").split("\n");
        // Test 1 -- Pull exactly one record by title
        // Create expeceted output
        String[] expOut_1 = new String[1];
        expOut_1[0] = "Operation Sandtrap\t1970-11-03\tBug the diplomatic pouch of the Libyan attaché during the layover in Rome.";

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

        assertTrue(out_2.length == expOut_2.length, "2. Records do not match");
        for (int i = 0; i < out_2.length; i++)
        {
            assertEquals(out_2[i], expOut_2[i], "2. Records do not match");
        }

        // Test 3 -- Pull random items by date
        /* TODO: Allow this to work in case multiple files have identical dates
         * For now does not support.
        */
        int num_loops = 30;
        String[] expOut_3 = new String[num_loops]; 
        for (int i = 0; i < num_loops; i++)
        {
            expOut_3 = data_raw[new Random().nextInt(1, data_raw.length)].split("\t");
            String dateToFind = expOut_3[1];
            String[] out_3 = testManager.getRecords("Date", dateToFind);

            for (int k = 0; k < out_3.length; k++)
            {
                assertEquals(out_3[k], expOut_3[k], "3. Records do not match");
            }
        }
    }

    @Test
    void getColumnTest()
    {
        // Store all information (including headers) into an array
        FileHandler fhandler = new FileHandler();
        String[] data_raw = fhandler.readFile("mission_briefs.tsv").split("\n");
        ArrayList<String> temp_titles = new ArrayList<String>();
        ArrayList<String> temp_dates = new ArrayList<String>();
        ArrayList<String> temp_texts = new ArrayList<String>();
        // Get expected output from raw
        for (int i = 1; i < data_raw.length; i++)
        {
            temp_titles.add(data_raw[i].split("\t")[0]);
            temp_dates.add(data_raw[i].split("\t")[1]);
            temp_texts.add(data_raw[i].split("\t")[2]);
        }
        String[] expOut_1 = temp_titles.toArray(new String[0]);
        String[] expOut_2 = temp_dates.toArray(new String[0]);
        String[] expOut_3 = temp_texts.toArray(new String[0]);

        // Get Title column
        String[] out_1 = testManager.getColumn("Title");
        assertTrue(out_1.length == expOut_1.length);
        for (int i = 0; i < out_1.length; i++)
        {
            assertEquals(out_1[i], expOut_1[i], "1. Arrays are not equivalent.");
        }

        // Get Date column
        String[] out_2 = testManager.getColumn("Date");
        assertTrue(out_2.length == expOut_2.length);
        for (int i = 0; i < out_2.length; i++)
        {
            assertEquals(out_2[i], expOut_2[i], "2. Arrays are not equivalent.");
        }
        
        // Get Text column
        String[] out_3 = testManager.getColumn("Text");
        assertTrue(out_3.length == expOut_3.length);
        for (int i = 0; i < out_3.length; i++)
        {
            assertEquals(out_3[i], expOut_3[i], "3. Arrays are not equivalent.");
        }
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
            manager.importTSV(this.missionData, 
                 this.testDirectory.toString(), this.testDatabase);
        }
        catch (IOException e)
        {
            System.err.print(e.getMessage());
        }
    }
}

