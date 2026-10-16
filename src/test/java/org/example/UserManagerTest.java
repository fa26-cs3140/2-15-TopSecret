package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.File;
import java.util.List;

class UserManagerTest
{
    private Path testDirectory;
    private final String testDirName = "testDir";
    private final String testDatabase = "testUsers.db";
    private UserManager testManager;

    private final String testURL = "jdbc:sqlite:" + testDirName + "/" + testDatabase;

    @BeforeEach
    void setUp() throws IOException
    {
        this.testDirectory = Path.of(testDirName);
        Files.createDirectory(testDirectory);

        this.testManager = new UserManager(testURL);
    }

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
    void createUserDatabaseTest()
    {

    }

    @Test 
    void createUsersTest()
    {
        
    }

    @Test 
    void getUsersTest()
    {

    }

    @Test 
    void getUserAccessLevelsTest()
    {

    }

    @Test 
    void createFirstPrivilegedUserTest()
    {

    }

    @Test 
    void noUsersFoundTest()
    {

    }

}


