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

class DatabaseManagerTest
{
    private Path testDirectory;
    private static final String testDatabase = "testDatabase.db";
    private DatabaseManager testManager;

    private Connection testConnection;
    private static final String testURL = "jdbc::sqlite::data/" + testDatabase;

    // Set up a dummy directory and databse to work with
    @BeforeEach
    void setUp() throws IOException
    {
        this.testDirectory = Path.of(testDatabase);
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
        this.testManager = new DatabaseManager(testDatabase);
        Boolean success = false;
        try
        {
            success = testManager.createMissionDatabaseFromTSV("data/mission_briefs.tsv", "data", "test_briefs.db");
        }
        catch (IOException e)
        {
            System.err.print(e.getMessage());
        }
        
        assertTrue(success, "createDatabaseTest failed: database not successfuly made");

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
    }

    @Test
    void getColumnTest()
    {
    }
}

