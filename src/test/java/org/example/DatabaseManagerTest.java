package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

import java.sql.*;

class DatabaseManagerTest
{
    private Path testDirectory;
    private static final String testDatabase = "testDatabase.tsv";
    private DatabaseManager testManager;

    private Connection testConnection;
    private static final String testURL = "jdbc::sqlite::data/" + testDatabase;

    @BeforeEach
    void setUp() throws IOException
    {
        this.testDirectory = Path.of(testDatabase);
        Files.createDirectory(testDirectory);

        this.testManager = new DatabaseManager(testURL);
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

